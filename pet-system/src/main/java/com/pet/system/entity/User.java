package com.pet.system.entity;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pet.system.vo.UserVO;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@TableName("user_wsh")
@Schema(description = "用户实体")
public class User {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id_wsh;

    @JsonProperty("username_wsh")
    @TableField(value = "username_wsh")
    @Schema(description = "用户名")
    private String username_wsh;

    @JsonIgnore
    @TableField(value = "password_wsh")
    @Schema(description = "密码")
    private String password_wsh;

    @JsonProperty("nickname_wsh")
    @TableField(value = "nickname_wsh")
    @Schema(description = "昵称")
    private String nickname_wsh;

    @JsonProperty("phone_wsh")
    @TableField(value = "phone_wsh")
    @Schema(description = "手机号")
    private String phone_wsh;

    @JsonProperty("avatar_wsh")
    @TableField(value = "avatar_wsh")
    @Schema(description = "头像URL")
    private String avatar_wsh;

    @JsonProperty("gender_wsh")
    @TableField(value = "gender_wsh")
    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender_wsh;

    @JsonProperty("email_wsh")
    @TableField(value = "email_wsh")
    @Schema(description = "邮箱")
    private String email_wsh;

    @JsonProperty("real_name_wsh")
    @TableField(value = "real_name_wsh")
    @Schema(description = "真实姓名")
    private String real_name_wsh;

    @JsonProperty("id_card_no_wsh")
    @TableField(value = "id_card_no_wsh")
    @Schema(description = "身份证号")
    private String id_card_no_wsh;

    @JsonProperty("real_name_status_wsh")
    @TableField(value = "real_name_status_wsh")
    @Schema(description = "实名状态：0-未认证 1-待审核 2-已认证 3-已驳回")
    private Integer real_name_status_wsh;

    @JsonProperty("reject_reason_wsh")
    @TableField(value = "reject_reason_wsh")
    @Schema(description = "实名审核驳回原因")
    private String reject_reason_wsh;

    @JsonIgnore
    @TableField(value = "payment_password_wsh")
    @Schema(description = "支付密码")
    private String payment_password_wsh;

    @JsonProperty("address_wsh")
    @TableField(value = "address_wsh")
    @Schema(description = "地址")
    private String address_wsh;

    @JsonProperty("latitude_wsh")
    @TableField(value = "latitude_wsh")
    @Schema(description = "纬度")
    private java.math.BigDecimal latitude_wsh;

    @JsonProperty("longitude_wsh")
    @TableField(value = "longitude_wsh")
    @Schema(description = "经度")
    private java.math.BigDecimal longitude_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    @Schema(description = "状态")
    private Integer status_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    @Schema(description = "逻辑删除标志")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updated_at_wsh;

    public UserVO toVO(List<String> roles) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(this, vo);
        vo.setRoles_wsh(roles);
        return vo;
    }
}
