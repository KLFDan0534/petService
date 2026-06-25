package com.pet.common;

public final class AdoptionStatus {
    private AdoptionStatus() {}

    public static final String PENDING = "pending";
    public static final String MERCHANT_REVIEWING = "merchant_reviewing";
    public static final String ADMIN_REVIEWING = "admin_reviewing";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";

    public static final String MERCHANT_STATUS_PENDING = "pending";
    public static final String MERCHANT_STATUS_APPROVED = "approved";
    public static final String MERCHANT_STATUS_REJECTED = "rejected";

    public static final String ADMIN_STATUS_PENDING = "pending";
    public static final String ADMIN_STATUS_APPROVED = "approved";
    public static final String ADMIN_STATUS_REJECTED = "rejected";

    public static final String PET_AVAILABLE = "available";
    public static final String PET_ADOPTED = "adopted";
}
