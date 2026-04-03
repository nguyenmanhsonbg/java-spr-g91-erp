package com.g90.backend.config;

import com.g90.backend.modules.product.storage.ProductImageStorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProductImageResourceConfig implements WebMvcConfigurer {

    private final ProductImageStorageProperties storageProperties;

    public ProductImageResourceConfig(ProductImageStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pattern = storageProperties.urlPrefix() + "**";
        String location = storageProperties.uploadDir().toUri().toString();
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}
