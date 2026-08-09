package com.pet.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.common.StatusCode;
import com.pet.security.JwtUtil;
import com.pet.system.dto.LoginRequestDTO;
import com.pet.system.dto.RegisterRequestDTO;
import com.pet.system.entity.Role;
import com.pet.system.entity.User;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserMapper;
import com.pet.system.mapper.UserRoleMapper;
import com.pet.system.service.UserService;
import com.pet.system.vo.LoginResponseVO;
import com.pet.system.vo.RegisterCaptchaVO;
import com.pet.system.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final int MAX_LOGIN_ATTEMPTS = 20;
    private static final long LOCK_DURATION_MINUTES = 15;
    private static final long REGISTER_CAPTCHA_TTL_SECONDS = 300;
    private static final SecureRandom REGISTER_CAPTCHA_RANDOM = new SecureRandom();
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    private static final String USER_LOGOUT_PREFIX = "logout:user:";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, LoginAttempt> localLoginAttempts = new ConcurrentHashMap<>();

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    private static class LoginAttempt {
        int count;
        long lockUntil;

        LoginAttempt(int count, long lockUntil) {
            this.count = count;
            this.lockUntil = lockUntil;
        }
    }

    /**
     * 【用户注册】
     *
     * 业务作用：创建新用户账号并完成自动登录
     *
     * 调用场景：用户在注册页面提交注册表单
     *
     * 调用链：RegisterController ↓ register() → 校验验证码 → ensurePhoneAvailable → UserMapper.insert → RoleMapper查询OWNER角色 → UserRoleMapper.insert → clearRegisterCaptcha → buildLoginResponse
     *
     * 数据处理：RegisterRequestDTO → normalizePhone/normalizeCaptcha → verifyRegisterCaptcha → 属性拷贝到User → passwordEncoder.encode加密密码 → HtmlUtils.htmlEscape防XSS → insert → LDQUERY OWNER角色 → 创建UserRole → 清除验证码 → 构建LoginResponseVO
     *
     * 业务规则：手机号和验证码需通过校验；密码BCrypt加密后存储；昵称做HTML转义防XSS；自动分配OWNER角色；DuplicateKeyException处理用户名唯一冲突
     *
     * 状态影响：插入user_wsh表记录 + 插入user_role_wsh表记录 + 删除Redis验证码缓存
     *
     * 异常情况：验证码错误/过期 → BusinessException；手机号已被绑定 → BusinessException；用户名重复(MySQL唯一索引) → 清除验证码后抛BusinessException
     *
     * 注意事项：注册成功即自动登录返回JWT；事务注解确保用户表和角色关联表写入原子性
     */
    @Override
    @Transactional
    public LoginResponseVO register(RegisterRequestDTO request) {
        String phone = normalizePhone(request.getPhone_wsh());
        String captcha = normalizeCaptcha(request.getCaptcha_wsh());
        verifyRegisterCaptcha(phone, captcha);
        ensurePhoneAvailable(phone, null);

        User user = new User();
        BeanUtil.copyProperties(request, user);
        user.setPhone_wsh(phone);
        user.setPassword_wsh(passwordEncoder.encode(request.getPassword_wsh()));
        user.setNickname_wsh(StringUtils.hasText(request.getNickname_wsh())
                ? HtmlUtils.htmlEscape(request.getNickname_wsh())
                : request.getUsername_wsh());
        user.setStatus_wsh(StatusCode.USER_ACTIVE.getValue());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            clearRegisterCaptcha(phone);
            throw new BusinessException("用户名已存在");
        }

        Role ownerRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode_wsh, "OWNER"));
        if (ownerRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUser_id_wsh(user.getId_wsh());
            userRole.setRole_id_wsh(ownerRole.getId_wsh());
            userRoleMapper.insert(userRole);
        }

        clearRegisterCaptcha(phone);
        return buildLoginResponse(user);
    }

    /**
     * 【请求注册验证码】
     *
     * 业务作用：生成6位数字注册验证码并存入Redis，返回给调用方
     *
     * 调用场景：用户在注册页点击"获取验证码"
     *
     * 调用链：RegisterController ↓ requestRegisterCaptcha() → normalizePhone → ensurePhoneAvailable → generateRegisterCaptcha → Redis SET → RegisterCaptchaVO
     *
     * 数据处理：phone → normalizePhone → ensurePhoneAvailable(检查手机号未被绑定) → SecureRandom生成6位数字 → Redis缓存(5分钟) → 封装RegisterCaptchaVO
     *
     * 业务规则：手机号不能为空且不能已被已有用户占用；验证码5分钟过期；同手机号重复调用会覆盖验证码
     *
     * 状态影响：Redis中写入 "register_captcha:{phone}" → captcha，TTL 300秒
     *
     * 异常情况：手机号为空 → BusinessException；手机号已被绑定 → BusinessException
     *
     * 注意事项：开发调试阶段直接返回验证码明文；生产环境短信发送后不应返回验证码
     */
    @Override
    public RegisterCaptchaVO requestRegisterCaptcha(String phone) {
        String normalizedPhone = normalizePhone(phone);
        ensurePhoneAvailable(normalizedPhone, null);

        String captcha = generateRegisterCaptcha();
        redisTemplate.opsForValue().set(buildRegisterCaptchaKey(normalizedPhone), captcha, REGISTER_CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        RegisterCaptchaVO vo = new RegisterCaptchaVO();
        vo.setPhone_wsh(normalizedPhone);
        vo.setCaptcha_wsh(captcha);
        vo.setExpires_in_seconds_wsh((int) REGISTER_CAPTCHA_TTL_SECONDS);
        return vo;
    }

    /**
     * 【用户登录】
     *
     * 业务作用：用户名密码认证，含登录失败锁定保护
     *
     * 调用场景：用户在登录页提交登录表单
     *
     * 调用链：AuthController ↓ login() → checkLoginAttempt → UserMapper.selectOne → 状态/密码校验 → clearLoginAttempt → buildLoginResponse
     *
     * 数据处理：LoginRequestDTO → checkLoginAttempt(防暴力破解) → 根据username查询用户 → 校验用户状态 → BCrypt.matches校验密码 → clearLoginAttempt → 查询角色编码 → JwtUtil生成令牌对 → LoginResponseVO
     *
     * 业务规则：密码错误或用户名不存在统一提示"用户名或密码错误"防止枚举攻击；连续失败20次锁定15分钟；被封禁用户禁止登录
     *
     * 状态影响：Redis中"login_fail:{username}"递增（失败时）/ 删除（成功时）
     *
     * 异常情况：用户名不存在 → 401 BusinessException；账户被封禁 → 403 BusinessException；密码错误 → 401 BusinessException；账户被锁定 → BusinessException
     *
     * 注意事项：错误提示不区分"用户不存在"和"密码错误"以提高安全性；登录成功后清除失败计数
     */
    @Override
    public LoginResponseVO login(LoginRequestDTO request) {
        checkLoginAttempt(request.getUsername_wsh());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername_wsh, request.getUsername_wsh()));
        if (user == null) {
            recordLoginAttempt(request.getUsername_wsh());
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus_wsh() == StatusCode.USER_BANNED.getValue()) {
            throw new BusinessException(403, "账户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword_wsh(), user.getPassword_wsh())) {
            recordLoginAttempt(request.getUsername_wsh());
            throw new BusinessException(401, "用户名或密码错误");
        }
        clearLoginAttempt(request.getUsername_wsh());
        return buildLoginResponse(user);
    }

    /**
     * 【刷新用户令牌】
     *
     * 业务作用：使用refresh_token换取全新的access_token和refresh_token
     *
     * 调用场景：客户端access_token过期后使用refresh_token自动续期
     *
     * 调用链：TokenController ↓ refreshToken() → JwtUtil.validateToken → isTokenBlacklisted → JwtUtil.getUserIdFromToken → isRefreshTokenBeforeLogout → UserMapper.selectById → buildLoginResponse
     *
     * 数据处理：refreshToken → JWT格式校验 → Redis黑名单查询 → 解析userId → 登出前置时间校验 → 重新查询用户 → 重新生成令牌对 → LoginResponseVO
     *
     * 业务规则：refresh_token必须有效且不在黑名单；登出后该登出时间点前签发的refresh_token失效；用户删除后无法续期
     *
     * 状态影响：无数据库写入；生成全新的JWT令牌对并返回
     *
     * 异常情况：refresh_token无效/过期 → BusinessException；token在黑名单中 → BusinessException；用户已被删除 → BusinessException；token在登出前签发 → BusinessException
     *
     * 注意事项：每次刷新产生全新令牌对，旧refresh_token因issuedAt小于登出时间戳而失效
     */
    @Override
    public LoginResponseVO refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || isTokenBlacklisted(refreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        if (isRefreshTokenBeforeLogout(refreshToken, userId)) {
            throw new BusinessException("鍒锋柊浠ょ墝鏃犳晥");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildLoginResponse(user);
    }

    /**
     * 【用户登出】
     *
     * 业务作用：将当前令牌加入Redis黑名单并记录登出时间戳，阻断后续refresh_token续期
     *
     * 调用场景：用户主动退出登录
     *
     * 调用链：AuthController ↓ logout() → Redis SET黑名单 → rememberUserLogout(记录登出时间戳)
     *
     * 数据处理：token → 判读非空 → Redis SET(blacklist:token:{token}, "1", 1天) → rememberUserLogout(解析token → Redis SET(logout:user:{userId}, 时间戳))
     *
     * 业务规则：token为空或Redis不可用时静默跳过；黑名单有效期1天；登出时间戳有效期等同refresh token过期时间
     *
     * 状态影响：Redis写入 "blacklist:token:{token}" → "1" (1天) + "logout:user:{userId}" → 当前毫秒时间戳
     *
     * 异常情况：token为空或Redis异常均静默处理，不抛异常
     *
     * 注意事项：多端登录时登出仅影响当前token，其他设备仍有效；黑名单过期后同名字符串token可能恢复
     */
    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token) && redisTemplate != null && redisTemplate.opsForValue() != null) {
            redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, "1", 1, TimeUnit.DAYS);
            rememberUserLogout(token);
        }
    }

    /**
     * 【删除当前用户】
     *
     * 业务作用：物理删除用户及其所有角色关联，并将当前令牌加入黑名单
     *
     * 调用场景：用户在安全设置页面确认注销账号
     *
     * 调用链：UserController ↓ deleteCurrentUser() → UserMapper.selectById → 删除所有UserRole → UserMapper.deleteById → 令牌拉黑 → rememberUserLogout
     *
     * 数据处理：userId + token → 查询用户是否存在 → 删除user_role_wsh关联 → 物理删除user_wsh → token加入Redis黑名单(1天) → rememberUserLogout
     *
     * 业务规则：用户不存在直接抛错；先删角色关联再删用户；删除后立即将当前令牌拉黑
     *
     * 状态影响：物理删除user_role_wsh表记录(按userId) + 物理删除user_wsh表记录(按userId) + Redis黑名单 + Redis登出时间戳
     *
     * 异常情况：用户不存在 → BusinessException
     *
     * 注意事项：物理删除不可恢复；事务确保角色关联删除和用户删除的原子性
     */
    @Override
    @Transactional
    public void deleteCurrentUser(Long userId, String token) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUser_id_wsh, userId));
        userMapper.deleteById(userId);

        if (StringUtils.hasText(token) && redisTemplate != null && redisTemplate.opsForValue() != null) {
            redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, "1", 1, TimeUnit.DAYS);
            rememberUserLogout(token);
        }
    }

    /**
     * 【获取用户信息】
     *
     * 业务作用：查询用户基本信息并组装为UserVO（含角色和支付密码状态）
     *
     * 调用场景：用户中心/管理后台需要展示用户资料的各处
     *
     * 调用链：UserController ↓ getUserInfo() → UserMapper.selectById → toUserVO(含RoleMapper查询角色)
     *
     * 数据处理：userId → selectById → 判读是否存在 → 调用toUserVO(内部查询角色编码+判读支付密码是否设置) → UserVO
     *
     * 业务规则：用户不存在则抛异常；VO中包含角色编码列表和支付密码是否已设置的标志
     *
     * 状态影响：无
     *
     * 异常情况：用户不存在 → BusinessException
     *
     * 注意事项：此方法不返回支付密码密文，仅返回布尔标志
     */
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVO(user);
    }

    /**
     * 【更新用户信息】
     *
     * 业务作用：部分字段更新用户个人资料，仅修改VO中非空的字段
     *
     * 调用场景：用户在个人设置页修改个人资料
     *
     * 调用链：UserController ↓ updateUser() → UserMapper.selectById → 逐字段判读非空 → UserMapper.updateById → toUserVO
     *
     * 数据处理：userId + UserVO → selectById判读存在 → 逐字段if(StringUtils.hasText/!= null) set → updateById → toUserVO返回
     *
     * 业务规则：仅更新VO中非空/非null字段；修改手机号需normalize并校验唯一性(排除当前用户)；昵称会做HTML转义(在register中)
     *
     * 状态影响：更新user_wsh表对应用户记录
     *
     * 异常情况：用户不存在 → BusinessException；手机号被其他用户占用 → BusinessException
     *
     * 注意事项：事务保证更新原子性；可更新实名状态等敏感字段，建议业务层做权限控制
     */
    @Override
    @Transactional
    public UserVO updateUser(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(vo.getNickname_wsh())) user.setNickname_wsh(vo.getNickname_wsh());
        if (StringUtils.hasText(vo.getPhone_wsh())) {
            String phone = normalizePhone(vo.getPhone_wsh());
            ensurePhoneAvailable(phone, userId);
            user.setPhone_wsh(phone);
        }
        if (StringUtils.hasText(vo.getAvatar_wsh())) user.setAvatar_wsh(vo.getAvatar_wsh());
        if (vo.getGender_wsh() != null) user.setGender_wsh(vo.getGender_wsh());
        if (StringUtils.hasText(vo.getEmail_wsh())) user.setEmail_wsh(vo.getEmail_wsh());
        if (StringUtils.hasText(vo.getReal_name_wsh())) user.setReal_name_wsh(vo.getReal_name_wsh());
        if (StringUtils.hasText(vo.getId_card_no_wsh())) user.setId_card_no_wsh(vo.getId_card_no_wsh());
        if (vo.getReal_name_status_wsh() != null) user.setReal_name_status_wsh(vo.getReal_name_status_wsh());
        if (StringUtils.hasText(vo.getAddress_wsh())) user.setAddress_wsh(vo.getAddress_wsh());
        if (vo.getLatitude_wsh() != null) user.setLatitude_wsh(vo.getLatitude_wsh());
        if (vo.getLongitude_wsh() != null) user.setLongitude_wsh(vo.getLongitude_wsh());
        if (vo.getStatus_wsh() != null) user.setStatus_wsh(vo.getStatus_wsh());
        userMapper.updateById(user);
        return toUserVO(user);
    }

    /**
     * 【设置支付密码】
     *
     * 业务作用：为指定用户设置BCrypt加密后的支付密码
     *
     * 调用场景：用户在安全中心设置/修改支付密码
     *
     * 调用链：UserController ↓ setPaymentPassword() → UserMapper.selectById → passwordEncoder.encode → UserMapper.updateById
     *
     * 数据处理：userId + 明文密码 → selectById → 判读非空 → trim → BCrypt加密 → setPayment_password_wsh → updateById
     *
     * 业务规则：支付密码不能为null或空字符串；密码trim后再加密；覆盖已有支付密码
     *
     * 状态影响：更新user_wsh表的payment_password_wsh字段
     *
     * 异常情况：用户不存在 → 404 BusinessException；密码为空 → 400 BusinessException
     *
     * 注意事项：事务保证；支付密码和登录密码独立存储互不影响
     */
    @Override
    @Transactional
    public void setPaymentPassword(Long userId, String paymentPassword) {
        if (!StringUtils.hasText(paymentPassword)) {
            throw new BusinessException(400, "支付密码不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPayment_password_wsh(passwordEncoder.encode(paymentPassword.trim()));
        userMapper.updateById(user);
    }

    /**
     * 【查询是否已设置支付密码】
     *
     * 业务作用：判断用户的支付密码字段是否已设置
     *
     * 调用场景：支付流程前置判断是否需要引导用户设置支付密码
     *
     * 调用链：XxxController ↓ hasPaymentPassword() → UserMapper.selectById → 判读payment_password字段非空
     *
     * 数据处理：userId → selectById → 判读payment_password_wsh是否StringUtils.hasText → boolean
     *
     * 业务规则：userId为null返回false；用户不存在返回false（不抛异常）；仅判断密文字段是否存在
     *
     * 状态影响：无
     *
     * 异常情况：无（所有边界情况返回false）
     *
     * 注意事项：不验证密码正确性，仅检查是否设置过；验证密码请用verifyPaymentPassword
     */
    @Override
    public boolean hasPaymentPassword(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && StringUtils.hasText(user.getPayment_password_wsh());
    }

    /**
     * 【校验支付密码】
     *
     * 业务作用：验证用户输入的支付密码与数据库中的BCrypt密文是否匹配
     *
     * 调用场景：用户发起支付交易时进行支付密码验证
     *
     * 调用链：PaymentController ↓ verifyPaymentPassword() → UserMapper.selectById → 判读支付密码存在 → passwordEncoder.matches
     *
     * 数据处理：userId + 明文支付密码 → 判读非空 → selectById → 判读支付密码已设置 → trim → BCrypt.matches匹配
     *
     * 业务规则：密码不能为空；必须先设置支付密码才能校验；trim后匹配
     *
     * 状态影响：无
     *
     * 异常情况：密码为空 → 400 BusinessException；用户不存在 → 404 BusinessException；未设置支付密码 → 400 BusinessException；密码错误 → 403 BusinessException
     *
     * 注意事项：当前不记录校验失败次数，无锁定机制；与登录密码校验独立
     */
    @Override
    public void verifyPaymentPassword(Long userId, String paymentPassword) {
        if (!StringUtils.hasText(paymentPassword)) {
            throw new BusinessException(400, "请输入支付密码");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!StringUtils.hasText(user.getPayment_password_wsh())) {
            throw new BusinessException(400, "请先设置支付密码");
        }
        if (!passwordEncoder.matches(paymentPassword.trim(), user.getPayment_password_wsh())) {
            throw new BusinessException(403, "支付密码错误");
        }
    }

    /**
     * 【获取所有用户列表】
     *
     * 业务作用：查询全部用户列表，每用户附带角色编码，最多1000条
     *
     * 调用场景：管理后台用户管理页加载全部用户
     *
     * 调用链：AdminController ↓ listAll() → UserMapper.selectList → 每用户调用roleMapper.selectRoleCodesByUserId → User.toVO → List<UserVO>
     *
     * 数据处理：无入参 → LambdaQueryWrapper.orderByDesc(created_at).last("LIMIT 1000") → selectList → stream遍历 → 每用户查角色 → toVO → collect至List
     *
     * 业务规则：按创建时间倒序排列，最多1000条
     *
     * 状态影响：无
     *
     * 异常情况：无
     *
     * 注意事项：N+1查询问题(每用户查一次角色)，数据量大时性能差，建议用listPage
     */
    @Override
    public List<UserVO> listAll() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreated_at_wsh).last("LIMIT 1000"));
        return users.stream()
                .map(u -> u.toVO(roleMapper.selectRoleCodesByUserId(u.getId_wsh())))
                .collect(Collectors.toList());
    }

    /**
     * 【分页查询用户列表】
     *
     * 业务作用：支持多字段模糊匹配的分页用户查询
     *
     * 调用场景：管理后台用户管理页面的分页搜索
     *
     * 调用链：AdminController ↓ listPage() → UserMapper.selectPage → 遍历每用户查角色编码 → 组装IPage<UserVO>
     *
     * 数据处理：pageParam(page,size) + keyword → 构造Page分页对象 → LambdaQueryWrapper.orderByDesc(created_at) → 若keyword非空则OR like(用户名,昵称,手机号,真实姓名) → selectPage → 转换Page<User>至Page<UserVO>(每用户查角色)
     *
     * 业务规则：keyword为空查全部；keyword非空对4个字段做OR like匹配；按创建时间倒序
     *
     * 状态影响：无
     *
     * 异常情况：无
     *
     * 注意事项：N+1角色查询问题；like %keyword%无法利用索引
     */
    @Override
    public IPage<UserVO> listPage(PageRequestDTO pageParam, String keyword) {
        Page<User> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>().orderByDesc(User::getCreated_at_wsh);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername_wsh, keyword)
                    .or().like(User::getNickname_wsh, keyword)
                    .or().like(User::getPhone_wsh, keyword)
                    .or().like(User::getReal_name_wsh, keyword));
        }
        Page<User> userPage = userMapper.selectPage(page, qw);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
                .map(u -> u.toVO(roleMapper.selectRoleCodesByUserId(u.getId_wsh())))
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 【获取用户角色列表】
     *
     * 业务作用：委托RoleMapper查询用户的所有角色编码
     *
     * 调用场景：权限校验/角色管理
     *
     * 调用链：XxxController ↓ getUserRoles() → RoleMapper.selectRoleCodesByUserId
     *
     * 数据处理：userId → 透传给RoleMapper执行联表SQL → List<String>角色编码
     *
     * 业务规则：仅查询未逻辑删除的角色关联；SQL JOIN role_wsh + user_role_wsh
     *
     * 状态影响：无
     *
     * 异常情况：无
     *
     * 注意事项：直接委托mapper层查询，不做额外业务处理
     */
    @Override
    public List<String> getUserRoles(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    /**
     * 【忘记密码】
     *
     * 业务作用：通过邮箱重置密码，生成8位随机临时密码并加密更新
     *
     * 调用场景：用户忘记密码后通过邮箱验证重置
     *
     * 调用链：AuthController ↓ forgotPassword() → UserMapper.selectOne(email) → 生成8位随机密码 → BCrypt.encode → UserMapper.updateById → 返回明文
     *
     * 数据处理：email → selectOne → 判读用户存在 → SecureRandom生成8位随机密码(大写+小写+数字) → BCrypt.encode → setPassword → updateById → 返回明文密码
     *
     * 业务规则：邮箱不存在则抛错；密码由大小写字母+数字组成；使用BCrypt加密存储
     *
     * 状态影响：更新user_wsh表的password_wsh字段为新密码
     *
     * 异常情况：邮箱未找到关联用户 → BusinessException
     *
     * 注意事项：当前实现直接返回明文密码到接口响应中，仅用于开发；生产环境应通过邮件发送
     */
    @Override
    public String forgotPassword(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail_wsh, email));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(REGISTER_CAPTCHA_RANDOM.nextInt(chars.length())));
        }
        String tempPassword = sb.toString();
        user.setPassword_wsh(passwordEncoder.encode(tempPassword));
        userMapper.updateById(user);
        return tempPassword;
    }

    @Override
    @Transactional
    public int resetAllPasswords(String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6 || newPassword.length() > 72) {
            throw new BusinessException(400, "密码长度必须为6到72个字符");
        }
        return userMapper.resetAllPasswords(passwordEncoder.encode(newPassword));
    }

    /**
     * 【检查令牌是否在黑名单中】
     *
     * 业务作用：判断JWT令牌是否已被加入Redis黑名单（登出/删除后生效）
     *
     * 调用场景：在刷新token和鉴权时校验令牌是否有效
     *
     * 调用链：refreshToken ↓ isTokenBlacklisted() → Redis GET blacklist:token:{token}
     *
     * 数据处理：token → 拼接Redis key → GET操作 → 判读是否存在 → boolean
     *
     * 业务规则：token为空返回false；Redis异常时静默返回false（不阻断流程）
     *
     * 状态影响：无
     *
     * 异常情况：Redis连接异常等内部错误静默处理，返回false
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            return StringUtils.hasText(token)
                    && redisTemplate != null
                    && redisTemplate.opsForValue() != null
                    && redisTemplate.opsForValue().get(TOKEN_BLACKLIST_PREFIX + token) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 【判断refresh_token是否在登出前签发】
     *
     * 业务作用：防止登出后旧refresh_token被用于续期，实现登出后全部令牌失效
     *
     * 调用场景：refreshToken方法中校验令牌的签发时间
     *
     * 调用链：refreshToken ↓ isRefreshTokenBeforeLogout() → Redis GET logout:user:{userId} → JwtUtil.parseToken.getIssuedAt → 时间比较
     *
     * 数据处理：refreshToken + userId → 从Redis获取登出时间戳 → 解析token的issuedAt → 比较issuedAt<logoutAt
     *
     * 业务规则：登出时间戳不存在则返回false(正常)；issuedAt小于登出时间戳则判定为登出前签发的无效令牌
     *
     * 状态影响：无
     *
     * 异常情况：userId为null或Redis异常时返回false
     */
    private boolean isRefreshTokenBeforeLogout(String refreshToken, Long userId) {
        try {
            if (userId == null || redisTemplate == null || redisTemplate.opsForValue() == null) {
                return false;
            }
            String logoutAtText = redisTemplate.opsForValue().get(USER_LOGOUT_PREFIX + userId);
            if (!StringUtils.hasText(logoutAtText)) {
                return false;
            }
            var issuedAt = jwtUtil.parseToken(refreshToken).getIssuedAt();
            return issuedAt != null && issuedAt.getTime() < Long.parseLong(logoutAtText);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 【记录用户登出时间戳】
     *
     * 业务作用：将当前时间戳存入Redis，使该时间之前签发的所有refresh_token失效
     *
     * 调用场景：用户登出或删除账号时调用
     *
     * 调用链：logout/deleteCurrentUser ↓ rememberUserLogout() → JwtUtil验证token → JwtUtil.getUserIdFromToken → Redis SET登出时间戳
     *
     * 数据处理：token → JWT格式校验 → 解析userId → 获取当前毫秒时间戳 → Redis SET(logout:user:{userId}, 时间戳, TTL=refreshToken有效期)
     *
     * 业务规则：token为空或无效时静默跳过；TTL与refresh_token有效期一致，保证不做无限期存储
     *
     * 状态影响：Redis中写入 "logout:user:{userId}" → 当前毫秒时间戳
     *
     * 异常情况：JWT解析异常或Redis异常时静默处理
     *
     * 注意事项：这是关键安全机制——登出后仅拉黑access_token不够，通过此时间戳确保refresh_token也无法续期
     */
    private void rememberUserLogout(String token) {
        try {
            if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
                return;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 教学注释：这里只记录“这个时间点之前签发的 refresh token 都失效”。
            // 这样登出接口不需要客户端额外上传 refresh token，也能切断旧 refresh token 的续期能力。
            redisTemplate.opsForValue().set(
                    USER_LOGOUT_PREFIX + userId,
                    String.valueOf(System.currentTimeMillis()),
                    jwtUtil.getRefreshTokenExpiration(),
                    TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
    }

    /**
     * 【检查登录尝试次数】
     *
     * 业务作用：防暴力破解，检查当前用户是否因失败次数过多而被锁定
     *
     * 调用场景：每次登录请求开始时调用
     *
     * 调用链：login ↓ checkLoginAttempt() → Redis GET login_fail:{username} 或 本地ConcurrentHashMap查询
     *
     * 数据处理：username → 优先从Redis获取失败次数 → 若≥20则检查锁定时间是否仍在有效期内 → 本地缓存兜底
     *
     * 业务规则：失败次数≥20且锁定时间未过期才阻止登录；Redis不可用时使用本地ConcurrentHashMap兜底
     *
     * 状态影响：无
     *
     * 异常情况：失败次数超限 → BusinessException("账户已被锁定，请15分钟后再试")
     *
     * 注意事项：本地缓存方案在应用重启后锁定状态会丢失；Redis方案可跨实例共享
     */
    private void checkLoginAttempt(String username) {
        if (redisTemplate != null) {
            var ops = redisTemplate.opsForValue();
            if (ops != null) {
                String failCount = ops.get("login_fail:" + username);
                if (failCount != null && Integer.parseInt(failCount) >= MAX_LOGIN_ATTEMPTS) {
                    throw new BusinessException("账户已被锁定，请 15 分钟后再试");
                }
            }
        } else {
            LoginAttempt attempt = localLoginAttempts.get(username);
            if (attempt != null && System.currentTimeMillis() < attempt.lockUntil) {
                throw new BusinessException("账户已被锁定，请 15 分钟后再试");
            }
        }
    }

    /**
     * 【记录登录失败次数】
     *
     * 业务作用：递增登录失败计数，达到阈值时锁定用户
     *
     * 调用场景：登录验证失败（用户名不存在或密码错误）时调用
     *
     * 调用链：login → 密码校验失败 → recordLoginAttempt → Redis INCR login_fail:{username} 或 本地计数递增
     *
     * 数据处理：username → Redis INCR递增 → 设置key过期时间15分钟 → 失败次数≥20时设置lockUntil未来时间戳
     *
     * 业务规则：Redis模式使用INCR+EXPIRE原子操作；本地模式用ConcurrentHashMap.compute原子更新；锁定后lockUntil=当前时间+15min
     *
     * 状态影响：Redis中 "login_fail:{username}" 递增并刷新TTL为15分钟
     *
     * 异常情况：无（内部异常不抛给上层）
     *
     * 注意事项：锁定时间从第一次失败开始计算15分钟窗口，非到达20次才开始计时
     */
    private void recordLoginAttempt(String username) {
        if (redisTemplate != null && redisTemplate.opsForValue() != null) {
            String lockKey = "login_fail:" + username;
            redisTemplate.opsForValue().increment(lockKey);
            redisTemplate.expire(lockKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        } else {
            localLoginAttempts.compute(username, (k, v) -> {
                if (v == null || System.currentTimeMillis() >= v.lockUntil) {
                    return new LoginAttempt(1, 0);
                }
                int newCount = v.count + 1;
                long lockUntil = newCount >= MAX_LOGIN_ATTEMPTS
                        ? System.currentTimeMillis() + LOCK_DURATION_MINUTES * 60 * 1000
                        : 0;
                return new LoginAttempt(newCount, lockUntil);
            });
        }
    }

    /**
     * 【清除登录失败记录】
     *
     * 业务作用：登录成功后清除该用户的失败计数和锁定状态
     *
     * 调用场景：用户登录成功时调用
     *
     * 调用链：login → 密码校验成功 → clearLoginAttempt → Redis DELETE 或 本地Map.remove
     *
     * 数据处理：username → Redis删除"login_fail:{username}"key 或 本地ConcurrentHashMap.remove
     *
     * 业务规则：Redis和本地两种模式分别清理，保证下次登录从零开始计数
     *
     * 状态影响：删除Redis中/login失败计数记录
     *
     * 异常情况：无（内部异常不抛给上层）
     */
    private void clearLoginAttempt(String username) {
        if (redisTemplate != null && redisTemplate.opsForValue() != null) {
            redisTemplate.delete("login_fail:" + username);
        } else {
            localLoginAttempts.remove(username);
        }
    }

    /**
     * 【手机号规范化】
     *
     * 业务作用：校验手机号不为空并trim前后空格
     *
     * 调用场景：注册、更新用户信息等操作中统一处理手机号
     *
     * 数据处理：phone → 判读非空 → trim → 返回规范化手机号
     *
     * 异常情况：phone为null或空字符串 → BusinessException("手机号不能为空")
     */
    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException("手机号不能为空");
        }
        return phone.trim();
    }

    /**
     * 【验证码规范化】
     *
     * 业务作用：校验验证码不为空并trim前后空格
     *
     * 调用场景：注册时处理用户输入的验证码
     *
     * 数据处理：captcha → 判读非空 → trim → 返回规范化验证码
     *
     * 异常情况：captcha为null或空字符串 → BusinessException("验证码不能为空")
     */
    private String normalizeCaptcha(String captcha) {
        if (!StringUtils.hasText(captcha)) {
            throw new BusinessException("验证码不能为空");
        }
        return captcha.trim();
    }

    /**
     * 【构建注册验证码Redis键】
     *
     * 业务作用：统一管理注册验证码在Redis中的key格式
     *
     * 数据处理：phone → "register_captcha:" + phone → 返回Redis key
     */
    private String buildRegisterCaptchaKey(String phone) {
        return "register_captcha:" + phone;
    }

    /**
     * 【生成注册验证码】
     *
     * 业务作用：使用SecureRandom生成6位数字验证码
     *
     * 数据处理：SecureRandom.nextInt(1_000_000) → String.format("%06d")补零 → 返回6位数字字符串
     *
     * 注意事项：使用SecureRandom而非Random，保证随机数的安全性
     */
    private String generateRegisterCaptcha() {
        return String.format("%06d", REGISTER_CAPTCHA_RANDOM.nextInt(1_000_000));
    }

    /**
     * 【校验注册验证码】
     *
     * 业务作用：从Redis获取存储的验证码并与用户输入比较
     *
     * 调用场景：注册时校验用户输入的验证码是否正确
     *
     * 调用链：register ↓ verifyRegisterCaptcha() → Redis GET register_captcha:{phone} → equals比较
     *
     * 数据处理：phone + captcha → 拼接Redis key → GET → 判读存储值是否与用户输入一致
     *
     * 业务规则：Redis中无验证码或验证码不匹配均视为无效；验证码使用一次后在下个步骤(register)中会被清除
     *
     * 异常情况：验证码错误或已过期 → BusinessException
     */
    private void verifyRegisterCaptcha(String phone, String captcha) {
        String stored = redisTemplate.opsForValue().get(buildRegisterCaptchaKey(phone));
        if (!StringUtils.hasText(stored) || !stored.equals(captcha)) {
            throw new BusinessException("验证码错误或已过期");
        }
    }

    /**
     * 【清除注册验证码】
     *
     * 业务作用：注册成功后删除Redis中的验证码，防止重复使用
     *
     * 调用场景：注册成功/注册失败(用户名重复)时均需清除
     *
     * 调用链：register ↓ clearRegisterCaptcha() → Redis DELETE register_captcha:{phone}
     *
     * 数据处理：phone → 拼接Redis key → DELETE
     *
     * 业务规则：无论注册成功还是失败都清除验证码，保证验证码单次使用
     */
    private void clearRegisterCaptcha(String phone) {
        if (redisTemplate != null) {
            redisTemplate.delete(buildRegisterCaptchaKey(phone));
        }
    }

    /**
     * 【确保手机号可用】
     *
     * 业务作用：检查手机号是否已被其他用户绑定
     *
     * 调用场景：注册或修改手机号时校验手机号唯一性
     *
     * 调用链：register/updateUser ↓ ensurePhoneAvailable() → UserMapper.selectOne → 判读存在且非当前用户
     *
     * 数据处理：phone + currentUserId(可null) → selectOne查询该手机号用户 → 判读存在且不是本人 → 抛错/通过
     *
     * 业务规则：手机号未被占用则通过；已被占用时若currentUserId匹配本人（更新操作）则通过，否则抛错
     *
     * 异常情况：手机号已被其他用户绑定 → BusinessException("手机号已被绑定")
     */
    private void ensurePhoneAvailable(String phone, Long currentUserId) {
        User existed = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone_wsh, phone).last("LIMIT 1"));
        if (existed != null && (currentUserId == null || !currentUserId.equals(existed.getId_wsh()))) {
            throw new BusinessException("手机号已被绑定");
        }
    }

    /**
     * 【构建登录响应】
     *
     * 业务作用：根据用户信息生成access_token、refresh_token并组装LoginResponseVO
     *
     * 调用场景：注册成功和登录成功后调用
     *
     * 调用链：register/login/refreshToken ↓ buildLoginResponse() → roleMapper.selectRoleCodesByUserId → JwtUtil.generateAccessToken → JwtUtil.generateRefreshToken → 组装VO
     *
     * 数据处理：User → 查询角色编码 → 生成access_token(含userId+username+roles) → 生成refresh_token(含userId) → 组装LoginResponseVO(含用户基本信息+角色+令牌)
     *
     * 业务规则：access_token携带用户基础信息和角色用于鉴权；refresh_token仅携带userId用于续期
     *
     * 状态影响：无
     */
    private LoginResponseVO buildLoginResponse(User user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId_wsh());
        String accessToken = jwtUtil.generateAccessToken(user.getId_wsh(), user.getUsername_wsh(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId_wsh());

        LoginResponseVO response = new LoginResponseVO();
        response.setAccess_token_wsh(accessToken);
        response.setRefresh_token_wsh(refreshToken);
        response.setUser_id_wsh(user.getId_wsh());
        response.setUsername_wsh(user.getUsername_wsh());
        response.setNickname_wsh(user.getNickname_wsh());
        response.setAvatar_wsh(user.getAvatar_wsh());
        response.setRoles_wsh(roles);
        return response;
    }

    /**
     * 【用户实体转VO】
     *
     * 业务作用：将User实体转换为UserVO视图对象，补充角色编码和支付密码状态
     *
     * 调用场景：getUserInfo、updateUser等方法返回用户信息时
     *
     * 调用链：getUserInfo/updateUser ↓ toUserVO() → 属性拷贝 → roleMapper.selectRoleCodesByUserId → UserVO
     *
     * 数据处理：User → 逐字段拷贝至UserVO → 查询角色编码列表 → 判读支付密码是否已设置 → 返回完整UserVO
     *
     * 注意事项：支付密码不暴露密文，仅通过payment_password_set_wsh布尔值标识是否已设置
     */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId_wsh(user.getId_wsh());
        vo.setUsername_wsh(user.getUsername_wsh());
        vo.setNickname_wsh(user.getNickname_wsh());
        vo.setPhone_wsh(user.getPhone_wsh());
        vo.setAvatar_wsh(user.getAvatar_wsh());
        vo.setGender_wsh(user.getGender_wsh());
        vo.setEmail_wsh(user.getEmail_wsh());
        vo.setReal_name_wsh(user.getReal_name_wsh());
        vo.setId_card_no_wsh(user.getId_card_no_wsh());
        vo.setReal_name_status_wsh(user.getReal_name_status_wsh());
        vo.setAddress_wsh(user.getAddress_wsh());
        vo.setLatitude_wsh(user.getLatitude_wsh());
        vo.setLongitude_wsh(user.getLongitude_wsh());
        vo.setStatus_wsh(user.getStatus_wsh());
        vo.setCreated_at_wsh(user.getCreated_at_wsh());
        vo.setRoles_wsh(roleMapper.selectRoleCodesByUserId(user.getId_wsh()));
        vo.setPayment_password_set_wsh(StringUtils.hasText(user.getPayment_password_wsh()));
        return vo;
    }
}
