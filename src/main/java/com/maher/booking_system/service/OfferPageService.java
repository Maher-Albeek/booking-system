package com.maher.booking_system.service;

import com.maher.booking_system.model.OfferSection;
import com.maher.booking_system.repository.OfferDraftRepository;
import com.maher.booking_system.repository.OfferPublishedRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OfferPageService {

    private static final String DEFAULT_BACKGROUND_COLOR = "#10243a";
    private static final String DEFAULT_TEXT_COLOR = "#f7f2ea";

    private final OfferDraftRepository draftRepository;
    private final OfferPublishedRepository publishedRepository;

    public OfferPageService(OfferDraftRepository draftRepository, OfferPublishedRepository publishedRepository) {
        this.draftRepository = draftRepository;
        this.publishedRepository = publishedRepository;
    }

    public List<OfferSection> getDraftSections() {
        return normalizeSections(draftRepository.findAll());
    }

    public List<OfferSection> saveDraftSections(List<OfferSection> sections) {
        List<OfferSection> normalized = normalizeSections(sections);
        draftRepository.replaceAll(normalized);
        return normalized;
    }

    public List<OfferSection> getPublishedSections() {
        return normalizeSections(publishedRepository.findAll());
    }

    public List<OfferSection> publishDraftSections() {
        List<OfferSection> normalizedDraft = normalizeSections(draftRepository.findAll());
        draftRepository.replaceAll(normalizedDraft);
        publishedRepository.replaceAll(normalizedDraft);
        return normalizedDraft;
    }

    private List<OfferSection> normalizeSections(List<OfferSection> sections) {
        List<OfferSection> safeSections = sections == null ? List.of() : sections;
        List<OfferSection> normalized = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();
        long nextId = 1L;
        int sortOrder = 0;

        for (OfferSection input : safeSections) {
            if (input == null) {
                continue;
            }

            OfferSection copy = new OfferSection();

            Long inputId = input.getId();
            Long normalizedId = null;
            if (inputId != null && inputId > 0 && usedIds.add(inputId)) {
                normalizedId = inputId;
            } else {
                while (usedIds.contains(nextId)) {
                    nextId++;
                }
                normalizedId = nextId;
                usedIds.add(nextId);
                nextId++;
            }

            copy.setId(normalizedId);
            copy.setSortOrder(sortOrder++);
            copy.setTitle(normalizeText(input.getTitle()));
            copy.setDescription(normalizeText(input.getDescription()));
            copy.setImageUrl(normalizeText(input.getImageUrl()));
            copy.setBackgroundColor(normalizeColor(input.getBackgroundColor(), DEFAULT_BACKGROUND_COLOR));
            copy.setTextColor(normalizeColor(input.getTextColor(), DEFAULT_TEXT_COLOR));
            copy.setHeightPx(clampInt(input.getHeightPx(), 220, 980, 380));
            copy.setColumns(clampInt(input.getColumns(), 1, 3, 1));
            copy.setDescriptionColumnGapPx(clampInt(input.getDescriptionColumnGapPx(), 0, 120, 24));
            copy.setDescriptionColumnDividerWidthPx(clampInt(input.getDescriptionColumnDividerWidthPx(), 0, 12, 1));
            copy.setDescriptionColumnDividerColor(normalizeColor(input.getDescriptionColumnDividerColor(), DEFAULT_TEXT_COLOR));
            copy.setTitleFontSizePx(clampInt(input.getTitleFontSizePx(), 20, 96, 36));
            copy.setDescriptionFontSizePx(clampInt(input.getDescriptionFontSizePx(), 12, 52, 18));
            copy.setTitleXPercent(clampDouble(input.getTitleXPercent(), 2, 80, 8));
            copy.setTitleYPercent(clampDouble(input.getTitleYPercent(), 2, 78, 12));
            copy.setDescriptionXPercent(clampDouble(input.getDescriptionXPercent(), 2, 80, 8));
            copy.setDescriptionYPercent(clampDouble(input.getDescriptionYPercent(), 2, 86, 40));

            normalized.add(copy);
        }

        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeColor(String value, String fallback) {
        String normalized = normalizeText(value);
        if (normalized.matches("^#[0-9a-fA-F]{6}$")) {
            return normalized;
        }
        return fallback;
    }

    private Integer clampInt(Integer value, int min, int max, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private Double clampDouble(Double value, double min, double max, double fallback) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
