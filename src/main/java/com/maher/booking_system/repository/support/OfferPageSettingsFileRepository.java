package com.maher.booking_system.repository.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.OfferPageSettings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public abstract class OfferPageSettingsFileRepository {

    private final ObjectMapper objectMapper;
    private final Path filePath;
    private final Object monitor = new Object();

    protected OfferPageSettingsFileRepository(ObjectMapper objectMapper, Path storageDirectory, String fileName) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.filePath = Objects.requireNonNull(storageDirectory, "storageDirectory must not be null").resolve(fileName);
    }

    public OfferPageSettings read() {
        synchronized (monitor) {
            ensureStorageExists();
            try {
                return objectMapper.readValue(filePath.toFile(), OfferPageSettings.class);
            } catch (IOException ex) {
                OfferPageSettings fallback = new OfferPageSettings();
                fallback.setHeroBackgroundImageUrl("");
                write(fallback);
                return fallback;
            }
        }
    }

    public void write(OfferPageSettings settings) {
        synchronized (monitor) {
            ensureStorageExists();
            Path tempPath = filePath.resolveSibling(filePath.getFileName() + ".tmp");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempPath.toFile(), settings);
                try {
                    Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to write offer page settings to " + filePath, ex);
            }
        }
    }

    private void ensureStorageExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), new OfferPageSettings());
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to initialize offer page settings storage for " + filePath, ex);
        }
    }
}
