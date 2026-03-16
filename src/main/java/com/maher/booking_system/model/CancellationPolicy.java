package com.maher.booking_system.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CancellationPolicy {
    private Long id;
    private String version;
    private String agreementText;
    private List<CancellationPolicyRule> rules = new ArrayList<>();
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAgreementText() { return agreementText; }
    public void setAgreementText(String agreementText) { this.agreementText = agreementText; }

    public List<CancellationPolicyRule> getRules() {
        return rules == null ? List.of() : List.copyOf(rules);
    }

    public void setRules(List<CancellationPolicyRule> rules) {
        this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
