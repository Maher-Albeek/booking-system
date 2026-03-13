package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.OfferCampaign;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.model.enums.DiscountType;
import com.maher.booking_system.repository.OfferCampaignRepository;
import com.maher.booking_system.repository.ResourcesRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class OfferCampaignService {
    private final OfferCampaignRepository offerCampaignRepository;
    private final ResourcesRepository resourcesRepository;

    public OfferCampaignService(OfferCampaignRepository offerCampaignRepository, ResourcesRepository resourcesRepository) {
        this.offerCampaignRepository = offerCampaignRepository;
        this.resourcesRepository = resourcesRepository;
    }

    public List<OfferCampaign> getAll() {
        return offerCampaignRepository.findAll();
    }

    public List<OfferCampaign> getActive() {
        LocalDateTime now = LocalDateTime.now();
        return offerCampaignRepository.findAll().stream()
                .filter(OfferCampaign::isEnabled)
                .filter(o -> o.getStartDateTime() == null || !o.getStartDateTime().isAfter(now))
                .filter(o -> o.getEndDateTime() == null || !o.getEndDateTime().isBefore(now))
                .toList();
    }

    public OfferCampaign create(OfferCampaign campaign) {
        OfferCampaign normalized = normalize(campaign, null);
        normalized.setBookingsCount(0L);
        normalized.setRevenueCents(0L);
        return offerCampaignRepository.save(normalized);
    }

    public OfferCampaign update(Long id, OfferCampaign campaign) {
        OfferCampaign existing = offerCampaignRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Offer campaign not found"));
        OfferCampaign normalized = normalize(campaign, existing);
        normalized.setId(id);
        return offerCampaignRepository.save(normalized);
    }

    public void delete(Long id) {
        offerCampaignRepository.deleteById(id);
    }

    public void recordAttributedBooking(Long offerId, Long bookingRevenueCents) {
        offerCampaignRepository.findById(offerId).ifPresent(offer -> {
            offer.setBookingsCount((offer.getBookingsCount() == null ? 0L : offer.getBookingsCount()) + 1L);
            offer.setRevenueCents((offer.getRevenueCents() == null ? 0L : offer.getRevenueCents())
                    + (bookingRevenueCents == null ? 0L : bookingRevenueCents));
            offerCampaignRepository.save(offer);
        });
    }

    private OfferCampaign normalize(OfferCampaign input, OfferCampaign fallback) {
        OfferCampaign safe = Objects.requireNonNull(input, "campaign must not be null");
        OfferCampaign result = new OfferCampaign();
        result.setTitle(normalizeRequired(safe.getTitle(), "title"));
        result.setDescription(normalizeOptional(safe.getDescription()));
        result.setImageUrl(normalizeOptional(safe.getImageUrl()));
        result.setTitleColor(normalizeOptional(safe.getTitleColor()));
        result.setDescriptionColor(normalizeOptional(safe.getDescriptionColor()));
        result.setTitleFontSizePx(safe.getTitleFontSizePx());
        result.setDescriptionFontSizePx(safe.getDescriptionFontSizePx());
        result.setSectionLayout(normalizeOptional(safe.getSectionLayout()));
        result.setStartDateTime(safe.getStartDateTime());
        result.setEndDateTime(safe.getEndDateTime());
        result.setEnabled(safe.isEnabled());
        result.setDiscountType(safe.getDiscountType() == null ? DiscountType.PERCENT : safe.getDiscountType());
        if (safe.getDiscountValue() == null || safe.getDiscountValue() <= 0) {
            throw new BadRequestException("discountValue must be greater than 0");
        }
        result.setDiscountValue(safe.getDiscountValue());
        List<Long> validCarIds = safe.getEligibleCarIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(this::carExists)
                .toList();
        result.setEligibleCarIds(validCarIds);
        if (result.getStartDateTime() != null && result.getEndDateTime() != null && !result.getStartDateTime().isBefore(result.getEndDateTime())) {
            throw new BadRequestException("startDateTime must be before endDateTime");
        }
        result.setBookingsCount(fallback == null ? 0L : fallback.getBookingsCount());
        result.setRevenueCents(fallback == null ? 0L : fallback.getRevenueCents());
        return result;
    }

    private boolean carExists(Long carId) {
        Resources resource = resourcesRepository.findById(carId).orElse(null);
        return resource != null && "Car".equalsIgnoreCase(resource.getType());
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
