package com.rzodeczko.infrastructure.transaction;


import com.rzodeczko.application.service.InvoiceService;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceTransactionBoundary {
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public boolean existsByOrderId(UUID orderId) {
        return invoiceService.existsByOrderId(orderId);
    }

    @Transactional
    public void saveNewInvoice(Invoice invoice) {
        invoiceService.saveNewInvoice(invoice);
    }

    @Transactional
    public void markInvoiceAsIssued(Invoice invoice, String externalId) {
        invoiceService.markInvoiceAsIssued(invoice, externalId);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> findById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> findPdfContent(UUID invoiceId) {
        return invoiceRepository.findPdfContent(invoiceId);
    }

    @Transactional
    public void savePdfContent(UUID invoiceId, byte[] content) {
        invoiceRepository.savePdfContent(invoiceId, content);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> findByExternalId(String externalId) {
        return invoiceRepository.findByExternalId(externalId);
    }
}
