package com.maher.booking_system.model;

import com.maher.booking_system.model.enums.DiscountType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OfferCampaign {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String titleColor;
    private String descriptionColor;
    private Integer titleFontSizePx;
    private Integer descriptionFontSizePx;
    private String sectionLayout;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean enabled;
    private DiscountType discountType;
    private Double discountValue;
    private List<Long> eligibleCarIds = new ArrayList<>();
    private Long bookingsCount;
    private Long revenueCents;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTitleColor() { return titleColor; }
    public void setTitleColor(String titleColor) { this.titleColor = titleColor; }
    public String getDescriptionColor() { return descriptionColor; }
    public void setDescriptionColor(String descriptionColor) { this.descriptionColor = descriptionColor; }
    public Integer getTitleFontSizePx() { return titleFontSizePx; }
    public void setTitleFontSizePx(Integer titleFontSizePx) { this.titleFontSizePx = titleFontSizePx; }
    public Integer getDescriptionFontSizePx() { return descriptionFontSizePx; }
    public void setDescriptionFontSizePx(Integer descriptionFontSizePx) { this.descriptionFontSizePx = descriptionFontSizePx; }
    public String getSectionLayout() { return sectionLayout; }
    public void setSectionLayout(String sectionLayout) { this.sectionLayout = sectionLayout; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
    public List<Long> getEligibleCarIds() { return eligibleCarIds == null ? List.of() : eligibleCarIds; }
    public void setEligibleCarIds(List<Long> eligibleCarIds) { this.eligibleCarIds = eligibleCarIds == null ? new ArrayList<>() : new ArrayList<>(eligibleCarIds); }
    public Long getBookingsCount() { return bookingsCount; }
    public void setBookingsCount(Long bookingsCount) { this.bookingsCount = bookingsCount; }
    public Long getRevenueCents() { return revenueCents; }
    public void setRevenueCents(Long revenueCents) { this.revenueCents = revenueCents; }
}
