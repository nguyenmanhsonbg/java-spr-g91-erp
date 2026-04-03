package com.g90.backend.modules.product.storage;

import java.nio.file.Path;
import java.util.Set;

public record ProductImageStorageProperties(
        Path uploadDir,
        String urlPrefix,
        long maxFileSize,
        Set<String> allowedExtensions
) {
}
