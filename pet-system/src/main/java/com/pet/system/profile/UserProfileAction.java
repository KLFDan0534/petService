package com.pet.system.profile;

/**
 * 【用户资料前置条件动作枚举】
 *
 * 业务作用：
 * 定义需要特定用户资料（实名认证/手机绑定）前置条件的业务动作。
 * 由 UserProfileRequirementService 在校验时使用，确保用户在
 * 执行敏感操作前已完成必要的资料完善。
 *
 * 枚举项说明：
 * - ADD_PET("上传宠物")：需要实名认证，不需要手机绑定
 * - CREATE_ORDER("下单")：需要实名认证和手机绑定
 * - APPLY_MERCHANT("成为商家")：需要实名认证和手机绑定
 * - APPLY_KEEPER("成为keeper")：需要实名认证和手机绑定
 *
 * 调用链：
 * Controller/Service 执行业务前
 *   ↓
 * UserProfileRequirementService.ensureAllowed(userId, action)
 *   ↓
 * 校验不通过 → BusinessException(403, "xxx前需要先完成实名认证")
 *
 * 扩展方式：
 * 新增需要前置条件的业务动作时，在此枚举添加新常量并设置
 * requireRealName 和 requireBoundPhone 标志即可。
 */
public enum UserProfileAction {
    ADD_PET("上传宠物", true, false),
    CREATE_ORDER("下单", true, true),
    APPLY_MERCHANT("成为商家", true, true),
    APPLY_KEEPER("成为keeper", true, true);

    private final String label;
    private final boolean requireRealName;
    private final boolean requireBoundPhone;

    UserProfileAction(String label, boolean requireRealName, boolean requireBoundPhone) {
        this.label = label;
        this.requireRealName = requireRealName;
        this.requireBoundPhone = requireBoundPhone;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRequireRealName() {
        return requireRealName;
    }

    public boolean isRequireBoundPhone() {
        return requireBoundPhone;
    }
}
