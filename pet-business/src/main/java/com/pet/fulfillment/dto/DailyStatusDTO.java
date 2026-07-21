package com.pet.fulfillment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class DailyStatusDTO {
    @Schema(description = "应打卡日期列表")
    private List<String> required_days_wsh;
    @Schema(description = "已打卡日期列表")
    private List<String> uploaded_days_wsh;
    @Schema(description = "缺卡日期列表")
    private List<String> missing_days_wsh;
    @Schema(description = "是否已完成")
    private boolean complete_wsh;
    @Schema(description = "打卡记录数")
    private int record_count_wsh;
}
