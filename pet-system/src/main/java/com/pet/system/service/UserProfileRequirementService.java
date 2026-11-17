package com.pet.system.service;

import com.pet.common.BusinessException;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import com.pet.system.profile.UserProfileAction;
import com.pet.system.profile.UserProfileConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserProfileRequirementService {

    private final UserMapper userMapper;

    public UserProfileRequirementService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 业务作用：在执行需要实名认证或手机绑定的操作前进行前置条件检查
     * 业务规则：动作是否需要实名/手机绑定由UserProfileAction枚举定义；认证状态必须为REAL_NAME_VERIFIED才算已认证
     * 异常情况：
     * 用户不存在/未登录 → 401 BusinessException；需要实名但未认证 → 403 BusinessException(action.label + "前需要先完成实名认证")；需要手机但未绑定 → 403 BusinessException(action.label + "前需要先绑定手机号")
     * 注意事项：后续新增需要前置条件的操作只需在UserProfileAction枚举中添加并设置相应标志即可
     */
    public void ensureAllowed(Long userId, UserProfileAction action) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或未登录");
        }
        // 动作要求实名时校验审核通过状态
        if (action.isRequireRealName() && !isRealNameVerified(user)) {
            throw new BusinessException(403, action.getLabel() + "前需要先完成实名认证");
        }
        // 手机号空白视为未绑定
        if (action.isRequireBoundPhone() && !StringUtils.hasText(user.getPhone_wsh())) {
            throw new BusinessException(403, action.getLabel() + "前需要先绑定手机号");
        }
    }

    /**
     * 业务作用：判断用户的实名审核状态是否为已通过。
     * 数据处理：状态为 null 视为未实名，否则与 REAL_NAME_VERIFIED 比较。
     * 业务规则：仅审核通过状态算已实名，审核中与驳回均不算。
     * 注意事项：状态值为 Integer，比较使用拆箱，null 已提前排除。
     */
    private boolean isRealNameVerified(User user) {
        return user.getReal_name_status_wsh() != null
                && user.getReal_name_status_wsh() == UserProfileConstants.REAL_NAME_VERIFIED;
    }
}
