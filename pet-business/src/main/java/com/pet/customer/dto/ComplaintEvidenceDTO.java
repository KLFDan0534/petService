package com.pet.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pet.pet.dto.CareRecordDTO;
import lombok.Data;

import java.util.List;

@Data
public class ComplaintEvidenceDTO {
    @Schema(description = "投诉信息")
    private ComplaintDTO complaint_wsh;
    @Schema(description = "证据摘要")
    private ComplaintEvidenceSummaryDTO summary_wsh;
    @Schema(description = "聊天消息列表")
    private List<ChatMessageDTO> chat_messages_wsh;
    @Schema(description = "服务记录列表")
    private List<CareRecordDTO> care_records_wsh;
}
