package com.pet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * JWT令牌工具类
 *
 * 业务作用：提供JWT令牌的生成、解析、校验等核心功能，是用户认证体系的基础组件
 * 业务规则：使用HMAC-SHA算法签名，支持Access Token和Refresh Token两种类型
 * 异常情况：令牌过期、签名无效、令牌格式错误时抛出JWT异常
 * 注意事项：密钥通过配置文件注入，生产环境需确保密钥安全且定期轮换
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    /**
     * 业务作用：解析 Base64 密钥并初始化 HMAC 签名器，同时读取两种令牌的有效期配置。
     * 业务规则：密钥长度必须满足 HS512 要求（不少于 256 bit），否则启动失败。
     * 异常情况：密钥为空或长度不足 → 启动期抛异常。
     * 注意事项：密钥变更会使所有已签发令牌立即失效。
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 生成Access Token（访问令牌）
     *
     * 业务作用：为用户生成短期有效的JWT访问令牌，包含用户身份和角色信息，用于API请求的认证鉴权
     * 业务规则：过期时间由配置项 jwt.access-token-expiration 控制（通常15-30分钟）；令牌中嵌入用户ID、用户名、角色列表
     * 异常情况：密钥无效时抛出异常
     * 注意事项：Access Token有效期短，客户端需在过期前使用Refresh Token刷新
     */
    public String generateAccessToken(Long userId, String username, List<String> roles) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成Refresh Token（刷新令牌）
     *
     * 业务作用：生成长期有效的刷新令牌，用于在Access Token过期后获取新的Access Token，避免用户频繁登录
     * 业务规则：过期时间由配置项 jwt.refresh-token-expiration 控制（通常7-30天）；仅包含用户ID，不包含角色信息
     * 异常情况：密钥无效时抛出异常
     * 注意事项：Refresh Token有效期长，需在服务端维护黑名单机制以支持手动失效
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析JWT令牌
     *
     * 业务作用：对JWT令牌进行验签并解析出令牌中的Claims声明数据，用于后续获取用户身份和权限信息
     * 业务规则：使用配置的HMAC密钥验签；令牌过期或签名不匹配时抛出异常
     * 异常情况：令牌过期(ExpiredJwtException)、签名无效(SignatureException)、格式错误(MalformedJwtException)
     * 注意事项：此方法不处理异常，由调用方负责异常捕获和处理
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验JWT令牌有效性
     *
     * 业务作用：验证JWT令牌是否有效（签名正确且未过期），作为请求认证的第一道关卡
     * 业务规则：解析成功返回true，解析失败（过期/签名无效/格式错误）捕获异常返回false
     * 异常情况：内部捕获所有异常，对外统一返回false
     * 注意事项：此方法只判断令牌是否有效，不校验黑名单状态，黑名单校验需额外调用
     */
    public boolean validateToken(String token) {
        try {
            // 能解析即有效，任何异常统一按无效处理
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从JWT令牌中获取用户ID
     *
     * 业务作用：解析JWT令牌并提取用户ID，用于后续的业务操作标识当前用户
     * 业务规则：令牌的subject存储的是用户ID的字符串形式；要求令牌必须有效，否则抛出异常
     * 异常情况：令牌无效时抛出JWT异常，NumberFormatException（subject不是合法数字时）
     * 注意事项：调用前应确保令牌已通过validateToken校验
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * 获取Refresh Token过期时间配置
     *
     * 业务作用：返回配置的Refresh Token过期时间（毫秒），供其他组件获取刷新令牌的有效期
     * 业务规则：值来源于配置文件 jwt.refresh-token-expiration
     * 注意事项：返回值单位是毫秒，与Spring @Value注入的单位一致
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * 从JWT令牌中获取角色列表
     *
     * 业务作用：解析JWT令牌并提取用户角色列表，用于Spring Security的权限判断
     * 业务规则：角色信息在生成Access Token时写入claims；返回的原始角色名后续由过滤器添加"ROLE_"前缀
     * 异常情况：令牌无效时抛出JWT异常；roles字段不存在时返回null
     * 注意事项：此方法使用@SuppressWarnings("unchecked")进行类型强制转换，需确保令牌中roles字段类型正确
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return parseToken(token).get("roles", List.class);
    }
}
