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
     * 【分页查询实名认证审核列表】
     *
     * 业务作用：根据状态和关键词分页查询用户的实名认证申请记录
     *
     * 调用场景：管理后台实名认证审核列表页
     *
     * 调用链：AdminController ↓ listPage() → UserMapper.selectPage(条件: 状态筛选 + 用户名/昵称/真实姓名模糊) → stream.map(toReviewVO) → IPage<RealNameReviewVO>
     *
     * 数据处理：pageParam + status(可为null) + keyword(可为null) → Page分页对象 → LambdaQueryWrapper条件(状态eq + 关键词or like) → selectPage → 遍历转RealNameReviewVO
     *
     * 业务规则：status为null时不加状态过滤；keyword为空时不加模糊条件；按更新时间倒序
     *
     * 状态影响：无
     *
     * 异常情况：无
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
     * 【审核通过实名认证】
     *
     * 业务作用：将用户的实名认证状态从"待审核"变更为"已认证"
     *
     * 调用场景：审核人员在后台审核通过实名认证申请
     *
     * 调用链：AdminController ↓ approve() → getUserById → 校验状态为待审核 → setReal_name_status(已认证) → setReject_reason(null) → UserMapper.updateById
     *
     * 数据处理：userId + reviewerId(预留) + remark(预留) → 查询用户 → 判读real_name_status == REAL_NAME_PENDING → 设为REAL_NAME_VERIFIED → 清除驳回原因 → updateById
     *
     * 业务规则：只有real_name_status=1(待审核)的才能批准；通过后清除之前可能的驳回原因
     *
     * 状态影响：更新user_wsh表: real_name_status_wsh=2(已认证) + reject_reason_wsh=null
     *
     * 异常情况：用户不存在 → BusinessException；实名状态非待审核 → BusinessException("仅待审核状态的实名认证可批准")
     *
     * 注意事项：reviewerId和remark当前为预留字段，未写入数据库
     */
    @Transactional
    @Override
    public void approve(Long userId, Long reviewerId, String remark) {
        log.info("调用 approve(), userId={}, reviewerId={}", userId, reviewerId);
        User user = getUserById(userId);
        if (user.getReal_name_status_wsh() == null
                || user.getReal_name_status_wsh() != UserProfileConstants.REAL_NAME_PENDING) {
            throw new BusinessException("仅待审核状态的实名认证可批准");
        }
        user.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_VERIFIED);
        user.setReject_reason_wsh(null);
        userMapper.updateById(user);
    }

    /**
     * 【驳回实名认证】
     *
     * 业务作用：将用户的实名认证状态从"待审核"变更为"已驳回"，并记录驳回原因
     *
     * 调用场景：审核人员在后台驳回实名认证申请
     *
     * 调用链：AdminController ↓ reject() → getUserById → 校验状态为待审核 → setReal_name_status(已驳回) → setReject_reason(remark) → UserMapper.updateById
     *
     * 数据处理：userId + reviewerId(预留) + remark(驳回原因) → 查询用户 → 判读real_name_status == REAL_NAME_PENDING → 设为REAL_NAME_REJECTED → 设置reject_reason → updateById
     *
     * 业务规则：只有real_name_status=1(待审核)的才能驳回；驳回原因会存入数据库供用户查看
     *
     * 状态影响：更新user_wsh表: real_name_status_wsh=3(已驳回) + reject_reason_wsh=remark
     *
     * 异常情况：用户不存在 → BusinessException；实名状态非待审核 → BusinessException("仅待审核状态的实名认证可驳回")
     *
     * 注意事项：用户重新提交实名认证申请后状态恢复为待审核，可再次审核
     */
    @Transactional
    @Override
    public void reject(Long userId, Long reviewerId, String remark) {
        log.info("调用 reject(), userId={}, reviewerId={}", userId, reviewerId);
        User user = getUserById(userId);
        if (user.getReal_name_status_wsh() == null
                || user.getReal_name_status_wsh() != UserProfileConstants.REAL_NAME_PENDING) {
            throw new BusinessException("仅待审核状态的实名认证可驳回");
        }
        user.setReal_name_status_wsh(UserProfileConstants.REAL_NAME_REJECTED);
        user.setReject_reason_wsh(remark);
        userMapper.updateById(user);
    }

    /**
     * 【根据ID获取用户】
     *
     * 业务作用：查询用户并判读是否存在，不存在则抛异常
     *
     * 调用场景：approve/reject方法中获取被审核用户信息
     *
     * 数据处理：userId → UserMapper.selectById → 判读null → 返回User / 抛异常
     *
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
     * 【用户实体转实名认证审核VO】
     *
     * 业务作用：将User实体转换为RealNameReviewVO，用于审核页面展示
     *
     * 调用场景：listPage方法中逐条转换
     *
     * 数据处理：User → 拷贝字段(用户名/昵称/真实姓名/身份证号/实名状态/手机号/邮箱/创建时间/驳回原因) → RealNameReviewVO
     *
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
