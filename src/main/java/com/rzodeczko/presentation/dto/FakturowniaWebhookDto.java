package com.rzodeczko.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FakturowniaWebhookDto(
        Long id,
        FakturowniaWebhookDealDto deal,
        @JsonProperty("app_name") String appName,
        @JsonProperty("api_token") String apiToken
) {
}
