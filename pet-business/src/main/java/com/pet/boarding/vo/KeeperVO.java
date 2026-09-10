package com.pet.boarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import com.pet.qualification.dto.QualificationDTO;
import java.math.BigDecimal;
import java.util.List;

/**
 * 看护者视图对象
 * 包含看护者基本信息及关联商户的距离等信息
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Getter
@Setter
public class KeeperVO {
    @Schema(description = "看护者ID")
    private Long id_wsh;
    @Schema(description = "所属商户ID")
    private Long merchant_id_wsh;
    @Schema(description = "商户名称")
    private String merchant_name_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "看护者姓名")
    private String name_wsh;
    @Schema(description = "联系电话")
    private String phone_wsh;
    @Schema(description = "头像URL")
    private String avatar_wsh;
    @Schema(description = "从业年限")
    private Integer experience_years_wsh;
    @Schema(description = "评分")
    private BigDecimal rating_wsh;
    @Schema(description = "完成率")
    private BigDecimal completion_rate_wsh;
    @Schema(description = "投诉率")
    private BigDecimal complaint_rate_wsh;
    @Schema(description = "每日价格")
    private BigDecimal price_per_day_wsh;
    @Schema(description = "最大可接待宠物数")
    private Integer max_pets_wsh;
    @Schema(description = "当前接待宠物数")
    private Integer current_pets_wsh;
    @Schema(description = "个人简介")
    private String bio_wsh;
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status_wsh;
    @Schema(description = "距离（公里）")
    private Double distance_wsh;
    @Schema(description = "商户纬度")
    private BigDecimal merchant_latitude_wsh;
    @Schema(description = "商户经度")
    private BigDecimal merchant_longitude_wsh;
    @Schema(description = "资质列表")
    private List<QualificationDTO> qualifications_wsh;
}
