package com.rzodeczko.application.service;

import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.model.InvoiceStatus;
import com.rzodeczko.domain.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceService.
 */
class InvoiceServiceTest {
    private InvoiceRepository invoiceRepository;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        invoiceService = new InvoiceService(invoiceRepository);
    }

    @Test
    void existsByOrderId_shouldDelegateToRepository() {
        UUID orderId = UUID.randomUUID();
        when(invoiceRepository.existsByOrderId(orderId)).thenReturn(true);
        assertTrue(invoiceService.existsByOrderId(orderId));
        verify(invoiceRepository).existsByOrderId(orderId);
    }

    @Test
    void buildInvoice_shouldCreateInvoiceWithCorrectFields() {
        var command = new com.rzodeczko.application.port.input.GenerateInvoiceCommand(
                UUID.randomUUID(), "1234567890", "Test Buyer",
                List.of(new com.rzodeczko.application.port.input.ItemCommand("item", 2, BigDecimal.TEN))
        );
        Invoice invoice = invoiceService.buildInvoice(command);
        assertEquals(command.orderId(), invoice.getOrderId());
        assertEquals(command.taxId(), invoice.getTaxId());
        assertEquals(command.buyerName(), invoice.getBuyerName());
        assertEquals(1, invoice.getItems().size());
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
    }

    @Test
    void saveNewInvoice_shouldDelegateToRepository() {
        Invoice invoice = mock(Invoice.class);
        invoiceService.saveNewInvoice(invoice);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void markInvoiceAsIssued_shouldUpdateStatusAndDelegateToRepository() {
        Invoice invoice = mock(Invoice.class);
        doNothing().when(invoice).markAsIssued(anyString());
        invoiceService.markInvoiceAsIssued(invoice, "ext-1");
        verify(invoice).markAsIssued("ext-1");
        verify(invoiceRepository).save(invoice);
    }
}

