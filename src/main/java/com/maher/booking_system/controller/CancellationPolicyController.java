package com.maher.booking_system.controller;

import com.maher.booking_system.model.CancellationPolicy;
import com.maher.booking_system.service.CancellationPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/cancellation-policy")
public class CancellationPolicyController {
    private final CancellationPolicyService cancellationPolicyService;

    public CancellationPolicyController(CancellationPolicyService cancellationPolicyService) {
        this.cancellationPolicyService = cancellationPolicyService;
    }

    @GetMapping
    public CancellationPolicy getCurrentPolicy() {
        return cancellationPolicyService.getCurrentPolicy();
    }

    @PutMapping
    public CancellationPolicy updatePolicy(@RequestBody CancellationPolicy policy) {
        return cancellationPolicyService.updatePolicy(policy);
    }
}
