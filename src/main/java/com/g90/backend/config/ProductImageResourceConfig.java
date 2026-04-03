package com.g90.backend.config;

import com.g90.backend.modules.product.storage.ProductImageStorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProductImageResourceConfig implements WebMvcConfigurer {

    private final ProductImageStorageProperties storageProperties;

    public ProductImageResourceConfig(ObjectProvider<ProductImageStorageProperties> storagePropertiesProvider) {
        this.storageProperties = storagePropertiesProvider.getIfAvailable();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (storageProperties == null) {
            return;
        }
        String pattern = storageProperties.urlPrefix() + "**";
        String location = storageProperties.uploadDir().toUri().toString();
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}
