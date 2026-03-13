package com.maher.booking_system.controller;

import com.maher.booking_system.model.OfferCampaign;
import com.maher.booking_system.model.PromoCode;
import com.maher.booking_system.service.OfferCampaignService;
import com.maher.booking_system.service.PromoCodeService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commercial")
public class CommercialController {
    private final OfferCampaignService offerCampaignService;
    private final PromoCodeService promoCodeService;

    public CommercialController(OfferCampaignService offerCampaignService, PromoCodeService promoCodeService) {
        this.offerCampaignService = offerCampaignService;
        this.promoCodeService = promoCodeService;
    }

    @GetMapping("/offers")
    public List<OfferCampaign> getActiveOffers() {
        return offerCampaignService.getActive();
    }

    @GetMapping("/admin/offers")
    public List<OfferCampaign> getAllOffers() {
        return offerCampaignService.getAll();
    }

    @PostMapping("/admin/offers")
    public OfferCampaign createOffer(@RequestBody OfferCampaign campaign) {
        return offerCampaignService.create(campaign);
    }

    @PutMapping("/admin/offers/{id}")
    public OfferCampaign updateOffer(@PathVariable Long id, @RequestBody OfferCampaign campaign) {
        return offerCampaignService.update(id, campaign);
    }

    @DeleteMapping("/admin/offers/{id}")
    public void deleteOffer(@PathVariable Long id) {
        offerCampaignService.delete(id);
    }

    @GetMapping("/admin/promo-codes")
    public List<PromoCode> getPromoCodes() {
        return promoCodeService.getAll();
    }

    @PostMapping("/admin/promo-codes")
    public PromoCode createPromoCode(@RequestBody PromoCode promoCode) {
        return promoCodeService.create(promoCode);
    }

    @PutMapping("/admin/promo-codes/{id}")
    public PromoCode updatePromoCode(@PathVariable Long id, @RequestBody PromoCode promoCode) {
        return promoCodeService.update(id, promoCode);
    }

    @DeleteMapping("/admin/promo-codes/{id}")
    public void deletePromoCode(@PathVariable Long id) {
        promoCodeService.delete(id);
    }

    @PostMapping("/promo-codes/preview")
    public PromoCodeService.Preview previewPromo(@RequestBody PromoPreviewRequest request) {
        return promoCodeService.previewDiscount(request.code(), request.amountCents());
    }

    public record PromoPreviewRequest(
            @NotBlank String code,
            @NotNull Long amountCents
    ) {
    }
}
