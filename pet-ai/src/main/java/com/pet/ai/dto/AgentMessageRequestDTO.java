package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能下单「一轮对话」请求。
 * <p>
 * {@code session_id_wsh} 为空 = 首轮：服务端先在内存里跑完一整轮，
 * 全部成功后才建会话入库；因此「点开页面但没发消息」不会产生任何记录。
 * <p>
 * 前端在首轮之前就生成好会话ID并每轮带上，配合 {@code request_id_wsh} 做到：
 * 网络超时重发时既不会多建一个会话，也不会把同一句话记两遍。
 */
@Data
public class AgentMessageRequestDTO {

    @Size(max = 64, message = "会话ID过长")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "会话ID格式不正确")
    @Schema(description = "会话ID；首轮由前端预先生成并沿用，为空时服务端兜底生成")
    private String session_id_wsh;

    @Size(max = 64, message = "request_id 过长")
    @Schema(description = "本轮请求唯一标识（前端生成），用于超时重发的幂等去重")
    private String request_id_wsh;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息过长，请控制在 500 字符以内")
    @Schema(description = "用户这一轮说的话")
    private String content_wsh;

    @Schema(description = "纬度（可空，首轮建议传；后续轮次可省略，服务端沿用）")
    private Double latitude_wsh;

    @Schema(description = "经度（可空）")
    private Double longitude_wsh;

    @Size(max = 200, message = "地址过长")
    @Schema(description = "定位地址（可空，仅展示）")
    private String address_wsh;
}
