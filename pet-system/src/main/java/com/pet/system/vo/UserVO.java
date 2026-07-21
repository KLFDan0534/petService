package com.pet.system.vo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias("id")
    private Long id_wsh;
    @Schema(description = "用户名")
    @JsonAlias("username")
    private String username_wsh;
    @Schema(description = "昵称")
    @JsonAlias("nickname")
    private String nickname_wsh;
    @Schema(description = "手机号")
    @JsonAlias("phone")
    private String phone_wsh;
    @Schema(description = "头像URL")
    @JsonAlias("avatar")
    private String avatar_wsh;
    @Schema(description = "性别：0-未知 1-男 2-女")
    @JsonAlias("gender")
    private Integer gender_wsh;
    @Schema(description = "邮箱")
    @JsonAlias("email")
    private String email_wsh;
    @Schema(description = "真实姓名")
    @JsonAlias("realName")
    private String real_name_wsh;
    @Schema(description = "身份证号")
    @JsonAlias("idCardNo")
    private String id_card_no_wsh;
    @Schema(description = "实名状态：0-未认证 1-待审核 2-已认证 3-已驳回")
    @JsonAlias("realNameStatus")
    private Integer real_name_status_wsh;
    @Schema(description = "地址")
    @JsonAlias("address")
    private String address_wsh;
    @Schema(description = "纬度")
    @JsonAlias("latitude")
    private BigDecimal latitude_wsh;
    @Schema(description = "经度")
    @JsonAlias("longitude")
    private BigDecimal longitude_wsh;
    @Schema(description = "状态：0-禁用 1-启用")
    @JsonAlias("status")
    private Integer status_wsh;
    @Schema(description = "是否已设置支付密码")
    @JsonAlias("paymentPasswordSet")
    private Boolean payment_password_set_wsh;
    @Schema(description = "角色列表")
    @JsonAlias("roles")
    private List<String> roles_wsh;
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonGetter("id")
    public Long getId() {
        return id_wsh;
    }

    @JsonGetter("username")
    public String getUsername() {
        return username_wsh;
    }

    @JsonGetter("nickname")
    public String getNickname() {
        return nickname_wsh;
    }

    @JsonGetter("phone")
    public String getPhone() {
        return phone_wsh;
    }

    @JsonGetter("email")
    public String getEmail() {
        return email_wsh;
    }

    @JsonGetter("roles")
    public List<String> getRoles() {
        return roles_wsh;
    }
}
