package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.OfferCampaign;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
public class OfferCampaignRepository extends JsonRepositorySupport<OfferCampaign> {
    public OfferCampaignRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "offer-campaigns.json", OfferCampaign.class, OfferCampaign::getId, OfferCampaign::setId);
    }
}
