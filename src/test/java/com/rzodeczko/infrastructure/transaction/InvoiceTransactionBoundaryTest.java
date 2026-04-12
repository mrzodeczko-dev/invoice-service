package com.rzodeczko.infrastructure.transaction;

import com.rzodeczko.application.service.InvoiceService;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceTransactionBoundary.
 */
class InvoiceTransactionBoundaryTest {
    private InvoiceService invoiceService;
    private InvoiceRepository invoiceRepository;
    private InvoiceTransactionBoundary boundary;

    @BeforeEach
    void setUp() {
        invoiceService = mock(InvoiceService.class);
        invoiceRepository = mock(InvoiceRepository.class);
        boundary = new InvoiceTransactionBoundary(invoiceService, invoiceRepository);
    }

    @Test
    void existsByOrderId_shouldDelegate() {
        UUID orderId = UUID.randomUUID();
        when(invoiceService.existsByOrderId(orderId)).thenReturn(true);
        assertTrue(boundary.existsByOrderId(orderId));
    }

    @Test
    void saveNewInvoice_shouldDelegate() {
        Invoice invoice = mock(Invoice.class);
        boundary.saveNewInvoice(invoice);
        verify(invoiceService).saveNewInvoice(invoice);
    }

    @Test
    void markInvoiceAsIssued_shouldDelegate() {
        Invoice invoice = mock(Invoice.class);
        boundary.markInvoiceAsIssued(invoice, "ext-1");
        verify(invoiceService).markInvoiceAsIssued(invoice, "ext-1");
    }

    @Test
    void findById_shouldDelegate() {
        UUID id = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        assertEquals(invoice, boundary.findById(id).orElse(null));
    }

    @Test
    void findPdfContent_shouldDelegate() {
        UUID id = UUID.randomUUID();
        byte[] pdf = new byte[]{1,2,3};
        when(invoiceRepository.findPdfContent(id)).thenReturn(Optional.of(pdf));
        assertArrayEquals(pdf, boundary.findPdfContent(id).orElse(null));
    }

    @Test
    void savePdfContent_shouldDelegate() {
        UUID id = UUID.randomUUID();
        byte[] content = new byte[]{1,2,3};
        boundary.savePdfContent(id, content);
        verify(invoiceRepository).savePdfContent(id, content);
    }

    @Test
    void findByExternalId_shouldDelegate() {
        String externalId = "ext-1";
        Invoice invoice = mock(Invoice.class);
        when(invoiceRepository.findByExternalId(externalId)).thenReturn(Optional.of(invoice));
        assertEquals(invoice, boundary.findByExternalId(externalId).orElse(null));
    }
}

