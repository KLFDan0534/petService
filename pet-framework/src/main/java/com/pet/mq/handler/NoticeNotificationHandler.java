package com.pet.mq.handler;

/**
 * 设计原因：
 * 接口定义在 pet-framework 的 com.pet.mq 包中，与 MessageListener、MessageSender 同包。
 * 消息监听器编译期直接引用本接口，具体实现由 pet-business 模块提供，由 Spring 在运行时注入。
 * 参数使用 Long 而非 Notice 实体，是为了避免 framework 反向依赖 business。
 *
 * 业务作用：定义「公告发布后为全量用户异步生成站内通知」的契约。
 * 公告创建/更新时只把 noticeId 投递到 MQ，由 MQ Consumer（MessageListener）
 * 通过此接口回调业务层，在后台分批完成通知生成与推送，避免阻塞发公告请求。
 *
 * 注意事项：
 * - 此接口由 pet-business 模块实现
 * - MQ 只保证至少一次投递，handle() 必须幂等
 */
@FunctionalInterface
public interface NoticeNotificationHandler {

    /**
     * 公告发布/更新后，为该公告异步生成全量用户通知
     * @param noticeId 公告ID（notice_wsh.id_wsh），非通知记录ID
     */
    void handle(Long noticeId);

}
