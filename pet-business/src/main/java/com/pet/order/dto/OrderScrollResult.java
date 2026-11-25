package com.pet.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 订单游标分页结果。
 * <p>
 * 为什么不用 PageResult：游标分页没有「页码」概念，也不需要每次返回 total
 * （COUNT 全量在大表上是额外成本）。它只回答两个问题：这一页有哪些、
 * 下一页从哪里开始。
 * <p>
 * 游标内容为 base64(created_at ISO-8601 + "|" + id)，由服务端生成，
 * 前端只需原样回传，不要解析。
 *
 * @author: wsh
 */
@Data
public class OrderScrollResult {

    @Schema(description = "当前页订单列表")
    private List<OrderDTO> list_wsh;

    @Schema(description = "下一页游标；为 null 表示已到末尾")
    private String next_cursor_wsh;

    @Schema(description = "是否还有下一页")
    private Boolean has_more_wsh;

    @Schema(description = "本页返回条数")
    private Integer size_wsh;

    /**
     * 业务作用：构造「已到末尾」的空结果。
     * 业务规则：list 为空集合而非 null，前端可直接遍历。
     *
     * @return 空结果
     */
    public static OrderScrollResult empty() {
        OrderScrollResult result = new OrderScrollResult();
        result.setList_wsh(List.of());
        result.setNext_cursor_wsh(null);
        result.setHas_more_wsh(false);
        result.setSize_wsh(0);
        return result;
    }
}
