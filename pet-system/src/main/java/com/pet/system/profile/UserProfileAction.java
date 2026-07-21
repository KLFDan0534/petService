package com.pet.system.profile;

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
