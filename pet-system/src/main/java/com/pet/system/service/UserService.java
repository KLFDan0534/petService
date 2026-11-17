package com.pet.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.system.dto.LoginRequestDTO;
import com.pet.system.dto.RegisterRequestDTO;
import com.pet.system.vo.LoginResponseVO;
import com.pet.system.vo.RegisterCaptchaVO;
import com.pet.system.vo.UserVO;
import com.pet.system.entity.User;

import java.util.List;

public interface UserService {

    /**
     * 业务作用：创建新用户账号并完成自动登录
     * 数据处理：RegisterRequestDTO → 校验验证码 → 加密密码 → insert用户 → 分配OWNER角色 → LoginResponseVO
     * 业务规则：手机号需唯一且通过验证码校验；密码BCrypt加密存储；自动分配OWNER角色；用户名唯一（DuplicateKey保护）
     * 异常情况：验证码错误/过期 → BusinessException；用户名重复 → BusinessException；手机号已被绑定 → BusinessException
     * 注意事项：注册成功后自动登录并颁发JWT，无需再次调用login接口
     */
    LoginResponseVO register(RegisterRequestDTO request);

    /**
     * 业务作用：向指定手机号生成注册验证码（开发环境直接返回验证码供调试）
     * 业务规则：手机号不能为空且未被已有用户绑定；验证码有效期5分钟；同手机号重复请求会覆盖旧验证码
     * 异常情况：手机号为空 → BusinessException；手机号已被绑定 → BusinessException
     * 注意事项：当前实现直接返回验证码明文到响应中，生产环境应通过短信网关发送且不返回明文
     */
    RegisterCaptchaVO requestRegisterCaptcha(String phone);

    /**
     * 业务作用：用户名密码认证并签发JWT访问令牌和刷新令牌
     * 业务规则：连续登录失败20次锁定15分钟；被封禁账户禁止登录；密码BCrypt匹配校验
     * 异常情况：
     * 用户名不存在 → 401 BusinessException；密码错误 → 401 BusinessException；账户被封禁 → 403 BusinessException；账户被锁定 → BusinessException
     * 注意事项：登录成功后清除该用户的登录失败计数；access_token和refresh_token有效期不同
     */
    LoginResponseVO login(LoginRequestDTO request);

    /**
     * 业务作用：使用refresh_token换取新的access_token和refresh_token，实现无感续期
     * 调用场景：access_token过期时，客户端使用refresh_token自动续期，避免用户重登录
     * 业务规则：refresh_token必须在有效期内且未被拉黑；已在用户登出前签发的refresh_token视为无效
     * 异常情况：refresh_token无效/过期 → BusinessException；用户已被删除 → BusinessException；令牌在登出前签发 → BusinessException
     * 注意事项：每次刷新都返回全新的令牌对，旧的refresh_token虽未主动吊销但因颁发时间校验不可再用
     */
    LoginResponseVO refreshToken(String refreshToken);

    /**
     * 业务作用：使当前JWT令牌失效并记录登出时间戳，阻断refresh_token续期
     * 业务规则：令牌加入黑名单后鉴权中间件会拦截；登出时间戳用于使该时间之前签发的refresh_token失效
     * 异常情况：token为空或JWT解析异常时静默处理，不阻断登出流程
     * 注意事项：登出仅影响当前令牌，不会阻断其他设备（多端登录需各自登出）
     */
    void logout(String token);

    /**
     * 业务作用：注销当前登录用户账号（物理删除用户记录及所有角色关联）
     * 业务规则：只能删除已存在的用户；先清除角色关联再删除用户主体；删除后当前令牌加入黑名单
     * 异常情况：用户不存在 → BusinessException
     * 注意事项：此为物理删除不可恢复，调用前应有二次确认机制；删除后该用户的所有数据将丢失
     */
    void deleteCurrentUser(Long userId, String token);

    /**
     * 业务作用：根据用户ID查询完整的用户个人资料
     * 业务规则：返回的UserVO包含角色编码列表和支付密码是否已设置的布尔标志
     * 异常情况：用户不存在 → BusinessException
     * 注意事项：支付密码字段不会暴露密文，仅通过布尔值标识是否已设置
     */
    UserVO getUserInfo(Long userId);

    /**
     * 业务作用：按 ID 返回用户实体本身（不脱敏、不组装 VO），供服务端内部业务使用。
     * 业务规则：**不得直接暴露给前端接口**。getUserInfo() 返回的 VO 会对手机号/邮箱脱敏， 需要真实号码或内部字段时用本方法。
     * 异常情况：无（用户不存在返回 null，由调用方决定语义）
     */
    User getById(Long userId);

    /**
     * 业务作用：部分更新用户个人资料，仅修改VO中非空的字段
     * 业务规则：仅更新VO中非null/非空字符串的字段，未传字段保持原值；修改手机号需校验唯一性（排除当前用户）
     * 异常情况：用户不存在 → BusinessException；手机号已被其他用户绑定 → BusinessException
     * 注意事项：可直接更新实名认证相关字段，需确保调用方有权限；建议对敏感字段做变更权限控制
     */
    UserVO updateUser(Long userId, UserVO vo);

    /**
     * 业务作用：为用户设置或修改支付密码，BCrypt加密后存储
     * 数据处理：userId + 明文支付密码 → 查询用户 → trim → BCrypt加密 → update写入payment_password字段 → 无返回值
     * 业务规则：支付密码不能为空字符串；密码前后空格会被trim掉再加密存储
     * 异常情况：用户不存在 → 404 BusinessException；支付密码为空 → 400 BusinessException
     * 注意事项：支付密码与登录密码使用相同的BCrypt加密算法但存储在不同字段，两者互不影响
     */
    void setPaymentPassword(Long userId, String paymentPassword);

    /**
     * 业务作用：判断用户是否已设置支付密码，不验证密码正确性
     * 业务规则：userId为null时直接返回false不抛异常；仅判断字段非空，不校验密码是否正确
     * 异常情况：无（用户不存在时返回false而非抛异常）
     * 注意事项：不要用此方法验证密码正确性，密码校验请调用verifyPaymentPassword
     */
    boolean hasPaymentPassword(Long userId);

    /**
     * 业务作用：验证用户输入的支付密码是否正确，用于支付交易授权
     * 业务规则：支付密码不能为空；必须先设置支付密码才能校验；使用BCrypt匹配算法校验
     * 异常情况：
     * 支付密码为空 → 400 BusinessException；用户不存在 → 404 BusinessException；未设置支付密码 → 400 BusinessException；密码错误 → 403 BusinessException
     * 注意事项：当前不记录校验失败次数，无锁定机制；支付密码错误与登录密码锁定互不影响
     */
    void verifyPaymentPassword(Long userId, String paymentPassword);

    /**
     * 业务作用：查询全部用户列表（最多返回1000条），含角色信息
     * 业务规则：最多返回1000条记录防止OOM；按创建时间倒序排列；每个UserVO附带角色编码列表
     * 注意事项：数据量大时建议使用listPage分页查询代替全量加载
     */
    List<UserVO> listAll();

    /**
     * 业务作用：按关键词分页模糊搜索用户，支持多字段匹配
     * 业务规则：keyword为空时查询全部不加where条件；keyword非空时对四个字段做OR like匹配；按创建时间倒序排列
     * 注意事项：模糊查询使用like %keyword%而非全文索引，数据量大时可能有性能问题，建议后续引入ES
     */
    IPage<UserVO> listPage(PageRequestDTO pageParam, String keyword);

    /**
     * 业务作用：查询指定用户拥有的所有角色编码（code），用于权限标识
     * 业务规则：仅返回未逻辑删除的角色和用户-角色关联；角色编码如"OWNER"、"ADMIN"、"STAFF"等
     * 异常情况：无（用户不存在或没有任何角色时返回空列表而非抛异常）
     * 注意事项：返回的是角色编码(code)而非角色名称(name)，用于JWT中的权限标识和接口鉴权
     */
    List<String> getUserRoles(Long userId);

    /**
     * 业务作用：通过邮箱重置用户密码，生成随机临时密码并BCrypt加密更新
     * 调用链：
     * AuthController ↓ forgotPassword() → UserMapper.selectOne → 生成8位随机密码 → BCrypt加密 → UserMapper.updateById
     * 数据处理：email → 查询用户 → 生成8位随机字母数字密码(大小写+数字) → BCrypt加密 → 更新数据库 → 返回明文临时密码
     * 业务规则：邮箱必须关联已存在的用户；新密码为8位随机字符串(大写字母+小写字母+数字)
     * 异常情况：邮箱未找到关联用户 → BusinessException
     * 注意事项：当前实现直接返回临时密码明文，仅为开发调试用途；生产环境应对接邮件服务发送密码而非直接返回
     */
    String forgotPassword(String email);

    /**
     * 业务作用：声明批量重置所有用户密码的能力。
     * 数据处理：入参为明文新密码，由实现类负责加密。
     * 业务规则：密码长度需在 6 到 72 个字符之间。
     * 异常情况：密码非法 → BusinessException(400)。
     * 注意事项：接口层不校验长度，校验在实现类中完成。
     */
    int resetAllPasswords(String newPassword);
}
