package com.pet.common;

public final class OrderStatus {
    private OrderStatus() {}
    public static final String PENDING = "pending";
    public static final String PAID = "paid";
    public static final String CONFIRMED = "confirmed";
    public static final String DELIVERED = "delivered";
    public static final String RECEIVED = "received";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String CANCELLED = "cancelled";
    public static final String REFUNDING = "refunding";
    public static final String REFUNDED = "refunded";
}
