package com.rzodeczko.infrastructure.usecase;


import com.rzodeczko.application.exception.ExternalTaxSystemException;
import com.rzodeczko.application.port.input.HandleInvoiceWebhookUseCase;
import com.rzodeczko.application.port.output.TaxSystemPort;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceWebhookUseCaseImpl implements HandleInvoiceWebhookUseCase {

    private final InvoiceTransactionBoundary invoiceTransactionBoundary;
    private final TaxSystemPort taxSystemPort;

    @Override
    public void handle(String externalId) {
        log.info("Webhook invoice:update. externalId={}", externalId);

        Optional<Invoice> invoiceOp = invoiceTransactionBoundary
                .findByExternalId(externalId);
        if (invoiceOp.isEmpty()) {
            log.warn("Invoice not found in cache. externalId={}", externalId);
            return;
        }
        Invoice invoice = invoiceOp.get();

        if (invoiceTransactionBoundary.findPdfContent(invoice.getId()).isEmpty()) {
            log.debug("PDF content not found in cache. externalId={}", externalId);
            return;
        }

        byte[] freshPdf;
        try {
            freshPdf = taxSystemPort.getPdf(externalId);
        } catch (ExternalTaxSystemException e) {
            log.warn("Failed to fetch PDF from Fakturownia, will retry on next webhook. externalId={}", externalId);
            return;
        }

        if (freshPdf == null || freshPdf.length == 0) {
            log.warn("Fetched pdf content is empty. externalId={}", externalId);
            return;
        }

        invoiceTransactionBoundary.savePdfContent(invoice.getId(), freshPdf);
        log.info("PDF content refreshed in cache. externalId={}, invoiceId={}", externalId, invoice.getId());
    }
}
