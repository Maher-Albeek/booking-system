package com.maher.booking_system.model;

public class LoyaltyAccount {
    private Long id;
    private Long userId;
    private Long pointsBalance;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(Long pointsBalance) { this.pointsBalance = pointsBalance; }
}
