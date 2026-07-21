package com.pet.common.mq;

/**
 * 投诉处理完成后的异步处理器接口。
 * 定义在 pet-common 中，以便 pet-framework 的 MessageListener 可以引用，
 * 而具体实现在 pet-business 中完成。
 */
@FunctionalInterface
public interface ComplaintProcessHandler {
    void handle(Long complaintId);
}
