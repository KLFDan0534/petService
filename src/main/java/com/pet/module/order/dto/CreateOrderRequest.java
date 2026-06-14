package com.pet.module.order.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateOrderRequest {
    @NotNull(message = "宠物ID不能为空")
    private Long petId;

    @NotNull(message = "寄养员ID不能为空")
    private Long keeperId;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    private Long serviceId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private String remark;

    public Long getPetId() { return petId; }
    public void setPetId(Long petId) { this.petId = petId; }
    public Long getKeeperId() { return keeperId; }
    public void setKeeperId(Long keeperId) { this.keeperId = keeperId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
