package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PaymentMethodCatalog {

    private static final Map<String, String> METHOD_BY_KEY = new LinkedHashMap<>();

    static {
        register("paypal", "PayPal");
        register("master card", "Master Card");
        register("visa", "Visa");
        register("apple pay", "Apple Pay");
        register("google pay", "Google Pay");
    }

    private PaymentMethodCatalog() {
    }

    public static List<String> supportedMethods() {
        return List.copyOf(METHOD_BY_KEY.values());
    }

    public static String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        return normalized;
    }

    public static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalizedValues = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeOptional(value);
            if (normalized != null) {
                normalizedValues.add(normalized);
            }
        }
        return List.copyOf(normalizedValues);
    }

    public static Map<String, String> normalizeDetails(Map<String, String> details, List<String> selectedMethods) {
        if (details == null || details.isEmpty() || selectedMethods == null || selectedMethods.isEmpty()) {
            return Map.of();
        }

        Set<String> normalizedMethods = Set.copyOf(selectedMethods);
        LinkedHashMap<String, String> normalizedDetails = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : details.entrySet()) {
            String normalizedMethod = normalizeOptional(entry.getKey());
            if (normalizedMethod == null || !normalizedMethods.contains(normalizedMethod)) {
                continue;
            }

            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!value.isEmpty()) {
                normalizedDetails.put(normalizedMethod, value);
            }
        }

        return Map.copyOf(normalizedDetails);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String key = value.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return null;
        }

        String normalized = METHOD_BY_KEY.get(key);
        if (normalized == null) {
            throw new BadRequestException("Unsupported payment method: " + value);
        }
        return normalized;
    }

    private static void register(String key, String label) {
        METHOD_BY_KEY.put(key, label);
    }
}
