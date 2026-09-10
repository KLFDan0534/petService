package com.pet.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserVO {
    @Schema(description = "用户ID")
    private Long id_wsh;
    @Schema(description = "用户名")
    private String username_wsh;
    @Schema(description = "昵称")
    private String nickname_wsh;
    @Schema(description = "手机号")
    private String phone_wsh;
    @Schema(description = "头像URL")
    private String avatar_wsh;
    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender_wsh;
    @Schema(description = "邮箱")
    private String email_wsh;
    @Schema(description = "真实姓名")
    private String real_name_wsh;
    @Schema(description = "身份证号")
    private String id_card_no_wsh;
    @Schema(description = "实名状态：0-未认证 1-待审核 2-已认证 3-已驳回")
    private Integer real_name_status_wsh;
    @Schema(description = "地址")
    private String address_wsh;
    @Schema(description = "纬度")
    private BigDecimal latitude_wsh;
    @Schema(description = "经度")
    private BigDecimal longitude_wsh;
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status_wsh;
    @Schema(description = "是否已设置支付密码")
    private Boolean payment_password_set_wsh;
    @Schema(description = "角色列表")
    private List<String> roles_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;
}
