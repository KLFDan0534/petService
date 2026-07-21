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

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token) && redisTemplate != null && redisTemplate.opsForValue() != null) {
            redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, "1", 1, TimeUnit.DAYS);
            rememberUserLogout(token);
        }
    }

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

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVO(user);
    }

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

    @Override
    public boolean hasPaymentPassword(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && StringUtils.hasText(user.getPayment_password_wsh());
    }

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

    @Override
    public List<UserVO> listAll() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreated_at_wsh).last("LIMIT 1000"));
        return users.stream()
                .map(u -> u.toVO(roleMapper.selectRoleCodesByUserId(u.getId_wsh())))
                .collect(Collectors.toList());
    }

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

    @Override
    public List<String> getUserRoles(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId);
    }

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

    private void clearLoginAttempt(String username) {
        if (redisTemplate != null && redisTemplate.opsForValue() != null) {
            redisTemplate.delete("login_fail:" + username);
        } else {
            localLoginAttempts.remove(username);
        }
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException("手机号不能为空");
        }
        return phone.trim();
    }

    private String normalizeCaptcha(String captcha) {
        if (!StringUtils.hasText(captcha)) {
            throw new BusinessException("验证码不能为空");
        }
        return captcha.trim();
    }

    private String buildRegisterCaptchaKey(String phone) {
        return "register_captcha:" + phone;
    }

    private String generateRegisterCaptcha() {
        return String.format("%06d", REGISTER_CAPTCHA_RANDOM.nextInt(1_000_000));
    }

    private void verifyRegisterCaptcha(String phone, String captcha) {
        String stored = redisTemplate.opsForValue().get(buildRegisterCaptchaKey(phone));
        if (!StringUtils.hasText(stored) || !stored.equals(captcha)) {
            throw new BusinessException("验证码错误或已过期");
        }
    }

    private void clearRegisterCaptcha(String phone) {
        if (redisTemplate != null) {
            redisTemplate.delete(buildRegisterCaptchaKey(phone));
        }
    }

    private void ensurePhoneAvailable(String phone, Long currentUserId) {
        User existed = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone_wsh, phone).last("LIMIT 1"));
        if (existed != null && (currentUserId == null || !currentUserId.equals(existed.getId_wsh()))) {
            throw new BusinessException("手机号已被绑定");
        }
    }

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
