package com.maher.booking_system.controller;

import com.maher.booking_system.model.LegalContent;
import com.maher.booking_system.service.LegalPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private final LegalPageService legalPageService;

    public LegalController(LegalPageService legalPageService) {
        this.legalPageService = legalPageService;
    }

    @GetMapping("/draft")
    public LegalContent getDraftContent() {
        return legalPageService.getDraftContent();
    }

    @PutMapping("/draft")
    public LegalContent saveDraftContent(@RequestBody(required = false) LegalContent legalContent) {
        return legalPageService.saveDraftContent(legalContent);
    }

    @GetMapping("/published")
    public LegalContent getPublishedContent() {
        return legalPageService.getPublishedContent();
    }

    @PostMapping("/publish")
    public LegalContent publishDraftContent() {
        return legalPageService.publishDraftContent();
    }
}
