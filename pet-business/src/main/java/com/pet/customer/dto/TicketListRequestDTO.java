package com.pet.customer.dto;

import com.pet.common.PageRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 【工单列表查询参数】
 * <p>在通用分页参数基础上扩展筛选条件：
 * 状态、分类、优先级、处理人、关键字（标题/内容模糊匹配）。</p>
 * <p>调用场景：管理员/商家/客服查看工单列表时按条件筛选。</p>
 */
@Getter
@Setter
public class TicketListRequestDTO extends PageRequestDTO {

    @Schema(description = "工单状态: pending/processing/resolved/closed")
    private String status_wsh;

    @Schema(description = "工单分类: complaint/question/suggestion/other")
    private String category_wsh;

    @Schema(description = "工单优先级: low/medium/high/urgent")
    private String priority_wsh;

    @Schema(description = "处理人用户ID，只看某人处理的工单")
    private Long assignee_id_wsh;

    @Schema(description = "关键字，模糊匹配标题或内容")
    private String keyword;

    @Schema(description = "所属商家ID")
    private Long merchant_id_wsh;
}
