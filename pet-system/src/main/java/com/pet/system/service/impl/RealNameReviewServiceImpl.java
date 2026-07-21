package com.pet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import com.pet.system.profile.UserProfileConstants;
import com.pet.system.service.RealNameReviewService;
import com.pet.system.vo.RealNameReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Slf4j
@Service
public class RealNameReviewServiceImpl implements RealNameReviewService {

    private final UserMapper userMapper;

    public RealNameReviewServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public IPage<RealNameReviewVO> listPage(PageRequestDTO pageParam, Integer status, String keyword) {
        log.info("call listPage(), status={}, keyword={}", status, keyword);
        Page<User> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getUpdated_at_wsh);
        if (status != null) {
            qw.eq(User::getReal_name_status_wsh, status);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername_wsh, keyword)
                    .or().like(User::getNickname_wsh, keyword)
                    .or().like(User::getReal_name_wsh, keyword));
        }
        Page<User> userPage = userMapper.selectPage(page, qw);
        Page<RealNameReviewVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream()
                .map(this::toReviewVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Transactional
    @Override
    public void approve(Long userId, Long reviewerId, String remark) {
        log.info("call approve(), userId={}, reviewerId={}", userId, reviewerId);
        User user = getUserById(userId);
        if (user.getReal_name_status_wsh() == null
                || user.getReal_name_status_wsh() != UserProfileConstants.REAL_NAME_PENDING) {
            throw new BusinessException("仅待审核状态的实名认证可批准");
        }
        user.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_VERIFIED);
        user.setReject_reason_wsh(null);
        userMapper.updateById(user);
    }

    @Transactional
    @Override
    public void reject(Long userId, Long reviewerId, String remark) {
        log.info("call reject(), userId={}, reviewerId={}", userId, reviewerId);
        User user = getUserById(userId);
        if (user.getReal_name_status_wsh() == null
                || user.getReal_name_status_wsh() != UserProfileConstants.REAL_NAME_PENDING) {
            throw new BusinessException("仅待审核状态的实名认证可驳回");
        }
        user.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_REJECTED);
        user.setReject_reason_wsh(remark);
        userMapper.updateById(user);
    }

    private User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private RealNameReviewVO toReviewVO(User user) {
        RealNameReviewVO vo = new RealNameReviewVO();
        vo.setId_wsh(user.getId_wsh());
        vo.setUsername_wsh(user.getUsername_wsh());
        vo.setNickname_wsh(user.getNickname_wsh());
        vo.setReal_name_wsh(user.getReal_name_wsh());
        vo.setId_card_no_wsh(user.getId_card_no_wsh());
        vo.setReal_name_status_wsh(user.getReal_name_status_wsh());
        vo.setPhone_wsh(user.getPhone_wsh());
        vo.setEmail_wsh(user.getEmail_wsh());
        vo.setCreated_at_wsh(user.getCreated_at_wsh());
        vo.setReject_reason_wsh(user.getReject_reason_wsh());
        return vo;
    }
}
