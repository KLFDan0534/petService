package com.pet.admin;

import com.pet.security.JwtUtil;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserMapper;
import com.pet.system.mapper.UserRoleMapper;
import com.pet.system.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPasswordResetServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;

    @Test
    void resetAllPasswordsEncryptsPasswordAndUpdatesEveryUser() {
        PasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        UserServiceImpl service = new UserServiceImpl(
                userMapper, roleMapper, userRoleMapper, passwordEncoder, jwtUtil, redisTemplate);
        when(userMapper.resetAllPasswords(org.mockito.ArgumentMatchers.anyString())).thenReturn(45);

        int updated = service.resetAllPasswords("123456");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userMapper).resetAllPasswords(hashCaptor.capture());
        assertEquals(45, updated);
        assertTrue(passwordEncoder.matches("123456", hashCaptor.getValue()));
    }
}
