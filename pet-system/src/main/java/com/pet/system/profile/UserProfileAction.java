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
    /** 【上传宠物】需要实名认证，不需要绑定手机 */
    ADD_PET("上传宠物", true, false),
    /** 【下单】需要实名认证，并且需要绑定手机 */
    CREATE_ORDER("下单", true, true),
    /** 【成为商家】需要实名认证，并且需要绑定手机 */
    APPLY_MERCHANT("成为商家", true, true),
    /** 【成为看护者】需要实名认证，并且需要绑定手机 */
    APPLY_KEEPER("成为keeper", true, true);

    private final String label;
    private final boolean requireRealName;
    private final boolean requireBoundPhone;

    /**
     * 【资料前置动作枚举构造器】
     *
     * 业务作用：为每个枚举常量绑定展示名与两项前置条件标志。
     *
     * 调用场景：枚举常量声明时由编译器调用。
     *
     * 调用链：枚举初始化 → 构造器 → 赋值 label / requireRealName / requireBoundPhone。
     *
     * 数据处理：三个参数直接写入不可变字段。
     *
     * 业务规则：新增动作时必须显式声明是否需要实名与绑定手机。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：常量顺序变更会影响枚举序号，历史数据若按序号存储需谨慎。
     */
    UserProfileAction(String label, boolean requireRealName, boolean requireBoundPhone) {
        this.label = label;
        this.requireRealName = requireRealName;
        this.requireBoundPhone = requireBoundPhone;
    }

    /**
     * 【获取动作展示名】
     *
     * 业务作用：返回该动作面向用户的中文名称。
     *
     * 调用场景：校验失败时拼接提示语，如「下单前需要先完成实名认证」。
     *
     * 调用链：UserProfileRequirementService.ensureAllowed → getLabel。
     *
     * 数据处理：直接返回 label 字段。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：文案直接暴露给用户，修改需同步前端提示。
     */
    public String getLabel() {
        return label;
    }

    /**
     * 【是否需要实名认证】
     *
     * 业务作用：标识执行该动作前是否要求已完成实名认证。
     *
     * 调用场景：UserProfileRequirementService 校验实名状态前判断。
     *
     * 调用链：ensureAllowed → isRequireRealName → 实名状态校验。
     *
     * 数据处理：直接返回 requireRealName 标志。
     *
     * 业务规则：仅 ADD_PET 为 false，其余三个动作为 true。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public boolean isRequireRealName() {
        return requireRealName;
    }

    /**
     * 【是否需要绑定手机】
     *
     * 业务作用：标识执行该动作前是否要求已绑定手机号。
     *
     * 调用场景：UserProfileRequirementService 校验手机绑定状态前判断。
     *
     * 调用链：ensureAllowed → isRequireBoundPhone → 手机绑定校验。
     *
     * 数据处理：直接返回 requireBoundPhone 标志。
     *
     * 业务规则：仅 ADD_PET 为 false，其余三个动作为 true。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public boolean isRequireBoundPhone() {
        return requireBoundPhone;
    }
}
