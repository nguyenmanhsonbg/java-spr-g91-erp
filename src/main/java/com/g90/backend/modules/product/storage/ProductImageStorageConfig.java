package com.g90.backend.modules.product.storage;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class ProductImageStorageConfig {

    @Bean
    public ProductImageStorageProperties productImageStorageProperties(
            @Value("${app.storage.products.location:uploads/products}") String location,
            @Value("${app.storage.products.url-prefix:/uploads/products/}") String urlPrefix,
            @Value("${app.storage.products.max-file-size:5242880}") long maxFileSize,
            @Value("${app.storage.products.allowed-types:jpg,jpeg,png,webp}") String allowedTypes
    ) {
        Path uploadDir = Paths.get(location).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare product upload directory", exception);
        }

        return new ProductImageStorageProperties(
                uploadDir,
                normalizeUrlPrefix(urlPrefix),
                Math.max(1, maxFileSize),
                parseAllowedTypes(allowedTypes)
        );
    }

    private static String normalizeUrlPrefix(String value) {
        String prefix = StringUtils.trimWhitespace(value);
        if (!StringUtils.hasText(prefix)) {
            prefix = "/uploads/products/";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }

    private static Set<String> parseAllowedTypes(@Nullable String allowedTypes) {
        if (!StringUtils.hasText(allowedTypes)) {
            return Set.of("jpg", "jpeg", "png", "webp");
        }
        LinkedHashSet<String> parsed = Arrays.stream(allowedTypes.split(","))
                .map(StringUtils::trimWhitespace)
                .filter(StringUtils::hasText)
                .map(type -> type.toLowerCase().replace(".", ""))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Set.copyOf(parsed);
    }
}
