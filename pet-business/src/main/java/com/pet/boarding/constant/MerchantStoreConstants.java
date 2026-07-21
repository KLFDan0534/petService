package com.pet.boarding.constant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "商户门店常量")
public final class MerchantStoreConstants {

    private MerchantStoreConstants() {}

    public static final int MODE_AUTO = 0;
    public static final int MODE_MANUAL_OPEN = 1;
    public static final int MODE_MANUAL_CLOSED = 2;

    public static final int STATUS_CLOSED = 0;
    public static final int STATUS_OPEN = 1;
}
