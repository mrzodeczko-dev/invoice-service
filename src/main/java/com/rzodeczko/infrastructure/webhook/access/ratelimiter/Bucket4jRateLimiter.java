package com.rzodeczko.infrastructure.webhook.access.ratelimiter;

import com.rzodeczko.infrastructure.configuration.properties.WebhookClientsConfig;
import com.rzodeczko.infrastructure.webhook.access.exception.WebhookRateLimitExceededException;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class Bucket4jRateLimiter implements ClientRateLimiter {

    private final ProxyManager<String> proxyManager;
    private final WebhookClientsConfig webhookClientsConfig;

    @Override
    public void check(String clientId) {
        String key = "webhook:ratelimit:" + clientId;
        WebhookClientsConfig.ClientConfig clientConfig = webhookClientsConfig.clients().get(clientId);
        if (Objects.isNull(clientConfig) || clientConfig.requestsPerMinute() == 0) {
            return; // No rate limit configured for this client
        }

        int rateLimit = clientConfig.requestsPerMinute();
        Supplier<BucketConfiguration> configurationSupplier = () ->
                BucketConfiguration.builder()
                        .addLimit(limit -> limit
                                .capacity(rateLimit)
                                .refillIntervally(rateLimit, Duration.ofMinutes(1))
                        )
                        .build();

        ConsumptionProbe probe = proxyManager.builder()
                .build(key, configurationSupplier)
                .tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            throw new WebhookRateLimitExceededException(
                    "Rate limit exceeded for clientId=%s. Retry after %d seconds"
                            .formatted(clientId, probe.getNanosToWaitForRefill() / 1_000_000_000)
            );
        }
    }
}
