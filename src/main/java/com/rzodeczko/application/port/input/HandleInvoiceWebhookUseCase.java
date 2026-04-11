package com.rzodeczko.application.port.input;

public interface HandleInvoiceWebhookUseCase {
    void handle(String externalId);
}
