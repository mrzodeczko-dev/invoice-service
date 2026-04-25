package com.rzodeczko.infrastructure.configuration.properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Objects;

@ConfigurationProperties(prefix = "webhooks")
public record WebhookClientsConfig(
        Map<String, ClientConfig> clients) {

    public WebhookClientsConfig {
        clients = Objects.isNull(clients) ? Map.of() : Map.copyOf(clients); /*unmodifiable in runtime*/
    }

    public record ClientConfig(
            Boolean enabled,
            int requestsPerMinute,
            String token) {

        public ClientConfig {
            enabled = Objects.isNull(enabled) || enabled;
            token = StringUtils.defaultString(token);
        }
    }
}