package com.pet.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批量下单单项（一只宠物的一个订单）。共享字段（商家/看护人/服务/地址/紧急联系人等）
 * 由 {@link OrderBatchCreateRequestDTO} 承载，本类只包含宠物维度的下单参数。
 */
@Getter
@Setter
public class OrderBatchItemDTO {

    @Schema(description = "宠物ID（必须属于当前用户）")
    @NotNull(message = "petId cannot be empty")
    @JsonAlias("petId")
    private Long pet_id_wsh;

    @Schema(description = "开始日期（day 模式必填）")
    @JsonAlias("startDate")
    private LocalDate start_date_wsh;

    @Schema(description = "结束日期（day 模式必填；由服务端校验与开始日期的跨度）")
    @JsonAlias("endDate")
    private LocalDate end_date_wsh;

    @Schema(description = "送达时间（day 模式可选，缺省由服务端按营业时间推导）")
    @JsonAlias("deliveryTime")
    private LocalDateTime delivery_time_wsh;

    @Schema(description = "接回时间（day 模式可选，缺省由服务端按营业时间推导）")
    @JsonAlias("pickupTime")
    private LocalDateTime pickup_time_wsh;

    @Schema(description = "计费数量（可选；day 模式由日期跨度推导，服务端重算）")
    @JsonAlias("quantity")
    private Integer quantity_wsh;
}
