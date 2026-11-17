package com.pet.security;

import com.pet.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SSE令牌校验服务
 *
 * 业务作用：为SSE（Server-Sent Events）连接提供令牌验证能力，校验SSE事件流连接中的JWT令牌有效性及黑名单状态， 确保SSE长连接的安全性
 * 业务规则：token为空、在黑名单中、JWT无效时均抛BusinessException(401)；校验通过返回用户ID
 * 异常情况：校验失败抛BusinessException(401, "Invalid token")
 * 注意事项：SSE长连接的令牌校验与普通API请求的过滤器校验是两套独立机制，两者均需维护
 */
@Component
public class SseTokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * 业务作用：注入 JWT 工具与 Redis 模板，供 SSE 连接鉴权使用。
     * 注意事项：redisTemplate 允许为 null，isBlacklisted 中已做空判断。
     */
    public SseTokenService(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 校验SSE令牌并获取用户ID
     *
     * 业务作用：校验SSE事件流连接中的JWT令牌，包含空值、黑名单和有效性三重校验，通过后返回用户ID
     * 业务规则：三重校验（空值/黑名单/JWT有效）全部通过才返回用户ID；任一校验失败抛BusinessException
     * 异常情况：BusinessException(401, "Invalid token") — token为空、在黑名单中或JWT无效时触发
     * 注意事项：此方法抛出BusinessException会被全局异常处理器捕获，返回401 JSON响应
     */
    public Long requireUserId(String token) {
        if (!StringUtils.hasText(token) || isBlacklisted(token) || !jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "Invalid token");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 业务作用：查询 Redis 中是否存在该令牌的黑名单键，用于登出后立即失效。
     * 业务规则：Redis 不可用或查询异常时按未拉黑处理，保证可用性优先。
     * 异常情况：查询异常被吞掉并返回 false。
     * 注意事项：降级放行意味着 Redis 故障期间已登出的令牌仍可用。
     */
    private boolean isBlacklisted(String token) {
        try {
            return redisTemplate != null
                    && redisTemplate.opsForValue() != null
                    && redisTemplate.opsForValue().get("blacklist:token:" + token) != null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
