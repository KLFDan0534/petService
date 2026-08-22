package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 【业务模块】投诉可选目标列表
 * 业务作用：返回当前用户可投诉/举报的对象（订单、商家、寄养师），
 * 前端渲染成选择列表，用户无需手填任何 ID。
 */
@Data
public class ComplaintTargetsDTO {

    @Schema(description = "可投诉的订单列表")
    private List<OrderTarget> orders_wsh;

    @Schema(description = "可投诉的商家列表（我消费过的商家）")
    private List<MerchantTarget> merchants_wsh;

    @Schema(description = "可投诉的寄养师列表（服务过我的寄养师）")
    private List<KeeperTarget> keepers_wsh;

    @Data
    public static class OrderTarget {
        @Schema(description = "订单ID")
        private Long id_wsh;
        @Schema(description = "订单号")
        private String order_no_wsh;
        @Schema(description = "订单状态")
        private String status_wsh;
        @Schema(description = "所属商家ID")
        private Long merchant_id_wsh;
        @Schema(description = "所属商家名称")
        private String merchant_name_wsh;
        @Schema(description = "寄养师ID")
        private Long keeper_id_wsh;
        @Schema(description = "寄养师名称")
        private String keeper_name_wsh;
        @Schema(description = "创建时间")
        private LocalDateTime created_at_wsh;
    }

    @Data
    public static class MerchantTarget {
        @Schema(description = "商家ID")
        private Long id_wsh;
        @Schema(description = "商家名称")
        private String name_wsh;
        @Schema(description = "我的下单次数")
        private Long order_count_wsh;
        @Schema(description = "最近下单时间")
        private LocalDateTime last_order_at_wsh;
    }

    @Data
    public static class KeeperTarget {
        @Schema(description = "寄养师ID")
        private Long id_wsh;
        @Schema(description = "寄养师名称")
        private String name_wsh;
        @Schema(description = "所属商家ID")
        private Long merchant_id_wsh;
        @Schema(description = "所属商家名称")
        private String merchant_name_wsh;
    }
}
