package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResourcePhotoStorageService {

    private static final Pattern DATA_URL_PATTERN = Pattern.compile("^data:([\\w.+-]+/[\\w.+-]+);base64,(.+)$");

    private final Path photoRootDirectory;

    public ResourcePhotoStorageService(
            @Value("${app.photo-storage.directory:${app.storage.directory}/car-photos}") String photoStorageDirectory
    ) {
        this.photoRootDirectory = Path.of(photoStorageDirectory);
    }

    public List<String> resolvePhotoUrls(Long resourceId, List<String> submittedPhotoUrls, List<String> existingPhotoUrls) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");

        LinkedHashSet<String> resolvedPhotoUrls = new LinkedHashSet<>();
        for (String submittedPhotoUrl : submittedPhotoUrls == null ? List.<String>of() : submittedPhotoUrls) {
            if (submittedPhotoUrl == null || submittedPhotoUrl.isBlank()) {
                continue;
            }

            String trimmedPhotoUrl = submittedPhotoUrl.trim();
            if (isEmbeddedDataUrl(trimmedPhotoUrl)) {
                resolvedPhotoUrls.add(storeEmbeddedPhoto(resourceId, trimmedPhotoUrl));
                continue;
            }

            if (isManagedPhotoUrl(trimmedPhotoUrl) && !belongsToResource(resourceId, trimmedPhotoUrl)) {
                throw new BadRequestException("Photo URL does not belong to the selected car.");
            }

            resolvedPhotoUrls.add(trimmedPhotoUrl);
        }

        deleteRemovedManagedPhotos(resourceId, existingPhotoUrls, resolvedPhotoUrls);
        deleteDirectoryIfEmpty(getCarPhotoDirectory(resourceId));

        return new ArrayList<>(resolvedPhotoUrls);
    }

    public ResponseEntity<org.springframework.core.io.Resource> readPhoto(Long resourceId, String fileName) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");

        Path photoPath = resolvePhotoPath(resourceId, fileName);
        if (Files.notExists(photoPath)) {
            throw new NotFoundException("Photo not found for car id: " + resourceId);
        }

        try {
            UrlResource resource = new UrlResource(photoPath.toUri());
            MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read photo " + fileName, ex);
        }
    }

    public void deleteAllPhotos(Long resourceId) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");

        Path carPhotoDirectory = getCarPhotoDirectory(resourceId);
        if (Files.notExists(carPhotoDirectory)) {
            return;
        }

        try (var paths = Files.walk(carPhotoDirectory)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new UncheckedIOException("Failed to delete photo path " + path, ex);
                        }
                    });
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to delete photo directory " + carPhotoDirectory, ex);
        }
    }

    private String storeEmbeddedPhoto(Long resourceId, String dataUrl) {
        Matcher matcher = DATA_URL_PATTERN.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new BadRequestException("Unsupported embedded image format.");
        }

        String mimeType = matcher.group(1).toLowerCase();
        String base64Payload = matcher.group(2);
        byte[] imageBytes;

        try {
            imageBytes = Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Embedded photo data is invalid.");
        }

        String fileName = "photo-" + UUID.randomUUID() + "." + extensionForMimeType(mimeType);
        Path photoPath = resolvePhotoPath(resourceId, fileName);

        try {
            Files.createDirectories(photoPath.getParent());
            Files.write(photoPath, imageBytes);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store photo for car id: " + resourceId, ex);
        }

        return managedPhotoUrl(resourceId, fileName);
    }

    private void deleteRemovedManagedPhotos(Long resourceId, List<String> existingPhotoUrls, LinkedHashSet<String> nextPhotoUrls) {
        for (String existingPhotoUrl : existingPhotoUrls == null ? List.<String>of() : existingPhotoUrls) {
            if (!belongsToResource(resourceId, existingPhotoUrl) || nextPhotoUrls.contains(existingPhotoUrl)) {
                continue;
            }

            String fileName = existingPhotoUrl.substring(managedPhotoUrlPrefix(resourceId).length());
            try {
                Files.deleteIfExists(resolvePhotoPath(resourceId, fileName));
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to remove old photo " + fileName, ex);
            }
        }
    }

    private boolean isEmbeddedDataUrl(String value) {
        return DATA_URL_PATTERN.matcher(value).matches();
    }

    private boolean isManagedPhotoUrl(String value) {
        return value.startsWith("/api/resources/") && value.contains("/photos/");
    }

    private boolean belongsToResource(Long resourceId, String value) {
        return value != null && value.startsWith(managedPhotoUrlPrefix(resourceId));
    }

    private String managedPhotoUrl(Long resourceId, String fileName) {
        return managedPhotoUrlPrefix(resourceId) + fileName;
    }

    private String managedPhotoUrlPrefix(Long resourceId) {
        return "/api/resources/" + resourceId + "/photos/";
    }

    private Path getCarPhotoDirectory(Long resourceId) {
        return photoRootDirectory.resolve("car-" + resourceId).normalize();
    }

    private Path resolvePhotoPath(Long resourceId, String fileName) {
        Path carPhotoDirectory = getCarPhotoDirectory(resourceId);
        Path photoPath = carPhotoDirectory.resolve(fileName).normalize();
        if (!photoPath.startsWith(carPhotoDirectory)) {
            throw new BadRequestException("Invalid photo path.");
        }
        return photoPath;
    }

    private void deleteDirectoryIfEmpty(Path directory) {
        try {
            if (Files.exists(directory) && Files.isDirectory(directory)) {
                try (var entries = Files.list(directory)) {
                    if (!entries.findAny().isPresent()) {
                        Files.deleteIfExists(directory);
                    }
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to clean up empty photo directory " + directory, ex);
        }
    }

    private String extensionForMimeType(String mimeType) {
        return switch (mimeType) {
            case "image/avif" -> "avif";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/svg+xml" -> "svg";
            default -> throw new BadRequestException("Unsupported image type: " + mimeType);
        };
    }
}
