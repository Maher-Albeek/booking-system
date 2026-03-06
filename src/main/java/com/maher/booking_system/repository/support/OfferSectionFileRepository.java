package com.maher.booking_system.repository.support;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.OfferSection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class OfferSectionFileRepository {

    private final ObjectMapper objectMapper;
    private final Path filePath;
    private final JavaType listType;
    private final Object monitor = new Object();

    protected OfferSectionFileRepository(ObjectMapper objectMapper, Path storageDirectory, String fileName) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.filePath = Objects.requireNonNull(storageDirectory, "storageDirectory must not be null").resolve(fileName);
        this.listType = objectMapper.getTypeFactory().constructCollectionType(List.class, OfferSection.class);
    }

    public List<OfferSection> findAll() {
        synchronized (monitor) {
            return new ArrayList<>(readAllUnsafe());
        }
    }

    public void replaceAll(List<OfferSection> sections) {
        synchronized (monitor) {
            writeAllUnsafe(new ArrayList<>(sections));
        }
    }

    private List<OfferSection> readAllUnsafe() {
        ensureStorageExists();
        try {
            return objectMapper.readValue(filePath.toFile(), listType);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read offer sections from " + filePath, ex);
        }
    }

    private void writeAllUnsafe(List<OfferSection> sections) {
        ensureStorageExists();
        Path tempPath = filePath.resolveSibling(filePath.getFileName() + ".tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempPath.toFile(), sections);
            try {
                Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write offer sections to " + filePath, ex);
        }
    }

    private void ensureStorageExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                Files.writeString(filePath, "[]");
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to initialize offer section storage for " + filePath, ex);
        }
    }
}
