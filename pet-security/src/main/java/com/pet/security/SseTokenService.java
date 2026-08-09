package com.pet.security;

import com.pet.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SSE令牌校验服务
 *
 * 业务作用：为SSE（Server-Sent Events）连接提供令牌验证能力，校验SSE事件流连接中的JWT令牌有效性及黑名单状态，
 *           确保SSE长连接的安全性
 *
 * 调用场景：SSE事件流（订单事件、聊天事件、通知事件）建立连接时进行令牌鉴权
 *
 * 调用链：SSE Controller ↓ SseTokenService.requireUserId ↓ JwtUtil ↓ Redis
 *
 * 数据处理：请求中的token参数 → 空值校验 → 黑名单校验 → JWT验签 → 提取用户ID
 *
 * 业务规则：token为空、在黑名单中、JWT无效时均抛BusinessException(401)；校验通过返回用户ID
 *
 * 状态影响：无状态，仅做校验不修改数据
 *
 * 异常情况：校验失败抛BusinessException(401, "Invalid token")
 *
 * 注意事项：SSE长连接的令牌校验与普通API请求的过滤器校验是两套独立机制，两者均需维护
 */
@Component
public class SseTokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public SseTokenService(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 校验SSE令牌并获取用户ID
     *
     * 业务作用：校验SSE事件流连接中的JWT令牌，包含空值、黑名单和有效性三重校验，通过后返回用户ID
     *
     * 调用场景：SSE控制器建立事件流连接时调用
     *
     * 调用链：SSE Controller ↓ requireUserId ↓ JwtUtil.validateToken / isBlacklisted ↓ jwtUtil.getUserIdFromToken
     *
     * 数据处理：输入token字符串 → 空值判断 → Redis黑名单查询 → JWT验签 → 返回Long类型用户ID
     *
     * 业务规则：三重校验（空值/黑名单/JWT有效）全部通过才返回用户ID；任一校验失败抛BusinessException
     *
     * 状态影响：无状态
     *
     * 异常情况：BusinessException(401, "Invalid token") — token为空、在黑名单中或JWT无效时触发
     *
     * 注意事项：此方法抛出BusinessException会被全局异常处理器捕获，返回401 JSON响应
     */
    public Long requireUserId(String token) {
        if (!StringUtils.hasText(token) || isBlacklisted(token) || !jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "Invalid token");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

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
