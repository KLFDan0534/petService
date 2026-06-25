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

@Getter
@Setter
@TableName("user_wsh")
public class User {
    @JsonProperty("id_wsh")
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @JsonProperty("username_wsh")
    @TableField(value = "username_wsh")
    private String username_wsh;

    @JsonIgnore
    @TableField(value = "password_wsh")
    private String password_wsh;

    @JsonProperty("nickname_wsh")
    @TableField(value = "nickname_wsh")
    private String nickname_wsh;

    @JsonProperty("phone_wsh")
    @TableField(value = "phone_wsh")
    private String phone_wsh;

    @JsonProperty("avatar_wsh")
    @TableField(value = "avatar_wsh")
    private String avatar_wsh;

    @JsonProperty("email_wsh")
    @TableField(value = "email_wsh")
    private String email_wsh;

    @JsonProperty("address_wsh")
    @TableField(value = "address_wsh")
    private String address_wsh;

    @JsonProperty("latitude_wsh")
    @TableField(value = "latitude_wsh")
    private java.math.BigDecimal latitude_wsh;

    @JsonProperty("longitude_wsh")
    @TableField(value = "longitude_wsh")
    private java.math.BigDecimal longitude_wsh;

    @JsonProperty("status_wsh")
    @TableField(value = "status_wsh")
    private Integer status_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @JsonProperty("created_at_wsh")
    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @JsonProperty("updated_at_wsh")
    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;

    public UserVO toVO(List<String> roles) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(this, vo);
        vo.setRoles_wsh(roles);
        return vo;
    }
}
