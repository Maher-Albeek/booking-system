package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.PromoCode;
import com.maher.booking_system.model.enums.DiscountType;
import com.maher.booking_system.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class PromoCodeService {
    private final PromoCodeRepository promoCodeRepository;

    public PromoCodeService(PromoCodeRepository promoCodeRepository) {
        this.promoCodeRepository = promoCodeRepository;
    }

    public List<PromoCode> getAll() {
        return promoCodeRepository.findAll();
    }

    public PromoCode create(PromoCode promoCode) {
        PromoCode normalized = normalize(promoCode, null);
        return promoCodeRepository.save(normalized);
    }

    public PromoCode update(Long id, PromoCode promoCode) {
        PromoCode existing = promoCodeRepository.findById(id).orElseThrow(() -> new NotFoundException("Promo code not found"));
        PromoCode normalized = normalize(promoCode, existing);
        normalized.setId(id);
        return promoCodeRepository.save(normalized);
    }

    public void delete(Long id) {
        promoCodeRepository.deleteById(id);
    }

    public Preview previewDiscount(String code, Long amountCents) {
        if (amountCents == null || amountCents < 0) {
            throw new BadRequestException("amountCents must be >= 0");
        }
        PromoCode promoCode = promoCodeRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Promo code not found"));
        validateUsable(promoCode);
        long discount = calculateDiscount(amountCents, promoCode);
        return new Preview(code.toUpperCase(Locale.ROOT), amountCents, discount, Math.max(0, amountCents - discount));
    }

    public void consume(String code) {
        PromoCode promoCode = promoCodeRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Promo code not found"));
        validateUsable(promoCode);
        int used = promoCode.getUsedCount() == null ? 0 : promoCode.getUsedCount();
        promoCode.setUsedCount(used + 1);
        promoCodeRepository.save(promoCode);
    }

    private void validateUsable(PromoCode promoCode) {
        if (!promoCode.isEnabled()) {
            throw new BadRequestException("Promo code is disabled");
        }
        if (promoCode.getExpiryDateTime() != null && promoCode.getExpiryDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Promo code is expired");
        }
        int used = promoCode.getUsedCount() == null ? 0 : promoCode.getUsedCount();
        if (promoCode.getUsageLimit() != null && used >= promoCode.getUsageLimit()) {
            throw new BadRequestException("Promo code usage limit reached");
        }
    }

    private long calculateDiscount(long amountCents, PromoCode promoCode) {
        if (promoCode.getDiscountType() == DiscountType.PERCENT) {
            return Math.min(amountCents, Math.round(amountCents * promoCode.getDiscountValue() / 100.0d));
        }
        return Math.min(amountCents, Math.round(promoCode.getDiscountValue() * 100.0d));
    }

    private PromoCode normalize(PromoCode input, PromoCode fallback) {
        PromoCode safe = Objects.requireNonNull(input, "promoCode must not be null");
        PromoCode result = new PromoCode();
        String normalizedCode = safe.getCode() == null ? "" : safe.getCode().trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.isBlank()) {
            throw new BadRequestException("code is required");
        }
        result.setCode(normalizedCode);
        result.setDiscountType(safe.getDiscountType() == null ? DiscountType.PERCENT : safe.getDiscountType());
        if (safe.getDiscountValue() == null || safe.getDiscountValue() <= 0) {
            throw new BadRequestException("discountValue must be > 0");
        }
        result.setDiscountValue(safe.getDiscountValue());
        result.setExpiryDateTime(safe.getExpiryDateTime());
        result.setUsageLimit(safe.getUsageLimit());
        result.setUsedCount(fallback == null ? 0 : (fallback.getUsedCount() == null ? 0 : fallback.getUsedCount()));
        result.setEnabled(safe.isEnabled());
        return result;
    }

    public record Preview(String code, Long amountCents, Long discountCents, Long totalAfterDiscountCents) {
    }
}
