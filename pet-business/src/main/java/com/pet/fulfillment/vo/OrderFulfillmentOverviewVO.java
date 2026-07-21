package com.pet.fulfillment.vo;

import com.pet.fulfillment.dto.DailyStatusDTO;
import com.pet.order.dto.OrderDTO;
import com.pet.pet.dto.CareRecordDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class OrderFulfillmentOverviewVO {
    @Schema(description = "订单信息")
    private OrderDTO order_wsh;
    @Schema(description = "当前用户角色")
    private String role_wsh;
    @Schema(description = "宠物主人用户ID")
    private Long owner_user_id_wsh;
    @Schema(description = "看护者用户ID")
    private Long keeper_user_id_wsh;
    @Schema(description = "商户用户ID")
    private Long merchant_user_id_wsh;
    @Schema(description = "时间线记录列表")
    private List<CareRecordDTO> timeline_wsh;
    @Schema(description = "每日状态")
    private DailyStatusDTO daily_status_wsh;
}
