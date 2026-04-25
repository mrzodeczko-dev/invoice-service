package com.rzodeczko.infrastructure.webhook.access.aop;

import com.rzodeczko.infrastructure.webhook.access.TrustedWebhookClient;
import com.rzodeczko.infrastructure.webhook.access.WebhookInvoiceUpdateAccessVerifier;
import com.rzodeczko.infrastructure.webhook.access.exception.UnauthorizedWebhookAccessException;
import com.rzodeczko.infrastructure.webhook.access.ratelimiter.ClientRateLimiter;
import com.rzodeczko.presentation.dto.FakturowniaWebhookDto;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
public class WebhookSecurityAspect {

    private final WebhookInvoiceUpdateAccessVerifier accessVerifier;
    private final ClientRateLimiter bucket4jRateLimiter;

    @Before("@annotation(webhookSecured) && args(payload,..)")
    public void verifyAccessAndRateLimit(
            WebhookSecured webhookSecured,
            FakturowniaWebhookDto payload
    ) {
        Optional<TrustedWebhookClient> actualClientOpt = TrustedWebhookClient.from(payload.appName());

        boolean isTrustedClient = actualClientOpt.isPresent() &&
                Arrays.stream(webhookSecured.value())
                        .anyMatch(allowedClient ->
                                allowedClient.getClientId().equals(actualClientOpt.get().getClientId()));
        if (!isTrustedClient) {
            throw new UnauthorizedWebhookAccessException(
                    "Webhook client '%s' is not allowed for this endpoint".formatted(payload.appName())
            );
        }

        accessVerifier.verifyEnabledAndSharedSecret(payload.appName(), payload.apiToken());
        bucket4jRateLimiter.check(payload.appName());
    }
}