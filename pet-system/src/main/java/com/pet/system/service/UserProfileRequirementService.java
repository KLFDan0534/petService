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
