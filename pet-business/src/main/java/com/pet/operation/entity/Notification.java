package com.pet.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户通知实体，每个通知归属于单个用户。
 * <p>
 * 通知可由系统公告推送、订单状态变更等事件触发创建，
 * 创建后通过 SSE {@link NotificationBroadcaster} 实时推送给用户。
 */
@Getter
@Setter
@TableName("notification_wsh")
@Schema(description = "通知实体")
public class Notification {
    @TableId(type = IdType.AUTO, value = "id_wsh")
    @Schema(description = "ID")
    private Long id_wsh;

    @TableField(value = "user_id_wsh")
    @Schema(description = "用户ID")
    private Long user_id_wsh;

    @TableField(value = "title_wsh")
    @Schema(description = "标题")
    private String title_wsh;

    @TableField(value = "content_wsh")
    @Schema(description = "内容")
    private String content_wsh;

    @TableField(value = "type_wsh")
    @Schema(description = "类型")
    private String type_wsh;

    @TableField(value = "is_read_wsh")
    @Schema(description = "是否已读")
    private Integer is_read_wsh;

    /**
     * 关联业务ID（多态关联，具体含义由 type_wsh 决定，没有独立的 related_type 字段）：
     * <ul>
     *   <li>type_wsh = "order_feedback"：订单ID（OrderFeedbackNotifier）</li>
     *   <li>type_wsh = "order_fulfillment"：订单ID（OrderFulfillmentServiceImpl）</li>
     *   <li>type_wsh = "notice"：公告ID（NoticeServiceImpl），公告下架/删除后
     *       服务端按该字段级联下线或清理对应通知（见 NotificationServiceImpl）</li>
     *   <li>type_wsh = "complaint"：投诉ID（ComplaintServiceImpl 等）</li>
     *   <li>type_wsh = "ticket"：工单ID（TicketServiceImpl）</li>
     * </ul>
     * 解析时必须先看 type_wsh 再决定该 ID 指向哪张表；
     * 系统/会员类等无关联业务的通知该字段为 NULL。
     * 前端/移动端点击通知时按 type_wsh + related_id_wsh 跳转对应详情页。
     */
    @TableField(value = "related_id_wsh")
    @Schema(description = "关联业务ID（多态关联，含义由 type_wsh 决定：order_feedback/order_fulfillment=订单ID，notice=公告ID，complaint=投诉ID，ticket=工单ID；无关联业务时为空）")
    private Long related_id_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
