package com.pet.security;

import com.pet.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SseTokenServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private SseTokenService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new SseTokenService(jwtUtil, redisTemplate);
    }

    @Test
    void requireUserIdRejectsBlacklistedQueryToken() {
        when(valueOperations.get("blacklist:token:token-old")).thenReturn("1");

        assertThrows(BusinessException.class, () -> service.requireUserId("token-old"));
    }

    @Test
    void requireUserIdReturnsUserIdForValidNonBlacklistedToken() {
        when(valueOperations.get("blacklist:token:token-ok")).thenReturn(null);
        when(jwtUtil.validateToken("token-ok")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token-ok")).thenReturn(12L);

        assertEquals(12L, service.requireUserId("token-ok"));
        verify(jwtUtil).validateToken("token-ok");
    }
}
