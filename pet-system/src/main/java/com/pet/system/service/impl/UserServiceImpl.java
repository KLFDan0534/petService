package com.pet.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageParam;
import com.pet.common.StatusCode;
import com.pet.system.dto.LoginRequest;
import com.pet.system.dto.LoginResponse;
import com.pet.system.dto.RegisterRequest;
import com.pet.system.vo.UserVO;
import com.pet.system.entity.Role;
import com.pet.system.entity.User;
import com.pet.system.entity.UserRole;
import com.pet.system.mapper.RoleMapper;
import com.pet.system.mapper.UserMapper;
import com.pet.system.mapper.UserRoleMapper;
import com.pet.security.JwtUtil;
import com.pet.system.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
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
        LoginAttempt(int count, long lockUntil) { this.count = count; this.lockUntil = lockUntil; }
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 登录响应
     */
    @Transactional
    @Override
    public LoginResponse register(RegisterRequest request) {
        log.info("调用 register()");
        User user = new User();
        BeanUtil.copyProperties(request, user);
        user.setPassword_wsh(passwordEncoder.encode(request.getPassword_wsh()));
        user.setNickname_wsh(StringUtils.hasText(request.getNickname_wsh()) ? HtmlUtils.htmlEscape(request.getNickname_wsh()) : request.getUsername_wsh());
        user.setStatus_wsh(StatusCode.USER_ACTIVE.getValue());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("用户名已存在");
        }

        Role ownerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getCode_wsh, "OWNER"));
        if (ownerRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUser_id_wsh(user.getId_wsh());
            userRole.setRole_id_wsh(ownerRole.getId_wsh());
            userRoleMapper.insert(userRole);
        }

        return buildLoginResponse(user);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    private static final int MAX_LOGIN_ATTEMPTS = 20;
    private static final long LOCK_DURATION_MINUTES = 15;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("调用 login()");
        checkLoginAttempt(request.getUsername_wsh());
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername_wsh, request.getUsername_wsh()));
        if (user == null) {
            recordLoginAttempt(request.getUsername_wsh());
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus_wsh() == StatusCode.USER_BANNED.getValue()) {
            throw new BusinessException(403, "账户已被封禁");
        }
        if (!passwordEncoder.matches(request.getPassword_wsh(), user.getPassword_wsh())) {
            recordLoginAttempt(request.getUsername_wsh());
            throw new BusinessException(401, "用户名或密码错误");
        }
        clearLoginAttempt(request.getUsername_wsh());
        return buildLoginResponse(user);
    }

    private void checkLoginAttempt(String username) {
        if (redisTemplate != null) {
            var ops = redisTemplate.opsForValue();
            if (ops != null) {
                String failCount = ops.get("login_fail:" + username);
                if (failCount != null && Integer.parseInt(failCount) >= MAX_LOGIN_ATTEMPTS) {
                    throw new BusinessException("账户已被锁定，请" + LOCK_DURATION_MINUTES + "分钟后再试");
                }
            }
        } else {
            LoginAttempt attempt = localLoginAttempts.get(username);
            if (attempt != null && System.currentTimeMillis() < attempt.lockUntil) {
                throw new BusinessException("账户已被锁定，请" + LOCK_DURATION_MINUTES + "分钟后再试");
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

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 登录响应
     */
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        log.info("调用 refreshToken()");
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

    /**
     * 登出
     *
     * @param token 令牌
     */
    @Override
    public void logout(String token) {
        log.info("调用 logout()");
        if (StringUtils.hasText(token)) {
            redisTemplate.opsForValue().set("blacklist:token:" + token, "1", 1, TimeUnit.DAYS);
        }
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public UserVO getUserInfo(Long userId) {
        log.info("调用 getUserInfo()");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVO(user);
    }

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param vo     用户信息
     * @return 用户信息
     */
    @Transactional
    @Override
    public UserVO updateUser(Long userId, UserVO vo) {
        log.info("调用 updateUser()");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(vo.getNickname_wsh())) user.setNickname_wsh(vo.getNickname_wsh());
        if (StringUtils.hasText(vo.getPhone_wsh())) user.setPhone_wsh(vo.getPhone_wsh());
        if (StringUtils.hasText(vo.getAvatar_wsh())) user.setAvatar_wsh(vo.getAvatar_wsh());
        if (StringUtils.hasText(vo.getEmail_wsh())) user.setEmail_wsh(vo.getEmail_wsh());
        if (StringUtils.hasText(vo.getAddress_wsh())) user.setAddress_wsh(vo.getAddress_wsh());
        if (vo.getLatitude_wsh() != null) user.setLatitude_wsh(vo.getLatitude_wsh());
        if (vo.getLongitude_wsh() != null) user.setLongitude_wsh(vo.getLongitude_wsh());
        if (vo.getStatus_wsh() != null) user.setStatus_wsh(vo.getStatus_wsh());
        userMapper.updateById(user);
        return toUserVO(user);
    }

    /**
     * 构建登录响应
     *
     * @param user 用户
     * @return 登录响应
     */
    private LoginResponse buildLoginResponse(User user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId_wsh());
        String accessToken = jwtUtil.generateAccessToken(user.getId_wsh(), user.getUsername_wsh(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId_wsh());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId_wsh());
        response.setUsername(user.getUsername_wsh());
        response.setNickname(user.getNickname_wsh());
        response.setRoles(roles);
        return response;
    }

    /**
     * 转换用户为VO
     *
     * @param user 用户
     * @return 用户VO
     */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId_wsh(user.getId_wsh());
        vo.setUsername_wsh(user.getUsername_wsh());
        vo.setNickname_wsh(user.getNickname_wsh());
        vo.setPhone_wsh(user.getPhone_wsh());
        vo.setAvatar_wsh(user.getAvatar_wsh());
        vo.setEmail_wsh(user.getEmail_wsh());
        vo.setAddress_wsh(user.getAddress_wsh());
        vo.setLatitude_wsh(user.getLatitude_wsh());
        vo.setLongitude_wsh(user.getLongitude_wsh());
        vo.setStatus_wsh(user.getStatus_wsh());
        vo.setCreated_at_wsh(user.getCreated_at_wsh());
        vo.setRoles_wsh(roleMapper.selectRoleCodesByUserId(user.getId_wsh()));
        return vo;
    }

    /**
     * 获取扢�1�7有用�1�7?     *
     * @return 用户VO列表
     */
    @Override
    public List<UserVO> listAll() {
        log.info("调用 listAll()");
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getCreated_at_wsh)
                        .last("LIMIT 1000"));
        return users.stream()
                .map(u -> u.toVO(roleMapper.selectRoleCodesByUserId(u.getId_wsh())))
                .collect(Collectors.toList());
    }

    /**
     * 分页获取用户
     *
     * @param pageParam 分页参数
     * @return 用户VO分页
     */
    @Override
    public IPage<UserVO> listPage(PageParam pageParam, String keyword) {
        log.info("调用 listPage(), keyword={}", keyword);
        Page<User> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>().orderByDesc(User::getCreated_at_wsh);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername_wsh, keyword)
                .or().like(User::getNickname_wsh, keyword));
        }
        Page<User> userPage = userMapper.selectPage(page, qw);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
                .map(u -> u.toVO(roleMapper.selectRoleCodesByUserId(u.getId_wsh())))
                .collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 获取用户角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<String> getUserRoles(Long userId) {
        log.info("调用 getUserRoles()");
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public String forgotPassword(String email) {
        log.info("调用 forgotPassword()");
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail_wsh, email));
        if (user == null) {
            throw new BusinessException("邮箱未注册");
        }
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String tempPassword = sb.toString();
        user.setPassword_wsh(passwordEncoder.encode(tempPassword));
        userMapper.updateById(user);
        return tempPassword;
    }

}
