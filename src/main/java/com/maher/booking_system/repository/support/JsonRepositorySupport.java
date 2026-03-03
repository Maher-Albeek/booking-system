package com.maher.booking_system.repository.support;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class JsonRepositorySupport<T> {

    private final ObjectMapper objectMapper;
    private final Path filePath;
    private final JavaType listType;
    private final Function<T, Long> idGetter;
    private final BiConsumer<T, Long> idSetter;
    private final Object monitor = new Object();

    protected JsonRepositorySupport(
            ObjectMapper objectMapper,
            Path storageDirectory,
            String fileName,
            Class<T> entityType,
            Function<T, Long> idGetter,
            BiConsumer<T, Long> idSetter
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.filePath = Objects.requireNonNull(storageDirectory, "storageDirectory must not be null").resolve(fileName);
        this.listType = objectMapper.getTypeFactory().constructCollectionType(List.class, entityType);
        this.idGetter = Objects.requireNonNull(idGetter, "idGetter must not be null");
        this.idSetter = Objects.requireNonNull(idSetter, "idSetter must not be null");
    }

    public List<T> findAll() {
        synchronized (monitor) {
            return new ArrayList<>(readAllUnsafe());
        }
    }

    public Optional<T> findById(Long id) {
        synchronized (monitor) {
            return readAllUnsafe().stream()
                    .filter(entity -> Objects.equals(idGetter.apply(entity), id))
                    .findFirst();
        }
    }

    public T save(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        synchronized (monitor) {
            List<T> entities = readAllUnsafe();
            Long id = idGetter.apply(entity);

            if (id == null) {
                id = nextId(entities);
                idSetter.accept(entity, id);
                entities.add(entity);
            } else {
                boolean updated = false;
                for (int i = 0; i < entities.size(); i++) {
                    if (Objects.equals(idGetter.apply(entities.get(i)), id)) {
                        entities.set(i, entity);
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    entities.add(entity);
                }
            }

            writeAllUnsafe(entities);
            return entity;
        }
    }

    public void deleteById(Long id) {
        synchronized (monitor) {
            List<T> entities = readAllUnsafe();
            boolean removed = entities.removeIf(entity -> Objects.equals(idGetter.apply(entity), id));
            if (removed) {
                writeAllUnsafe(entities);
            }
        }
    }

    private Long nextId(List<T> entities) {
        return entities.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }

    private List<T> readAllUnsafe() {
        ensureStorageExists();
        try {
            return objectMapper.readValue(filePath.toFile(), listType);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read data from " + filePath, ex);
        }
    }

    private void writeAllUnsafe(List<T> entities) {
        ensureStorageExists();
        Path tempPath = filePath.resolveSibling(filePath.getFileName() + ".tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempPath.toFile(), entities);
            try {
                Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write data to " + filePath, ex);
        }
    }

    private void ensureStorageExists() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                Files.writeString(filePath, "[]");
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to initialize storage for " + filePath, ex);
        }
    }
}
