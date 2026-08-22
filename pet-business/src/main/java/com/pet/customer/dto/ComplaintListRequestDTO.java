package com.pet.customer.dto;

import com.pet.common.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 【投诉列表查询参数】
 * <p>在通用分页参数基础上扩展筛选条件：状态、商家、关键字。</p>
 * <p>调用场景：管理员/商家/客服查看投诉列表时按条件筛选。</p>
 */
@Getter
@Setter
public class ComplaintListRequestDTO extends PageRequestDTO {

    @Schema(description = "投诉状态: pending/processing/resolved/rejected")
    private String status_wsh;

    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;

    @Schema(description = "关键字，模糊匹配标题或内容")
    private String keyword;
}
