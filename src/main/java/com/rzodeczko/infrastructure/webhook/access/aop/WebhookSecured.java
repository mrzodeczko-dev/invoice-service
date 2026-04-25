package com.rzodeczko.infrastructure.webhook.access.aop;

import com.rzodeczko.infrastructure.webhook.access.TrustedWebhookClient;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebhookSecured {
    TrustedWebhookClient[] value();
}
