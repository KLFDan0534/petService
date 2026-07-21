package com.pet.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AgentContext {
    @Schema(description = "鐢ㄦ埛ID")
    private Long userId;
    @Schema(description = "鐢ㄦ埛杈撳叆")
    private String userInput;
    @Schema(description = "瀹犵墿绫诲瀷")
    private String petType;
    @Schema(description = "澶╂暟")
    private Integer days;
    @Schema(description = "瀹犵墿ID")
    private Long petId;
    @Schema(description = "瀹犵墿鍚嶇О")
    private String petName;
    @Schema(description = "鐢ㄦ埛缁村害")
    private Double userLatitude;
    @Schema(description = "鐢ㄦ埛缁忓害")
    private Double userLongitude;
    @Schema(description = "鍟嗗鍒楄〃")
    private List<Map<String, Object>> merchants;
    @Schema(description = "瀵勫吇浜哄垪琛?")
    private List<Map<String, Object>> keepers;
    @Schema(description = "宸查€夊畾鐨勫瘎鍏讳汉")
    private Map<String, Object> selectedKeeper;
    @Schema(description = "璁㈠崟鍙?")
    private String orderNo;
    @Schema(description = "鏀粯鍙?")
    private String payNo;
    @Schema(description = "褰撳墠姝ラ")
    private Integer currentStep;
    @Schema(description = "鐘舵€?")
    private String status;
    @Schema(description = "鏃ュ織鍒楄〃")
    private List<String> logs;
    @Schema(description = "閿欒淇℃伅")
    private String error;
}
