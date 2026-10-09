package com.pet.common.mq;


/**
 * 【公告通知异步回调接口】
 *
 * 业务作用：
 * 定义「公告发布后为全量用户异步生成站内通知」的契约。
 * 公告创建/更新时只把 noticeId 投递到 MQ，由 MQ Consumer（MessageListener）
 * 通过此接口回调业务层，在后台分批完成通知生成与推送，避免阻塞发公告请求。
 *
 * 设计原因：
 * 接口定义在 pet-common 中是为了让 pet-framework 的 MessageListener
 * 能够编译期引用，而具体实现由 pet-business 模块提供（Spring @Autowired required=false）。
 * 参数使用 Long 而非 Notice 实体，同样是为了避免 pet-common 反向依赖 pet-business。
 *
 * 调用链：
 * NoticeServiceImpl.create()/update() → 公告落库
 *   ↓
 * MessageSender.sendNoticeNotification() → MQ 队列 notice.notification
 *   ↓
 * MessageListener.handleNoticeNotification() → MQ Consumer
 *   ↓
 * NoticeNotificationHandler.handle() → 业务实现（分批生成通知 + SSE 推送）
 *
 * 后续影响：
 * handle() 执行后为该公告批量创建 notification_wsh 记录，并向在线用户推送 SSE。
 *
 * 注意事项：
 * - 此接口由 pet-business 模块实现
 * - pet-framework 通过 @Autowired(required=false) 注入，避免循环依赖
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
