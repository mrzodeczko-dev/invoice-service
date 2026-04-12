package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.port.input.GenerateInvoiceUseCase;
import com.rzodeczko.application.port.input.GetInvoicePdfUseCase;
import com.rzodeczko.presentation.dto.CreateInvoiceRequestDto;
import com.rzodeczko.presentation.dto.CreateInvoiceResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceController.
 */
class InvoiceControllerTest {
    private GenerateInvoiceUseCase generateInvoiceUseCase;
    private GetInvoicePdfUseCase getInvoicePdfUseCase;
    private InvoiceController controller;

    @BeforeEach
    void setUp() {
        generateInvoiceUseCase = mock(GenerateInvoiceUseCase.class);
        getInvoicePdfUseCase = mock(GetInvoicePdfUseCase.class);
        controller = new InvoiceController(generateInvoiceUseCase, getInvoicePdfUseCase);
    }

    @Test
    void createInvoice_shouldReturnCreatedResponse() {
        CreateInvoiceRequestDto.ItemRequestDto item = new CreateInvoiceRequestDto.ItemRequestDto("item", 1, java.math.BigDecimal.TEN);
        CreateInvoiceRequestDto request = new CreateInvoiceRequestDto(UUID.randomUUID(), "1234567890", "Buyer", List.of(item));
        UUID invoiceId = UUID.randomUUID();
        when(generateInvoiceUseCase.generate(any())).thenReturn(invoiceId);
        ResponseEntity<CreateInvoiceResponseDto> response = controller.createInvoice(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(invoiceId, response.getBody().invoiceId());
    }

    @Test
    void getInvoicePdf_shouldReturnPdfContent() {
        UUID id = UUID.randomUUID();
        byte[] pdf = new byte[]{1,2,3};
        when(getInvoicePdfUseCase.getPdf(id)).thenReturn(pdf);
        ResponseEntity<byte[]> response = controller.getInvoicePdf(id);
        assertEquals(pdf, response.getBody());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
    }
}
