package com.pet.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.system.dto.LoginRequestDTO;
import com.pet.system.dto.RegisterCaptchaRequestDTO;
import com.pet.system.dto.RegisterRequestDTO;
import com.pet.system.vo.LoginResponseVO;
import com.pet.system.vo.RegisterCaptchaVO;
import com.pet.system.vo.UserVO;
import com.pet.system.entity.User;

import java.util.List;

public interface UserService {

    /**
     * 【用户注册】
     *
     * 业务作用：创建新用户账号并完成自动登录
     *
     * 调用场景：用户在注册页面填写手机号、验证码、密码等信息后提交
     *
     * 调用链：RegisterController ↓ register() ↓ UserMapper.insert / RoleMapper / UserRoleMapper
     *
     * 数据处理：RegisterRequestDTO → 校验验证码 → 加密密码 → insert用户 → 分配OWNER角色 → LoginResponseVO
     *
     * 业务规则：手机号需唯一且通过验证码校验；密码BCrypt加密存储；自动分配OWNER角色；用户名唯一（DuplicateKey保护）
     *
     * 状态影响：新增User记录 + 新增UserRole关联记录
     *
     * 异常情况：验证码错误/过期 → BusinessException；用户名重复 → BusinessException；手机号已被绑定 → BusinessException
     *
     * 注意事项：注册成功后自动登录并颁发JWT，无需再次调用login接口
     */
    LoginResponseVO register(RegisterRequestDTO request);

    /**
     * 【请求注册验证码】
     *
     * 业务作用：向指定手机号生成注册验证码（开发环境直接返回验证码供调试）
     *
     * 调用场景：用户在注册页点击"获取验证码"按钮
     *
     * 调用链：RegisterController ↓ requestRegisterCaptcha() ↓ Redis缓存验证码
     *
     * 数据处理：phone → 校验手机号可用性 → 生成6位随机验证码 → 存入Redis(5分钟) → RegisterCaptchaVO
     *
     * 业务规则：手机号不能为空且未被已有用户绑定；验证码有效期5分钟；同手机号重复请求会覆盖旧验证码
     *
     * 状态影响：Redis中写入/覆盖该手机号的验证码缓存，5分钟后自动过期
     *
     * 异常情况：手机号为空 → BusinessException；手机号已被绑定 → BusinessException
     *
     * 注意事项：当前实现直接返回验证码明文到响应中，生产环境应通过短信网关发送且不返回明文
     */
    RegisterCaptchaVO requestRegisterCaptcha(String phone);

    /**
     * 【用户登录】
     *
     * 业务作用：用户名密码认证并签发JWT访问令牌和刷新令牌
     *
     * 调用场景：用户在登录页提交用户名和密码完成身份认证
     *
     * 调用链：AuthController ↓ login() ↓ UserMapper.selectOne → 密码校验 → JwtUtil生成令牌
     *
     * 数据处理：LoginRequestDTO → 查询用户 → 校验密码 → 查询角色编码 → 生成access_token + refresh_token → LoginResponseVO
     *
     * 业务规则：连续登录失败20次锁定15分钟；被封禁账户禁止登录；密码BCrypt匹配校验
     *
     * 状态影响：无数据库写入；Redis中写入登录失败计数（锁定期内递增）
     *
     * 异常情况：用户名不存在 → 401 BusinessException；密码错误 → 401 BusinessException；账户被封禁 → 403 BusinessException；账户被锁定 → BusinessException
     *
     * 注意事项：登录成功后清除该用户的登录失败计数；access_token和refresh_token有效期不同
     */
    LoginResponseVO login(LoginRequestDTO request);

    /**
     * 【刷新用户令牌】
     *
     * 业务作用：使用refresh_token换取新的access_token和refresh_token，实现无感续期
     *
     * 调用场景：access_token过期时，客户端使用refresh_token自动续期，避免用户重登录
     *
     * 调用链：TokenController ↓ refreshToken() → JwtUtil验证 → 黑名单校验 → UserMapper.selectById → JwtUtil重新生成令牌
     *
     * 数据处理：refreshToken → JWT校验 → 黑名单/登出前置校验 → 查询用户 → 重新生成令牌对 → LoginResponseVO
     *
     * 业务规则：refresh_token必须在有效期内且未被拉黑；已在用户登出前签发的refresh_token视为无效
     *
     * 状态影响：无数据库写入；生成全新的JWT令牌对
     *
     * 异常情况：refresh_token无效/过期 → BusinessException；用户已被删除 → BusinessException；令牌在登出前签发 → BusinessException
     *
     * 注意事项：每次刷新都返回全新的令牌对，旧的refresh_token虽未主动吊销但因颁发时间校验不可再用
     */
    LoginResponseVO refreshToken(String refreshToken);

    /**
     * 【用户登出】
     *
     * 业务作用：使当前JWT令牌失效并记录登出时间戳，阻断refresh_token续期
     *
     * 调用场景：用户点击退出登录按钮主动结束当前会话
     *
     * 调用链：AuthController ↓ logout() → Redis黑名单写入 → Redis登出时间戳记录
     *
     * 数据处理：token → 加入Redis黑名单(有效期1天) → 解析token获取userId → 记录登出时间戳 → 无返回值
     *
     * 业务规则：令牌加入黑名单后鉴权中间件会拦截；登出时间戳用于使该时间之前签发的refresh_token失效
     *
     * 状态影响：Redis中写入黑名单记录（1天过期）+ 写入用户最近登出时间戳
     *
     * 异常情况：token为空或JWT解析异常时静默处理，不阻断登出流程
     *
     * 注意事项：登出仅影响当前令牌，不会阻断其他设备（多端登录需各自登出）
     */
    void logout(String token);

    /**
     * 【删除当前用户】
     *
     * 业务作用：注销当前登录用户账号（物理删除用户记录及所有角色关联）
     *
     * 调用场景：用户在安全设置页面申请注销账号
     *
     * 调用链：UserController ↓ deleteCurrentUser() → UserRoleMapper.delete → UserMapper.deleteById → Redis令牌黑名单
     *
     * 数据处理：userId + token → 查询用户是否存在 → 删除user_role关联 → 物理删除user → 令牌拉黑 → 无返回值
     *
     * 业务规则：只能删除已存在的用户；先清除角色关联再删除用户主体；删除后当前令牌加入黑名单
     *
     * 状态影响：物理删除user_role_wsh表关联记录 + 物理删除user_wsh表记录 + Redis令牌黑名单
     *
     * 异常情况：用户不存在 → BusinessException
     *
     * 注意事项：此为物理删除不可恢复，调用前应有二次确认机制；删除后该用户的所有数据将丢失
     */
    void deleteCurrentUser(Long userId, String token);

    /**
     * 【获取用户信息】
     *
     * 业务作用：根据用户ID查询完整的用户个人资料
     *
     * 调用场景：用户中心/个人主页/订单页等需要展示用户信息的页面
     *
     * 调用链：UserController ↓ getUserInfo() → UserMapper.selectById → RoleMapper查询角色编码
     *
     * 数据处理：userId → 查询用户基本信息 → 查询角色编码列表 → 组装UserVO（含支付密码是否已设置标志）
     *
     * 业务规则：返回的UserVO包含角色编码列表和支付密码是否已设置的布尔标志
     *
     * 状态影响：无
     *
     * 异常情况：用户不存在 → BusinessException
     *
     * 注意事项：支付密码字段不会暴露密文，仅通过布尔值标识是否已设置
     */
    UserVO getUserInfo(Long userId);

    /**
     * 【更新用户信息】
     *
     * 业务作用：部分更新用户个人资料，仅修改VO中非空的字段
     *
     * 调用场景：用户在个人设置页修改昵称、头像、手机号、地址、性别等信息
     *
     * 调用链：UserController ↓ updateUser() → UserMapper.selectById → 逐字段非空判断 → UserMapper.updateById
     *
     * 数据处理：userId + UserVO → 查询用户 → 遍历判断VO中非空字段并set → update写入数据库 → 重新组装UserVO返回
     *
     * 业务规则：仅更新VO中非null/非空字符串的字段，未传字段保持原值；修改手机号需校验唯一性（排除当前用户）
     *
     * 状态影响：更新user_wsh表中对应用户记录的字段
     *
     * 异常情况：用户不存在 → BusinessException；手机号已被其他用户绑定 → BusinessException
     *
     * 注意事项：可直接更新实名认证相关字段，需确保调用方有权限；建议对敏感字段做变更权限控制
     */
    UserVO updateUser(Long userId, UserVO vo);

    /**
     * 【设置支付密码】
     *
     * 业务作用：为用户设置或修改支付密码，BCrypt加密后存储
     *
     * 调用场景：用户在安全中心首次设置支付密码或修改已有支付密码
     *
     * 调用链：UserController ↓ setPaymentPassword() → UserMapper.selectById → passwordEncoder.encode → UserMapper.updateById
     *
     * 数据处理：userId + 明文支付密码 → 查询用户 → trim → BCrypt加密 → update写入payment_password字段 → 无返回值
     *
     * 业务规则：支付密码不能为空字符串；密码前后空格会被trim掉再加密存储
     *
     * 状态影响：更新user_wsh表的payment_password_wsh字段
     *
     * 异常情况：用户不存在 → 404 BusinessException；支付密码为空 → 400 BusinessException
     *
     * 注意事项：支付密码与登录密码使用相同的BCrypt加密算法但存储在不同字段，两者互不影响
     */
    void setPaymentPassword(Long userId, String paymentPassword);

    /**
     * 【查询是否已设置支付密码】
     *
     * 业务作用：判断用户是否已设置支付密码，不验证密码正确性
     *
     * 调用场景：在发起支付交易前判断是否需要引导用户先设置支付密码
     *
     * 调用链：PaymentController / UserController ↓ hasPaymentPassword() → UserMapper.selectById
     *
     * 数据处理：userId → 查询用户 → 判读payment_password字段是否非空 → 返回boolean
     *
     * 业务规则：userId为null时直接返回false不抛异常；仅判断字段非空，不校验密码是否正确
     *
     * 状态影响：无
     *
     * 异常情况：无（用户不存在时返回false而非抛异常）
     *
     * 注意事项：不要用此方法验证密码正确性，密码校验请调用verifyPaymentPassword
     */
    boolean hasPaymentPassword(Long userId);

    /**
     * 【校验支付密码】
     *
     * 业务作用：验证用户输入的支付密码是否正确，用于支付交易授权
     *
     * 调用场景：用户发起支付、确认交易等需要验证支付密码的场景
     *
     * 调用链：PaymentController ↓ verifyPaymentPassword() → UserMapper.selectById → passwordEncoder.matches
     *
     * 数据处理：userId + 明文支付密码 → 查询用户 → 判读是否已设置密码 → BCrypt匹配校验 → 无返回值（成功静默返回）
     *
     * 业务规则：支付密码不能为空；必须先设置支付密码才能校验；使用BCrypt匹配算法校验
     *
     * 状态影响：无
     *
     * 异常情况：支付密码为空 → 400 BusinessException；用户不存在 → 404 BusinessException；未设置支付密码 → 400 BusinessException；密码错误 → 403 BusinessException
     *
     * 注意事项：当前不记录校验失败次数，无锁定机制；支付密码错误与登录密码锁定互不影响
     */
    void verifyPaymentPassword(Long userId, String paymentPassword);

    /**
     * 【获取所有用户列表】
     *
     * 业务作用：查询全部用户列表（最多返回1000条），含角色信息
     *
     * 调用场景：后台管理系统的用户管理页面展示全部注册用户
     *
     * 调用链：AdminController ↓ listAll() → UserMapper.selectList → 遍历调用User.toVO(roles)
     *
     * 数据处理：无入参 → 查询所有用户(按创建时间倒序, LIMIT 1000) → 每用户查询角色编码 → List<UserVO>
     *
     * 业务规则：最多返回1000条记录防止OOM；按创建时间倒序排列；每个UserVO附带角色编码列表
     *
     * 状态影响：无
     *
     * 异常情况：无
     *
     * 注意事项：数据量大时建议使用listPage分页查询代替全量加载
     */
    List<UserVO> listAll();

    /**
     * 【分页查询用户列表】
     *
     * 业务作用：按关键词分页模糊搜索用户，支持多字段匹配
     *
     * 调用场景：后台管理系统用户管理页面的分页搜索功能
     *
     * 调用链：AdminController ↓ listPage() → UserMapper.selectPage → 遍历User.toVO(roles) → IPage<UserVO>
     *
     * 数据处理：pageParam + keyword → 构造LambdaQueryWrapper → 模糊匹配(用户名/昵称/手机号/真实姓名) → MyBatis-Plus分页查询 → 每用户追加角色编码 → IPage<UserVO>
     *
     * 业务规则：keyword为空时查询全部不加where条件；keyword非空时对四个字段做OR like匹配；按创建时间倒序排列
     *
     * 状态影响：无
     *
     * 异常情况：无
     *
     * 注意事项：模糊查询使用like %keyword%而非全文索引，数据量大时可能有性能问题，建议后续引入ES
     */
    IPage<UserVO> listPage(PageRequestDTO pageParam, String keyword);

    /**
     * 【获取用户角色列表】
     *
     * 业务作用：查询指定用户拥有的所有角色编码（code），用于权限标识
     *
     * 调用场景：权限校验拦截器、用户角色展示、管理后台角色分配查看
     *
     * 调用链：XxxController ↓ getUserRoles() → RoleMapper.selectRoleCodesByUserId
     *
     * 数据处理：userId → SQL联表查询(role_wsh JOIN user_role_wsh) → 返回角色编码字符串列表
     *
     * 业务规则：仅返回未逻辑删除的角色和用户-角色关联；角色编码如"OWNER"、"ADMIN"、"STAFF"等
     *
     * 状态影响：无
     *
     * 异常情况：无（用户不存在或没有任何角色时返回空列表而非抛异常）
     *
     * 注意事项：返回的是角色编码(code)而非角色名称(name)，用于JWT中的权限标识和接口鉴权
     */
    List<String> getUserRoles(Long userId);

    /**
     * 【忘记密码】
     *
     * 业务作用：通过邮箱重置用户密码，生成随机临时密码并BCrypt加密更新
     *
     * 调用场景：用户在登录页点击"忘记密码"并提交已注册的邮箱
     *
     * 调用链：AuthController ↓ forgotPassword() → UserMapper.selectOne → 生成8位随机密码 → BCrypt加密 → UserMapper.updateById
     *
     * 数据处理：email → 查询用户 → 生成8位随机字母数字密码(大小写+数字) → BCrypt加密 → 更新数据库 → 返回明文临时密码
     *
     * 业务规则：邮箱必须关联已存在的用户；新密码为8位随机字符串(大写字母+小写字母+数字)
     *
     * 状态影响：更新user_wsh表的password_wsh字段为新密码
     *
     * 异常情况：邮箱未找到关联用户 → BusinessException
     *
     * 注意事项：当前实现直接返回临时密码明文，仅为开发调试用途；生产环境应对接邮件服务发送密码而非直接返回
     */
    String forgotPassword(String email);

    int resetAllPasswords(String newPassword);
}
