package com.maher.booking_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maher.booking_system.model.CancellationPolicy;
import com.maher.booking_system.model.CancellationPolicyRule;
import com.maher.booking_system.repository.CancellationPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationPolicyServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createsDefaultPolicyWhenStorageIsEmpty() {
        CancellationPolicyService service = createService();

        CancellationPolicy policy = service.getCurrentPolicy();

        assertThat(policy.getVersion()).isEqualTo("default-v1");
        assertThat(policy.getRules()).hasSize(4);
        assertThat(policy.getRules().getFirst().getRefundPercentage()).isEqualTo(100);
    }

    @Test
    void calculatesRefundFromConfiguredRules() {
        CancellationPolicyService service = createService();

        CancellationPolicy policy = new CancellationPolicy();
        policy.setVersion("custom-v1");
        policy.setAgreementText("agree");
        policy.setRules(List.of(
                rule(48, 100, "48h"),
                rule(24, 50, "24h"),
                rule(0, 0, "late")
        ));
        service.updatePolicy(policy);

        CancellationPolicyService.RefundDecision fullRefund = service.calculateRefund(
                service.getCurrentPolicy(),
                LocalDateTime.now().plusHours(60),
                20000L
        );
        CancellationPolicyService.RefundDecision partialRefund = service.calculateRefund(
                service.getCurrentPolicy(),
                LocalDateTime.now().plusHours(30),
                20000L
        );
        CancellationPolicyService.RefundDecision noRefund = service.calculateRefund(
                service.getCurrentPolicy(),
                LocalDateTime.now().plusHours(2),
                20000L
        );

        assertThat(fullRefund.refundPercentage()).isEqualTo(100);
        assertThat(fullRefund.refundedAmountCents()).isEqualTo(20000L);
        assertThat(partialRefund.refundPercentage()).isEqualTo(50);
        assertThat(partialRefund.refundedAmountCents()).isEqualTo(10000L);
        assertThat(noRefund.refundPercentage()).isEqualTo(0);
        assertThat(noRefund.refundedAmountCents()).isZero();
    }

    private CancellationPolicyService createService() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        CancellationPolicyRepository repository = new CancellationPolicyRepository(objectMapper, tempDir.toString());
        return new CancellationPolicyService(repository);
    }

    private CancellationPolicyRule rule(int minimumHoursBeforePickup, int refundPercentage, String label) {
        CancellationPolicyRule rule = new CancellationPolicyRule();
        rule.setMinimumHoursBeforePickup(minimumHoursBeforePickup);
        rule.setRefundPercentage(refundPercentage);
        rule.setLabel(label);
        return rule;
    }
}
