package com.rzodeczko.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record FakturowniaWebhookDealDto(
        @JsonProperty("external_ids") Map<String, Long> externalIds
) {
}
