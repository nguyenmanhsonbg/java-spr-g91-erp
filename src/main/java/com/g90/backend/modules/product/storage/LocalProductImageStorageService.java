package com.g90.backend.modules.product.storage;

import com.g90.backend.exception.FileStorageException;
import com.g90.backend.exception.RequestValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalProductImageStorageService implements ProductImageStorageService {

    private static final String FILE_FIELD = "files";

    private final ProductImageStorageProperties properties;

    public LocalProductImageStorageService(ProductImageStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> store(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw RequestValidationException.singleError(FILE_FIELD, "At least one image is required");
        }

        List<String> storedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null) {
                continue;
            }
            storedUrls.add(storeFile(file));
        }
        if (storedUrls.isEmpty()) {
            throw RequestValidationException.singleError(FILE_FIELD, "At least one image is required");
        }
        return storedUrls;
    }

    private String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw RequestValidationException.singleError(FILE_FIELD, "Uploaded files must not be empty");
        }
        if (file.getSize() > properties.maxFileSize()) {
            throw RequestValidationException.singleError(
                    FILE_FIELD,
                    String.format("Each file must not exceed %d bytes", properties.maxFileSize())
            );
        }

        String originalName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename(), "File name is required"));
        String extension = StringUtils.getFilenameExtension(originalName);
        if (!StringUtils.hasText(extension)) {
            throw RequestValidationException.singleError(FILE_FIELD, "Uploaded files must contain a valid extension");
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (!properties.allowedExtensions().contains(normalizedExtension)) {
            throw RequestValidationException.singleError(
                    FILE_FIELD,
                    String.format("Allowed image types are %s", String.join(", ", properties.allowedExtensions()))
            );
        }

        String fileName = UUID.randomUUID() + "." + normalizedExtension;
        Path target = properties.uploadDir().resolve(fileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new FileStorageException("FILE_UPLOAD_FAILED", "Failed to store uploaded image", exception);
        }

        return properties.urlPrefix() + fileName;
    }
}
