package com.maher.booking_system.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OfferSection {

    private Long id;
    private Integer sortOrder;
    private String title;
    private String description;
    private String imageUrl;
    private String backgroundColor;
    private String textColor;
    private Integer heightPx;
    private Integer columns;
    private Integer descriptionColumnGapPx;
    private Integer descriptionColumnDividerWidthPx;
    private String descriptionColumnDividerColor;
    private Integer titleFontSizePx;
    private Integer descriptionFontSizePx;
    private Double titleXPercent;
    private Double titleYPercent;
    private Double descriptionXPercent;
    private Double descriptionYPercent;
    private Boolean enabled;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String ctaLabel;
    private List<Long> linkedResourceIds = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public Integer getHeightPx() {
        return heightPx;
    }

    public void setHeightPx(Integer heightPx) {
        this.heightPx = heightPx;
    }

    public Integer getColumns() {
        return columns;
    }

    public void setColumns(Integer columns) {
        this.columns = columns;
    }

    public Integer getDescriptionColumnGapPx() {
        return descriptionColumnGapPx;
    }

    public void setDescriptionColumnGapPx(Integer descriptionColumnGapPx) {
        this.descriptionColumnGapPx = descriptionColumnGapPx;
    }

    public Integer getDescriptionColumnDividerWidthPx() {
        return descriptionColumnDividerWidthPx;
    }

    public void setDescriptionColumnDividerWidthPx(Integer descriptionColumnDividerWidthPx) {
        this.descriptionColumnDividerWidthPx = descriptionColumnDividerWidthPx;
    }

    public String getDescriptionColumnDividerColor() {
        return descriptionColumnDividerColor;
    }

    public void setDescriptionColumnDividerColor(String descriptionColumnDividerColor) {
        this.descriptionColumnDividerColor = descriptionColumnDividerColor;
    }

    public Integer getTitleFontSizePx() {
        return titleFontSizePx;
    }

    public void setTitleFontSizePx(Integer titleFontSizePx) {
        this.titleFontSizePx = titleFontSizePx;
    }

    public Integer getDescriptionFontSizePx() {
        return descriptionFontSizePx;
    }

    public void setDescriptionFontSizePx(Integer descriptionFontSizePx) {
        this.descriptionFontSizePx = descriptionFontSizePx;
    }

    public Double getTitleXPercent() {
        return titleXPercent;
    }

    public void setTitleXPercent(Double titleXPercent) {
        this.titleXPercent = titleXPercent;
    }

    public Double getTitleYPercent() {
        return titleYPercent;
    }

    public void setTitleYPercent(Double titleYPercent) {
        this.titleYPercent = titleYPercent;
    }

    public Double getDescriptionXPercent() {
        return descriptionXPercent;
    }

    public void setDescriptionXPercent(Double descriptionXPercent) {
        this.descriptionXPercent = descriptionXPercent;
    }

    public Double getDescriptionYPercent() {
        return descriptionYPercent;
    }

    public void setDescriptionYPercent(Double descriptionYPercent) {
        this.descriptionYPercent = descriptionYPercent;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getCtaLabel() {
        return ctaLabel;
    }

    public void setCtaLabel(String ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    public List<Long> getLinkedResourceIds() {
        return linkedResourceIds == null ? List.of() : linkedResourceIds;
    }

    public void setLinkedResourceIds(List<Long> linkedResourceIds) {
        this.linkedResourceIds = linkedResourceIds == null ? new ArrayList<>() : new ArrayList<>(linkedResourceIds);
    }
}
