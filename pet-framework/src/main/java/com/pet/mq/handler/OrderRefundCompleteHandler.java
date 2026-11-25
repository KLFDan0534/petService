package com.pet.mq.handler;

@FunctionalInterface
public interface OrderRefundCompleteHandler {

    void handle(String refundId);

}
