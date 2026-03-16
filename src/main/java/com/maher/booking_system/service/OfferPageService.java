package com.maher.booking_system.service;

import com.maher.booking_system.dto.OfferAnalyticsResponse;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.OfferSection;
import com.maher.booking_system.model.OfferPageSettings;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.model.enums.PaymentStatus;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.OfferDraftRepository;
import com.maher.booking_system.repository.OfferPageSettingsDraftRepository;
import com.maher.booking_system.repository.OfferPageSettingsPublishedRepository;
import com.maher.booking_system.repository.OfferPublishedRepository;
import com.maher.booking_system.repository.ResourcesRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OfferPageService {

    private static final String DEFAULT_BACKGROUND_COLOR = "#10243a";
    private static final String DEFAULT_TEXT_COLOR = "#f7f2ea";

    private final OfferDraftRepository draftRepository;
    private final OfferPublishedRepository publishedRepository;
    private final OfferPageSettingsDraftRepository settingsDraftRepository;
    private final OfferPageSettingsPublishedRepository settingsPublishedRepository;
    private final ResourcesRepository resourcesRepository;
    private final BookingRepository bookingRepository;

    public OfferPageService(
            OfferDraftRepository draftRepository,
            OfferPublishedRepository publishedRepository,
            OfferPageSettingsDraftRepository settingsDraftRepository,
            OfferPageSettingsPublishedRepository settingsPublishedRepository,
            ResourcesRepository resourcesRepository,
            BookingRepository bookingRepository
    ) {
        this.draftRepository = draftRepository;
        this.publishedRepository = publishedRepository;
        this.settingsDraftRepository = settingsDraftRepository;
        this.settingsPublishedRepository = settingsPublishedRepository;
        this.resourcesRepository = resourcesRepository;
        this.bookingRepository = bookingRepository;
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

    public List<OfferSection> getLiveSections() {
        LocalDateTime now = LocalDateTime.now();
        return getPublishedSections().stream()
                .filter(section -> isOfferLive(section, now))
                .toList();
    }

    public List<OfferSection> publishDraftSections() {
        List<OfferSection> normalizedDraft = normalizeSections(draftRepository.findAll());
        draftRepository.replaceAll(normalizedDraft);
        publishedRepository.replaceAll(normalizedDraft);
        return normalizedDraft;
    }

    public OfferPageSettings getDraftSettings() {
        return normalizeSettings(settingsDraftRepository.read());
    }

    public OfferPageSettings saveDraftSettings(OfferPageSettings settings) {
        OfferPageSettings normalized = normalizeSettings(settings);
        settingsDraftRepository.write(normalized);
        return normalized;
    }

    public OfferPageSettings getPublishedSettings() {
        return normalizeSettings(settingsPublishedRepository.read());
    }

    public OfferPageSettings publishDraftSettings() {
        OfferPageSettings normalizedDraft = normalizeSettings(settingsDraftRepository.read());
        settingsDraftRepository.write(normalizedDraft);
        settingsPublishedRepository.write(normalizedDraft);
        return normalizedDraft;
    }

    public List<Resources> getLinkedResources(Long offerId) {
        OfferSection section = findPublishedSection(offerId);
        if (section == null) {
            return List.of();
        }

        Set<Long> linkedIds = new HashSet<>(section.getLinkedResourceIds());
        return resourcesRepository.findAll().stream()
                .filter(resource -> linkedIds.contains(resource.getId()))
                .filter(Resources::isActive)
                .sorted(Comparator.comparing(Resources::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void validateOfferAttribution(Long offerId, Long resourceId) {
        if (offerId == null) {
            return;
        }

        OfferSection section = findPublishedSection(offerId);
        if (section == null) {
            throw new BadRequestException("Offer not found");
        }
        if (!isOfferLive(section, LocalDateTime.now())) {
            throw new BadRequestException("Offer is not currently active");
        }
        if (!section.getLinkedResourceIds().contains(resourceId)) {
            throw new BadRequestException("Offer does not apply to the selected car");
        }
    }

    public List<OfferAnalyticsResponse> getOfferAnalytics(String dateFrom, String dateTo) {
        LocalDateTime from = parseAnalyticsStart(dateFrom);
        LocalDateTime to = parseAnalyticsEnd(dateTo);
        LocalDateTime now = LocalDateTime.now();
        Map<Long, OfferSection> sectionById = getPublishedSections().stream()
                .collect(Collectors.toMap(OfferSection::getId, Function.identity()));

        return sectionById.values().stream()
                .sorted(Comparator.comparing(OfferSection::getSortOrder))
                .map(section -> {
                    List<Booking> attributedBookings = bookingRepository.findAll().stream()
                            .filter(booking -> Objects.equals(booking.getOfferId(), section.getId()))
                            .filter(booking -> booking.getBookingTime() != null)
                            .filter(booking -> from == null || !booking.getBookingTime().isBefore(from))
                            .filter(booking -> to == null || booking.getBookingTime().isBefore(to))
                            .filter(this::isCountedForAnalytics)
                            .toList();
                    long revenueCents = attributedBookings.stream()
                            .map(Booking::getPayableAmountCents)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .sum();
                    return new OfferAnalyticsResponse(
                            section.getId(),
                            section.getTitle(),
                            Boolean.TRUE.equals(section.getEnabled()),
                            isOfferLive(section, now),
                            section.getStartDateTime(),
                            section.getEndDateTime(),
                            section.getLinkedResourceIds().size(),
                            attributedBookings.size(),
                            revenueCents
                    );
                })
                .toList();
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
            copy.setEnabled(input.getEnabled() == null ? Boolean.TRUE : input.getEnabled());
            copy.setStartDateTime(normalizeScheduleBoundary(input.getStartDateTime()));
            copy.setEndDateTime(normalizeScheduleBoundary(input.getEndDateTime()));
            if (copy.getStartDateTime() != null && copy.getEndDateTime() != null
                    && !copy.getStartDateTime().isBefore(copy.getEndDateTime())) {
                copy.setEndDateTime(copy.getStartDateTime().plusDays(1));
            }
            copy.setCtaLabel(normalizeText(input.getCtaLabel()).isEmpty() ? "Book now" : normalizeText(input.getCtaLabel()));
            copy.setLinkedResourceIds(normalizeLinkedResourceIds(input.getLinkedResourceIds()));

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

    private OfferPageSettings normalizeSettings(OfferPageSettings settings) {
        OfferPageSettings normalized = new OfferPageSettings();
        if (settings == null) {
            normalized.setHeroBackgroundImageUrl("");
            return normalized;
        }
        normalized.setHeroBackgroundImageUrl(normalizeText(settings.getHeroBackgroundImageUrl()));
        return normalized;
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

    private List<Long> normalizeLinkedResourceIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> value > 0)
                .distinct()
                .toList();
    }

    private LocalDateTime normalizeScheduleBoundary(LocalDateTime value) {
        return value;
    }

    private OfferSection findPublishedSection(Long offerId) {
        return getPublishedSections().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), offerId))
                .findFirst()
                .orElse(null);
    }

    private boolean isOfferLive(OfferSection section, LocalDateTime now) {
        if (!Boolean.TRUE.equals(section.getEnabled())) {
            return false;
        }
        if (section.getStartDateTime() != null && now.isBefore(section.getStartDateTime())) {
            return false;
        }
        if (section.getEndDateTime() != null && !now.isBefore(section.getEndDateTime())) {
            return false;
        }
        return true;
    }

    private boolean isCountedForAnalytics(Booking booking) {
        if (booking.getPaymentStatus() == null) {
            return false;
        }
        return booking.getPaymentStatus() == PaymentStatus.SUCCEEDED
                || booking.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED
                || booking.getPaymentStatus() == PaymentStatus.REFUNDED;
    }

    private LocalDateTime parseAnalyticsStart(String value) {
        String normalized = normalizeQueryValue(value);
        if (normalized == null) {
            return null;
        }
        return LocalDate.parse(normalized).atStartOfDay();
    }

    private LocalDateTime parseAnalyticsEnd(String value) {
        String normalized = normalizeQueryValue(value);
        if (normalized == null) {
            return null;
        }
        return LocalDate.parse(normalized).plusDays(1).atStartOfDay();
    }

    private String normalizeQueryValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
