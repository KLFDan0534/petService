package com.pet.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReputationStatsVO {
    @Schema(description = "总评价数")
    private long totalRatings;
    @Schema(description = "平均评分")
    private BigDecimal avgRating;
    @Schema(description = "总完成数")
    private long totalCompleted;
    @Schema(description = "完成率")
    private BigDecimal completionRate;
    @Schema(description = "投诉率")
    private BigDecimal complaintRate;
    @Schema(description = "总小费数")
    private long totalTips;
}
