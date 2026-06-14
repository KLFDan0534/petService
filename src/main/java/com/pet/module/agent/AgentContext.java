package com.pet.module.agent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserInput() { return userInput; }
    public void setUserInput(String userInput) { this.userInput = userInput; }
    public String getPetType() { return petType; }
    public void setPetType(String petType) { this.petType = petType; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public Double getUserLatitude() { return userLatitude; }
    public void setUserLatitude(Double userLatitude) { this.userLatitude = userLatitude; }
    public Double getUserLongitude() { return userLongitude; }
    public void setUserLongitude(Double userLongitude) { this.userLongitude = userLongitude; }
    public List<Map<String, Object>> getMerchants() { return merchants; }
    public void setMerchants(List<Map<String, Object>> merchants) { this.merchants = merchants; }
    public List<Map<String, Object>> getKeepers() { return keepers; }
    public void setKeepers(List<Map<String, Object>> keepers) { this.keepers = keepers; }
    public Map<String, Object> getSelectedKeeper() { return selectedKeeper; }
    public void setSelectedKeeper(Map<String, Object> selectedKeeper) { this.selectedKeeper = selectedKeeper; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPayNo() { return payNo; }
    public void setPayNo(String payNo) { this.payNo = payNo; }
    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getLogs() { return logs; }
    public void setLogs(List<String> logs) { this.logs = logs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
