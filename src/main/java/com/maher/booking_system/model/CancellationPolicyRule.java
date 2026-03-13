package com.maher.booking_system.model;

public class CancellationPolicyRule {
    private Long id;
    private Integer minHoursBeforePickup;
    private Integer maxHoursBeforePickup;
    private Integer refundPercent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getMinHoursBeforePickup() { return minHoursBeforePickup; }
    public void setMinHoursBeforePickup(Integer minHoursBeforePickup) { this.minHoursBeforePickup = minHoursBeforePickup; }
    public Integer getMaxHoursBeforePickup() { return maxHoursBeforePickup; }
    public void setMaxHoursBeforePickup(Integer maxHoursBeforePickup) { this.maxHoursBeforePickup = maxHoursBeforePickup; }
    public Integer getRefundPercent() { return refundPercent; }
    public void setRefundPercent(Integer refundPercent) { this.refundPercent = refundPercent; }
}
