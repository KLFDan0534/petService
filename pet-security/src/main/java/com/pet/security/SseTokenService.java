package com.pet.security;

import com.pet.common.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SseTokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public SseTokenService(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

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
