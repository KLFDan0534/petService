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

/**
 * 商家客服申请实体，记录用户向商家申请成为客服人员的信息。
 * <p>状态流转：pending（待审核）-> approved（已通过）/ rejected（已拒绝）
 * -> resigned（已辞职）/ terminated（已终止）。
 * 审批通过后用户获得 CUSTOMER_SERVICE 角色。</p>
 */
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
