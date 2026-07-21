package com.pet.customer.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("merchant_customer_service_wsh")
public class MerchantCustomerService {
    @TableId(value = "id_wsh", type = IdType.AUTO)
    private Long id_wsh;

    @TableField(value = "merchant_id_wsh")
    private Long merchant_id_wsh;

    @TableField(value = "user_id_wsh")
    private Long user_id_wsh;

    @TableField(value = "applicant_note_wsh")
    private String applicant_note_wsh;

    @TableField(value = "review_note_wsh")
    private String review_note_wsh;

    @TableField(value = "status_wsh")
    private String status_wsh;

    @TableField(value = "reviewer_id_wsh")
    private Long reviewer_id_wsh;

    @TableField(value = "reviewed_at_wsh")
    private LocalDateTime reviewed_at_wsh;

    @JsonIgnore
    @TableLogic
    @TableField(value = "deleted_wsh")
    private Integer deleted_wsh;

    @TableField(value = "created_at_wsh", fill = FieldFill.INSERT)
    private LocalDateTime created_at_wsh;

    @TableField(value = "updated_at_wsh", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updated_at_wsh;
}
