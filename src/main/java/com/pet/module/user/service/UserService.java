package com.pet.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.module.user.dto.LoginRequest;
import com.pet.module.user.dto.LoginResponse;
import com.pet.module.user.dto.RegisterRequest;
import com.pet.module.user.dto.UserVO;
import com.pet.module.user.entity.Role;
import com.pet.module.user.entity.User;
import com.pet.module.user.entity.UserRole;
import com.pet.module.user.mapper.RoleMapper;
import com.pet.module.user.mapper.UserMapper;
import com.pet.module.user.mapper.UserRoleMapper;
import com.pet.security.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public UserService(UserMapper userMapper, RoleMapper roleMapper,
                       UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        userMapper.insert(user);

        Role ownerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode, "OWNER"));
        if (ownerRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(ownerRole.getId());
            userRoleMapper.insert(userRole);
        }

        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        return buildLoginResponse(user);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildLoginResponse(user);
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.opsForValue().set("blacklist:token:" + token, "1", 1, TimeUnit.DAYS);
        }
    }

    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVO(user);
    }

    @Transactional
    public UserVO updateUser(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(vo.getNickname())) user.setNickname(vo.getNickname());
        if (StringUtils.hasText(vo.getPhone())) user.setPhone(vo.getPhone());
        if (StringUtils.hasText(vo.getAvatar())) user.setAvatar(vo.getAvatar());
        if (StringUtils.hasText(vo.getEmail())) user.setEmail(vo.getEmail());
        if (StringUtils.hasText(vo.getAddress())) user.setAddress(vo.getAddress());
        if (vo.getLatitude() != null) user.setLatitude(vo.getLatitude());
        if (vo.getLongitude() != null) user.setLongitude(vo.getLongitude());
        userMapper.updateById(user);
        return toUserVO(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoles(roles);
        return response;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoles(roleMapper.selectRoleCodesByUserId(user.getId()));
        return vo;
    }

    public List<User> listAll() {
        return userMapper.selectList(null);
    }
}
