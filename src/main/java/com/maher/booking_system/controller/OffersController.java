package com.maher.booking_system.controller;

import com.maher.booking_system.model.OfferSection;
import com.maher.booking_system.model.OfferPageSettings;
import com.maher.booking_system.service.OfferPageService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OffersController {

    private final OfferPageService offerPageService;

    public OffersController(OfferPageService offerPageService) {
        this.offerPageService = offerPageService;
    }

    @GetMapping("/draft")
    public List<OfferSection> getDraftSections() {
        return offerPageService.getDraftSections();
    }

    @PutMapping("/draft")
    public List<OfferSection> saveDraftSections(@RequestBody(required = false) List<OfferSection> sections) {
        return offerPageService.saveDraftSections(sections);
    }

    @GetMapping("/published")
    public List<OfferSection> getPublishedSections() {
        return offerPageService.getPublishedSections();
    }

    @PostMapping("/publish")
    public @NonNull List<OfferSection> publishDraftSections() {
        return offerPageService.publishDraftSections();
    }

    @GetMapping("/settings/draft")
    public OfferPageSettings getDraftSettings() {
        return offerPageService.getDraftSettings();
    }

    @PutMapping("/settings/draft")
    public OfferPageSettings saveDraftSettings(@RequestBody(required = false) OfferPageSettings settings) {
        return offerPageService.saveDraftSettings(settings);
    }

    @GetMapping("/settings/published")
    public OfferPageSettings getPublishedSettings() {
        return offerPageService.getPublishedSettings();
    }

    @PostMapping("/settings/publish")
    public OfferPageSettings publishDraftSettings() {
        return offerPageService.publishDraftSettings();
    }
}
