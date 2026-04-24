package com.rzodeczko.infrastructure.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Invoice service API",
                version = "v1",
                description = "Invoice service API documentation",
                contact = @Contact(name = "Michał Rzodeczko")
        )
)
public class OpenApiConfig {
}
