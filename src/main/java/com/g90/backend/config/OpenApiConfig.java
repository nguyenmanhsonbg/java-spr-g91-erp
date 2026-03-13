package com.g90.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI g90OpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("G90 Steel Business Management API")
                        .description("OpenAPI documentation for the G90 steel ERP backend.")
                        .version("v1")
                        .contact(new Contact().name("G90 Backend Team")));
    }
}
