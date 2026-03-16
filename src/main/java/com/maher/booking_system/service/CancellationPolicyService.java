package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.model.CancellationPolicy;
import com.maher.booking_system.model.CancellationPolicyRule;
import com.maher.booking_system.repository.CancellationPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CancellationPolicyService {
    private static final Logger log = LoggerFactory.getLogger(CancellationPolicyService.class);

    private final CancellationPolicyRepository cancellationPolicyRepository;

    public CancellationPolicyService(CancellationPolicyRepository cancellationPolicyRepository) {
        this.cancellationPolicyRepository = cancellationPolicyRepository;
    }

    public synchronized CancellationPolicy getCurrentPolicy() {
        try {
            return cancellationPolicyRepository.findAll().stream()
                    .findFirst()
                    .map(this::normalizePolicy)
                    .orElseGet(this::createDefaultPolicy);
        } catch (BadRequestException | UncheckedIOException ex) {
            log.warn(
                    "Falling back to default cancellation policy because persisted policy is unavailable or invalid: {}",
                    ex.getMessage()
            );
            return createDefaultPolicy();
        }
    }

    public synchronized CancellationPolicy updatePolicy(CancellationPolicy policy) {
        CancellationPolicy normalized = normalizePolicy(policy);
        normalized.setId(1L);
        normalized.setUpdatedAt(LocalDateTime.now());
        return cancellationPolicyRepository.save(normalized);
    }

    public RefundDecision calculateRefund(CancellationPolicy policy, LocalDateTime pickupTime, long paidAmountCents) {
        if (pickupTime == null) {
            return new RefundDecision(0, 0L, "Pickup time is missing");
        }

        CancellationPolicy effectivePolicy = normalizePolicy(policy);
        long hoursBeforePickup = Math.max(0L, ChronoUnit.HOURS.between(LocalDateTime.now(), pickupTime));

        for (CancellationPolicyRule rule : effectivePolicy.getRules()) {
            int threshold = rule.getMinimumHoursBeforePickup() == null ? 0 : Math.max(0, rule.getMinimumHoursBeforePickup());
            if (hoursBeforePickup >= threshold) {
                int percentage = clampPercentage(rule.getRefundPercentage());
                long refundedAmount = Math.round(paidAmountCents * (percentage / 100.0d));
                String reason = rule.getLabel() == null || rule.getLabel().isBlank()
                        ? "Cancellation policy refund"
                        : rule.getLabel().trim();
                return new RefundDecision(percentage, refundedAmount, reason);
            }
        }

        return new RefundDecision(0, 0L, "Cancellation policy refund");
    }

    private CancellationPolicy createDefaultPolicy() {
        CancellationPolicy policy = new CancellationPolicy();
        policy.setId(1L);
        policy.setVersion("default-v1");
        policy.setAgreementText("I agree to the cancellation policy shown before payment.");
        policy.setRules(List.of(
                createRule(48, 100, "Cancel at least 48 hours before pickup"),
                createRule(24, 50, "Cancel 24 to 47 hours before pickup"),
                createRule(6, 25, "Cancel 6 to 23 hours before pickup"),
                createRule(0, 0, "Cancel less than 6 hours before pickup")
        ));
        policy.setUpdatedAt(LocalDateTime.now());
        return cancellationPolicyRepository.save(policy);
    }

    private CancellationPolicy normalizePolicy(CancellationPolicy policy) {
        if (policy == null) {
            throw new BadRequestException("Cancellation policy payload is required");
        }

        String version = policy.getVersion() == null ? "" : policy.getVersion().trim();
        if (version.isBlank()) {
            throw new BadRequestException("Cancellation policy version is required");
        }

        List<CancellationPolicyRule> rules = new ArrayList<>();
        for (CancellationPolicyRule rule : policy.getRules()) {
            if (rule == null) {
                continue;
            }
            CancellationPolicyRule normalizedRule = new CancellationPolicyRule();
            normalizedRule.setMinimumHoursBeforePickup(rule.getMinimumHoursBeforePickup() == null ? 0 : Math.max(0, rule.getMinimumHoursBeforePickup()));
            normalizedRule.setRefundPercentage(clampPercentage(rule.getRefundPercentage()));
            normalizedRule.setLabel(rule.getLabel() == null ? "" : rule.getLabel().trim());
            rules.add(normalizedRule);
        }

        if (rules.isEmpty()) {
            throw new BadRequestException("At least one cancellation policy rule is required");
        }

        rules.sort(Comparator.comparing(CancellationPolicyRule::getMinimumHoursBeforePickup).reversed());

        CancellationPolicy normalized = new CancellationPolicy();
        normalized.setId(policy.getId());
        normalized.setVersion(version);
        normalized.setAgreementText(policy.getAgreementText() == null ? "" : policy.getAgreementText().trim());
        normalized.setRules(rules);
        normalized.setUpdatedAt(policy.getUpdatedAt());
        return normalized;
    }

    private CancellationPolicyRule createRule(int minimumHoursBeforePickup, int refundPercentage, String label) {
        CancellationPolicyRule rule = new CancellationPolicyRule();
        rule.setMinimumHoursBeforePickup(minimumHoursBeforePickup);
        rule.setRefundPercentage(refundPercentage);
        rule.setLabel(label);
        return rule;
    }

    private int clampPercentage(Integer value) {
        if (value == null) {
            throw new BadRequestException("refundPercentage is required");
        }
        return Math.max(0, Math.min(100, value));
    }

    public record RefundDecision(int refundPercentage, long refundedAmountCents, String reason) {
    }
}
