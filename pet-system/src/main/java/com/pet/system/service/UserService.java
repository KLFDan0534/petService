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
     * 用户注册
     * @param request 注册请求对象
     * @return 登录响应（含token）
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    LoginResponseVO register(RegisterRequestDTO request);

    RegisterCaptchaVO requestRegisterCaptcha(String phone);

    /**
     * 用户登录
     * @param request 登录请求对象
     * @return 登录响应（含token）
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    LoginResponseVO login(LoginRequestDTO request);

    /**
     * 刷新用户token
     * @param refreshToken 刷新令牌
     * @return 新的登录响应
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    LoginResponseVO refreshToken(String refreshToken);

    /**
     * 用户登出
     * @param token 用户令牌
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void logout(String token);

    void deleteCurrentUser(Long userId, String token);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户视图对象
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    UserVO getUserInfo(Long userId);

    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param vo 用户视图对象
     * @return 更新后的用户信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    UserVO updateUser(Long userId, UserVO vo);

    void setPaymentPassword(Long userId, String paymentPassword);

    boolean hasPaymentPassword(Long userId);

    void verifyPaymentPassword(Long userId, String paymentPassword);

    /**
     * 获取所有用户列表
     * @return 用户视图对象列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<UserVO> listAll();

    /**
     * 分页查询用户列表
     * @param pageParam 分页参数
     * @param keyword 关键词
     * @return 分页用户数据
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    IPage<UserVO> listPage(PageRequestDTO pageParam, String keyword);

    /**
     * 获取用户角色列表
     * @param userId 用户ID
     * @return 角色名称列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<String> getUserRoles(Long userId);

    /**
     * 忘记密码（发送重置邮件）
     * @param email 用户邮箱
     * @return 处理结果信息
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    String forgotPassword(String email);
}
