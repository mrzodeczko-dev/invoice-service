package com.rzodeczko.presentation.controller;


import com.rzodeczko.application.port.input.HandleInvoiceWebhookUseCase;
import com.rzodeczko.infrastructure.webhook.access.TrustedWebhookClient;
import com.rzodeczko.infrastructure.webhook.access.aop.WebhookSecured;
import com.rzodeczko.presentation.dto.FakturowniaWebhookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    private final HandleInvoiceWebhookUseCase handleInvoiceWebhookUseCase;

    @PostMapping("/fakturownia/invoices/update")
    @WebhookSecured(value = TrustedWebhookClient.FAKTUROWNIA)
    public ResponseEntity<Void> handleInvoiceUpdated(
            @RequestBody FakturowniaWebhookDto payload
    ) {
        log.info(">>> Webhook for invoice updated: payload Id={}", payload.id());

        if (
                payload.deal() == null ||
                        payload.deal().externalIds() == null ||
                        !payload.deal().externalIds().containsKey("fakturownia")
        ) {
            log.info("<<< Webhook without externalId: fakturowniaId={}", payload.id());
            return ResponseEntity.ok().build();
        }

        Long externalId = payload.deal().externalIds().get("fakturownia");
        handleInvoiceWebhookUseCase.handle(String.valueOf(externalId));
        return ResponseEntity.ok().build();
    }
}
