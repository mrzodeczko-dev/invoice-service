package com.rzodeczko.infrastructure.configuration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConditionalOnProperty(name = "http.cors.swagger-ui.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "http.cors.swagger-ui")
@Getter
@Setter
public class SwaggerCorsProperties {
    private String allowedOrigins;
    private boolean enabled;
}
