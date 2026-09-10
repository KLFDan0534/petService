package com.pet.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReputationStatsVO {
    @Schema(description = "总评价数")
    private long total_ratings_wsh;
    @Schema(description = "平均评分")
    private BigDecimal avg_rating_wsh;
    @Schema(description = "总完成数")
    private long total_completed_wsh;
    @Schema(description = "完成率")
    private BigDecimal completion_rate_wsh;
    @Schema(description = "投诉率")
    private BigDecimal complaint_rate_wsh;
    @Schema(description = "总小费数")
    private long total_tips_wsh;
}
