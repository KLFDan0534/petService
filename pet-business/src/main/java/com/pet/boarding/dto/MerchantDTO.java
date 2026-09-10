package com.pet.boarding.dto;

import lombok.Data;
import com.pet.qualification.dto.QualificationDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商家数据传输对象
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Data
public class MerchantDTO {
    @Schema(description = "商家ID")
    private Long id_wsh;
    @Schema(description = "用户ID")
    private Long user_id_wsh;
    @Schema(description = "商家名称")
    private String name_wsh;
    @Schema(description = "手机号")
    private String phone_wsh;
    @Schema(description = "地址")
    private String address_wsh;
    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;
    @Schema(description = "经度")
    private BigDecimal longitude_wsh;
    @Schema(description = "描述")
    private String description_wsh;
    @Schema(description = "营业执照")
    private String business_license_wsh;
    @Schema(description = "评分")
    private BigDecimal rating_wsh;
    @Schema(description = "距离(公里)")
    private Double distance_wsh;
    @Schema(description = "状态")
    private Integer status_wsh;
    @Schema(description = "店铺模式")
    private Integer store_mode_wsh;
    @Schema(description = "店铺状态")
    private Integer store_status_wsh;
    @Schema(description = "是否接受未来预约: 0-关闭 1-开启")
    private Integer future_booking_enabled_wsh;
    @Schema(description = "店主昵称(店铺归属人)")
    private String owner_name_wsh;
    @Schema(description = "店主头像URL")
    private String owner_avatar_wsh;
    @Schema(description = "店主看护者主页ID(店主同时是看护者时非空)")
    private Long owner_keeper_id_wsh;
    @Schema(description = "资质列表")
    private List<QualificationDTO> qualifications_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
