package com.maher.booking_system.model;

import com.maher.booking_system.model.enums.DiscountType;

import java.time.LocalDateTime;

public class PromoCode {
    private Long id;
    private String code;
    private DiscountType discountType;
    private Double discountValue;
    private LocalDateTime expiryDateTime;
    private Integer usageLimit;
    private Integer usedCount;
    private boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
    public LocalDateTime getExpiryDateTime() { return expiryDateTime; }
    public void setExpiryDateTime(LocalDateTime expiryDateTime) { this.expiryDateTime = expiryDateTime; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
