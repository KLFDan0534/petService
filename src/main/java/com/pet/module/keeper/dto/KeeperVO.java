package com.pet.module.keeper.dto;

import java.math.BigDecimal;

public class KeeperVO {
    private Long id;
    private Long merchantId;
    private String merchantName;
    private Long userId;
    private String name;
    private String phone;
    private String avatar;
    private Integer experienceYears;
    private BigDecimal rating;
    private BigDecimal completionRate;
    private BigDecimal complaintRate;
    private BigDecimal pricePerDay;
    private Integer maxPets;
    private Integer currentPets;
    private Integer status;
    private Double distance;
    private BigDecimal merchantLatitude;
    private BigDecimal merchantLongitude;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public BigDecimal getCompletionRate() { return completionRate; }
    public void setCompletionRate(BigDecimal completionRate) { this.completionRate = completionRate; }
    public BigDecimal getComplaintRate() { return complaintRate; }
    public void setComplaintRate(BigDecimal complaintRate) { this.complaintRate = complaintRate; }
    public BigDecimal getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }
    public Integer getMaxPets() { return maxPets; }
    public void setMaxPets(Integer maxPets) { this.maxPets = maxPets; }
    public Integer getCurrentPets() { return currentPets; }
    public void setCurrentPets(Integer currentPets) { this.currentPets = currentPets; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    public BigDecimal getMerchantLatitude() { return merchantLatitude; }
    public void setMerchantLatitude(BigDecimal merchantLatitude) { this.merchantLatitude = merchantLatitude; }
    public BigDecimal getMerchantLongitude() { return merchantLongitude; }
    public void setMerchantLongitude(BigDecimal merchantLongitude) { this.merchantLongitude = merchantLongitude; }
}
