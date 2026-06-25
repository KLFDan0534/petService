package com.pet.ai.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AgentContext {
    private Long userId;
    private String userInput;
    private String petType;
    private Integer days;
    private Long petId;
    private String petName;
    private Double userLatitude;
    private Double userLongitude;
    private List<Map<String, Object>> merchants;
    private List<Map<String, Object>> keepers;
    private Map<String, Object> selectedKeeper;
    private String orderNo;
    private String payNo;
    private Integer currentStep;
    private String status;
    private List<String> logs;
    private String error;
}
