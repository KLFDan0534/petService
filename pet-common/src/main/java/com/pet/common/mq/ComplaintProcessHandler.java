package com.pet.common.mq;

/**
 * 【投诉处理异步回调接口】
 *
 * 业务作用：
 * 定义投诉处理完成的异步回调契约。投诉审核通过后，
 * MQ Consumer（MessageListener）通过此接口回调业务层执行后续逻辑。
 *
 * 设计原因：
 * 接口定义在 pet-common 中是为了让 pet-framework 的 MessageListener
 * 能够编译期引用，而具体实现由 pet-business 模块提供（Spring @Autowired required=false）。
 *
 * 调用链：
 * ComplaintController.approve() → 审核通过
 *   ↓
 * MessageSender.sendComplaintProcess() → MQ
 *   ↓
 * MessageListener.handleComplaintProcess() → MQ Consumer
 *   ↓
 * ComplaintProcessHandler.handle() → 业务实现
 *
 * 后续影响：
 * handle() 执行后通常触发通知推送（通知投诉方处理结果）。
 *
 * 注意事项：
 * - 此接口由 pet-business 模块实现
 * - pet-framework 通过 @Autowired(required=false) 注入，避免循环依赖
 */
@FunctionalInterface
public interface ComplaintProcessHandler {
    /**
     * 投诉处理完成后执行异步后续操作
     * @param complaintId 投诉记录 ID
     */
    void handle(Long complaintId);
}
