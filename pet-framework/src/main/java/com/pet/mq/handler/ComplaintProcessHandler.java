package com.pet.mq.handler;

/**
 * 设计原因：
 * 接口定义在 pet-framework 的 com.pet.mq 包中，与 MessageListener、MessageSender 同包。
 * 消息监听器编译期直接引用本接口，具体实现由 pet-business 模块提供，由 Spring 在运行时注入。
 * pet-business 已经依赖 pet-framework，因此不会形成循环依赖。
 *
 * 业务作用：定义投诉处理完成的异步回调契约。投诉审核通过后，
 * MQ Consumer（MessageListener）通过此接口回调业务层执行后续逻辑。
 *
 * 注意事项：
 * - 此接口由 pet-business 模块实现
 * - MessageListener 通过 @Resource 注入，框架层不依赖具体业务实现
 */
@FunctionalInterface
public interface ComplaintProcessHandler {
    /**
     * 投诉处理完成后执行异步后续操作
     * @param complaintId 投诉记录 ID
     */
    void handle(Long complaintId);
}
