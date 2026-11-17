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

    /**
     * 业务作用：根据状态和关键词分页查询用户的实名认证申请记录
     * 业务规则：status为null时不加状态过滤；keyword为空时不加模糊条件；按更新时间倒序
     */
    @Override
    public IPage<RealNameReviewVO> listPage(PageRequestDTO pageParam, Integer status, String keyword) {
        log.info("调用 listPage(), status={}, keyword={}", status, keyword);
        Page<User> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getUpdated_at_wsh);
        if (status != null) {
            qw.eq(User::getReal_name_status_wsh, status);
        }
        // 关键词同时模糊匹配用户名、昵称与真实姓名
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

    /**
     * 业务作用：将用户的实名认证状态从"待审核"变更为"已认证"
     * 业务规则：只有real_name_status=1(待审核)的才能批准；通过后清除之前可能的驳回原因
     * 异常情况：用户不存在 → BusinessException；实名状态非待审核 → BusinessException("仅待审核状态的实名认证可批准")
     * 注意事项：reviewerId和remark当前为预留字段，未写入数据库
     */
    @Transactional
    @Override
    public void approve(Long userId, Long reviewerId, String remark) {
        log.info("调用 approve(), userId={}, reviewerId={}", userId, reviewerId);
        User user = getUserById(userId);
        // 只有待审核状态才能批准
        if (user.getReal_name_status_wsh() == null
                || user.getReal_name_status_wsh() != UserProfileConstants.REAL_NAME_PENDING) {
            throw new BusinessException("仅待审核状态的实名认证可批准");
        }
        user.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_VERIFIED);
        // 通过后清掉历史驳回原因
        user.setReject_reason_wsh(null);
        userMapper.updateById(user);
    }

    /**
     * 业务作用：将用户的实名认证状态从"待审核"变更为"已驳回"，并记录驳回原因
     * 业务规则：只有real_name_status=1(待审核)的才能驳回；驳回原因会存入数据库供用户查看
     * 异常情况：用户不存在 → BusinessException；实名状态非待审核 → BusinessException("仅待审核状态的实名认证可驳回")
     * 注意事项：用户重新提交实名认证申请后状态恢复为待审核，可再次审核
     */
    @Transactional
    @Override
    public void reject(Long userId, Long reviewerId, String remark) {
        log.info("调用 reject(), userId={}, reviewerId={}", userId, reviewerId);
        User user = getUserById(userId);
        // 只有待审核状态才能驳回
        if (user.getReal_name_status_wsh() == null
                || user.getReal_name_status_wsh() != UserProfileConstants.REAL_NAME_PENDING) {
            throw new BusinessException("仅待审核状态的实名认证可驳回");
        }
        user.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_REJECTED);
        user.setReject_reason_wsh(remark);
        userMapper.updateById(user);
    }

    /**
     * 业务作用：查询用户并判读是否存在，不存在则抛异常
     * 异常情况：用户不存在 → BusinessException
     */
    private User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 业务作用：将User实体转换为RealNameReviewVO，用于审核页面展示
     * 注意事项：身份证号(id_card_no)会明文返回，需注意数据脱敏合规问题
     */
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
