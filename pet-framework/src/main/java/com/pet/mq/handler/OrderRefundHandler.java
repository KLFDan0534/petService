package com.pet.mq.handler;

@FunctionalInterface
public interface OrderRefundHandler {

    void handle(String refundId);

}
