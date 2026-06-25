package com.pet.ai.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AgentExecuteResult {
    private String status;
    private String orderNo;
    private String payNo;
    private Object selectedKeeper;
    private String petName;
    private Integer days;
    private List<String> logs;
    private Integer currentStep;
    private String error;
}
