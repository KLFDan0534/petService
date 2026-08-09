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
     * 【确保用户具备操作所需资料】
     *
     * 业务作用：在执行需要实名认证或手机绑定的操作前进行前置条件检查
     *
     * 调用场景：发起发帖、接单等需要实名/手机绑定的业务操作前
     *
     * 调用链：XxxController/bizService ↓ ensureAllowed() → UserMapper.selectById → 判读action是否需要实名认证(且用户是否已完成) → 判读是否需要手机绑定(且用户是否已绑定)
     *
     * 数据处理：userId + action(UserProfileAction枚举) → 查询用户 → 若action.requireRealName且用户未实名 → 403 → 若action.requireBoundPhone且用户无手机号 → 403
     *
     * 业务规则：动作是否需要实名/手机绑定由UserProfileAction枚举定义；认证状态必须为REAL_NAME_VERIFIED才算已认证
     *
     * 状态影响：无
     *
     * 异常情况：用户不存在/未登录 → 401 BusinessException；需要实名但未认证 → 403 BusinessException(action.label + "前需要先完成实名认证")；需要手机但未绑定 → 403 BusinessException(action.label + "前需要先绑定手机号")
     *
     * 注意事项：后续新增需要前置条件的操作只需在UserProfileAction枚举中添加并设置相应标志即可
     */
    public void ensureAllowed(Long userId, UserProfileAction action) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在或未登录");
        }
        if (action.isRequireRealName() && !isRealNameVerified(user)) {
            throw new BusinessException(403, action.getLabel() + "前需要先完成实名认证");
        }
        if (action.isRequireBoundPhone() && !StringUtils.hasText(user.getPhone_wsh())) {
            throw new BusinessException(403, action.getLabel() + "前需要先绑定手机号");
        }
    }

    private boolean isRealNameVerified(User user) {
        return user.getReal_name_status_wsh() != null
                && user.getReal_name_status_wsh() == UserProfileConstants.REAL_NAME_VERIFIED;
    }
}
