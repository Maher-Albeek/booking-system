package com.maher.booking_system.service;

import com.maher.booking_system.model.LegalContent;
import com.maher.booking_system.model.LegalField;
import com.maher.booking_system.repository.LegalDraftRepository;
import com.maher.booking_system.repository.LegalPublishedRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LegalPageService {

    private final LegalDraftRepository draftRepository;
    private final LegalPublishedRepository publishedRepository;

    public LegalPageService(LegalDraftRepository draftRepository, LegalPublishedRepository publishedRepository) {
        this.draftRepository = draftRepository;
        this.publishedRepository = publishedRepository;
    }

    public LegalContent getDraftContent() {
        return withDefaults(normalize(draftRepository.read()));
    }

    public LegalContent saveDraftContent(LegalContent legalContent) {
        LegalContent normalized = withDefaults(normalize(legalContent));
        draftRepository.write(normalized);
        return normalized;
    }

    public LegalContent getPublishedContent() {
        return withDefaults(normalize(publishedRepository.read()));
    }

    public LegalContent publishDraftContent() {
        LegalContent normalizedDraft = withDefaults(normalize(draftRepository.read()));
        draftRepository.write(normalizedDraft);
        publishedRepository.write(normalizedDraft);
        return normalizedDraft;
    }

    private LegalContent withDefaults(LegalContent content) {
        if (!content.getImpressumFields().isEmpty() && !content.getDatenschutzFields().isEmpty()) {
            return content;
        }

        if (content.getImpressumFields().isEmpty()) {
            content.setImpressumFields(defaultImpressumFields());
        }

        if (content.getDatenschutzFields().isEmpty()) {
            content.setDatenschutzFields(defaultDatenschutzFields());
        }

        return content;
    }

    private LegalContent normalize(LegalContent content) {
        LegalContent source = content == null ? new LegalContent() : content;
        LegalContent normalized = new LegalContent();
        normalized.setImpressumFields(normalizeFields(source.getImpressumFields()));
        normalized.setDatenschutzFields(normalizeFields(source.getDatenschutzFields()));
        return normalized;
    }

    private List<LegalField> normalizeFields(List<LegalField> fields) {
        List<LegalField> safeFields = fields == null ? List.of() : fields;
        List<LegalField> normalized = new ArrayList<>();

        for (LegalField field : safeFields) {
            if (field == null) {
                continue;
            }

            String label = normalizeText(field.getLabel());
            String value = normalizeText(field.getValue());
            if (label.isEmpty() && value.isEmpty()) {
                continue;
            }

            LegalField copy = new LegalField();
            copy.setLabel(label);
            copy.setValue(value);
            normalized.add(copy);
        }

        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private List<LegalField> defaultImpressumFields() {
        return List.of(
                field("Company", "Booking System GmbH"),
                field("Address", "Musterstrasse 10, 10115 Berlin"),
                field("Email", "legal@example.com"),
                field("Phone", "+49 30 000000")
        );
    }

    private List<LegalField> defaultDatenschutzFields() {
        return List.of(
                field("Data controller", "Booking System GmbH"),
                field("Purpose", "Account handling, car bookings, and payment preference storage."),
                field("Stored fields", "Name, email, address, bookings, and selected payment methods."),
                field("Requests", "Contact legal@example.com for access, correction, or deletion.")
        );
    }

    private LegalField field(String label, String value) {
        LegalField legalField = new LegalField();
        legalField.setLabel(label);
        legalField.setValue(value);
        return legalField;
    }
}
