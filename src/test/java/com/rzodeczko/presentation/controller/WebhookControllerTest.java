package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.port.input.HandleInvoiceWebhookUseCase;
import com.rzodeczko.presentation.dto.FakturowniaWebhookDealDto;
import com.rzodeczko.presentation.dto.FakturowniaWebhookDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookController.
 */
class WebhookControllerTest {
    private HandleInvoiceWebhookUseCase handleInvoiceWebhookUseCase;
    private WebhookController controller;

    @BeforeEach
    void setUp() {
        handleInvoiceWebhookUseCase = mock(HandleInvoiceWebhookUseCase.class);
        controller = new WebhookController(handleInvoiceWebhookUseCase);
    }

    @Test
    void handleInvoiceUpdated_shouldReturnOkIfNoExternalId() {
        FakturowniaWebhookDealDto deal = new FakturowniaWebhookDealDto(new HashMap<>());
        FakturowniaWebhookDto payload = new FakturowniaWebhookDto(1L, deal, null, null);
        ResponseEntity<Void> response = controller.handleInvoiceUpdated(payload);
        assertEquals(200, response.getStatusCode().value());
        verifyNoInteractions(handleInvoiceWebhookUseCase);
    }

    @Test
    void handleInvoiceUpdated_shouldCallHandleIfExternalIdPresent() {
        Map<String, Long> externalIds = new HashMap<>();
        externalIds.put("fakturownia", 123L);
        FakturowniaWebhookDealDto deal = new FakturowniaWebhookDealDto(externalIds);
        FakturowniaWebhookDto payload = new FakturowniaWebhookDto(1L, deal, null, null);
        ResponseEntity<Void> response = controller.handleInvoiceUpdated(payload);
        assertEquals(200, response.getStatusCode().value());
        verify(handleInvoiceWebhookUseCase).handle("123");
    }
}

