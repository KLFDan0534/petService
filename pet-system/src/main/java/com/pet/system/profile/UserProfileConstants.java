package com.pet.system.profile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户资料常量")
public final class UserProfileConstants {
    public static final int REAL_NAME_UNVERIFIED = 0;
    public static final int REAL_NAME_PENDING = 1;
    public static final int REAL_NAME_VERIFIED = 2;
    public static final int REAL_NAME_REJECTED = 3;

    private UserProfileConstants() {
    }
}
