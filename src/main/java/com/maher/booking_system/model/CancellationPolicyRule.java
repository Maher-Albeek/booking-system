package com.maher.booking_system.model;

public class CancellationPolicyRule {
    private Integer minimumHoursBeforePickup;
    private Integer refundPercentage;
    private String label;

    public Integer getMinimumHoursBeforePickup() { return minimumHoursBeforePickup; }
    public void setMinimumHoursBeforePickup(Integer minimumHoursBeforePickup) {
        this.minimumHoursBeforePickup = minimumHoursBeforePickup;
    }

    public Integer getRefundPercentage() { return refundPercentage; }
    public void setRefundPercentage(Integer refundPercentage) { this.refundPercentage = refundPercentage; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
