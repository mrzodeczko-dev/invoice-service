package com.rzodeczko.infrastructure.usecase;

import com.rzodeczko.application.exception.EmptyPdfResponseException;
import com.rzodeczko.application.port.input.GenerateInvoiceCommand;
import com.rzodeczko.application.port.output.TaxSystemPort;
import com.rzodeczko.application.service.InvoiceService;
import com.rzodeczko.domain.exception.InvoiceAlreadyExistsException;
import com.rzodeczko.domain.exception.InvoiceNotIssuedException;
import com.rzodeczko.domain.exception.ResourceNotFoundException;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceUseCaseImpl.
 */
class InvoiceUseCaseImplTest {
    private InvoiceTransactionBoundary invoiceTransactionBoundary;
    private InvoiceService invoiceService;
    private TaxSystemPort taxSystemPort;
    private InvoiceUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        invoiceTransactionBoundary = mock(InvoiceTransactionBoundary.class);
        invoiceService = mock(InvoiceService.class);
        taxSystemPort = mock(TaxSystemPort.class);
        useCase = new InvoiceUseCaseImpl(invoiceTransactionBoundary, invoiceService, taxSystemPort);
    }

    @Test
    void generate_shouldThrowIfOrderExists() {
        UUID orderId = UUID.randomUUID();
        GenerateInvoiceCommand command = mock(GenerateInvoiceCommand.class);
        when(command.orderId()).thenReturn(orderId);
        when(invoiceTransactionBoundary.existsByOrderId(orderId)).thenReturn(true);
        assertThrows(InvoiceAlreadyExistsException.class, () -> useCase.generate(command));
    }

    @Test
    void generate_shouldSaveAndIssueInvoice() {
        GenerateInvoiceCommand command = mock(GenerateInvoiceCommand.class);
        Invoice invoice = mock(Invoice.class);
        when(command.orderId()).thenReturn(UUID.randomUUID());
        when(invoiceTransactionBoundary.existsByOrderId(any())).thenReturn(false);
        when(invoiceService.buildInvoice(command)).thenReturn(invoice);
        when(taxSystemPort.issueInvoice(invoice)).thenReturn("ext-1");
        doNothing().when(invoiceTransactionBoundary).saveNewInvoice(invoice);
        doNothing().when(invoiceTransactionBoundary).markInvoiceAsIssued(invoice, "ext-1");
        UUID id = UUID.randomUUID();
        when(invoice.getId()).thenReturn(id);
        assertEquals(id, useCase.generate(command));
    }

    @Test
    void getPdf_shouldThrowIfInvoiceNotFound() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceTransactionBoundary.findById(invoiceId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.getPdf(invoiceId));
    }

    @Test
    void getPdf_shouldThrowIfInvoiceNotIssued() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoiceTransactionBoundary.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoice.isIssued()).thenReturn(false);
        assertThrows(InvoiceNotIssuedException.class, () -> useCase.getPdf(invoiceId));
    }

    @Test
    void getPdf_shouldReturnCachedPdf() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoiceTransactionBoundary.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoice.isIssued()).thenReturn(true);
        byte[] pdf = new byte[]{1, 2, 3};
        when(invoiceTransactionBoundary.findPdfContent(invoiceId)).thenReturn(Optional.of(pdf));
        assertArrayEquals(pdf, useCase.getPdf(invoiceId));
    }

    @Test
    void getPdf_shouldFetchAndCachePdfIfNotCached() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoiceTransactionBoundary.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoice.isIssued()).thenReturn(true);
        when(invoice.getExternalId()).thenReturn("ext-1");
        when(invoiceTransactionBoundary.findPdfContent(invoiceId)).thenReturn(Optional.empty());
        byte[] pdf = new byte[]{1, 2, 3};
        when(taxSystemPort.getPdf("ext-1")).thenReturn(pdf);
        doNothing().when(invoiceTransactionBoundary).savePdfContent(invoiceId, pdf);
        assertArrayEquals(pdf, useCase.getPdf(invoiceId));
    }

    @Test
    void getPdf_shouldThrowIfPdfEmpty() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoiceTransactionBoundary.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoice.isIssued()).thenReturn(true);
        when(invoice.getExternalId()).thenReturn("ext-1");
        when(invoiceTransactionBoundary.findPdfContent(invoiceId)).thenReturn(Optional.empty());
        when(taxSystemPort.getPdf("ext-1")).thenReturn(new byte[0]);
        assertThrows(EmptyPdfResponseException.class, () -> useCase.getPdf(invoiceId));
    }
}

