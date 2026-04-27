package com.rzodeczko.infrastructure.reconciliation;

import com.rzodeczko.application.exception.TaxSystemTemporaryException;
import com.rzodeczko.application.port.output.TaxSystemPort;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "reconciliation.jobs.unknown-invoice-recovery.enabled", havingValue = "true")
public class InvoiceIssueUnknownReconciliationJob {

    private final InvoiceTransactionBoundary invoiceTransactionBoundary;
    private final TaxSystemPort taxSystemPort;
    private final InvoiceReconciliationService invoiceReconciliationService;

    @Scheduled(fixedDelayString = "${invoice.reconciliation.fixed-delay-ms:30000}")
    @SchedulerLock(
            name = "invoiceIssueUnknownReconciliationJob",
            lockAtMostFor = "10m",
            lockAtLeastFor = "5s"
    )
    public void reconcileIssueUnknownInvoices() {
        List<Invoice> invoices = invoiceTransactionBoundary.findIssueUnknownBatch(50);

        if (invoices.isEmpty()) {
            return;
        }

        log.info("Starting ISSUE_UNKNOWN reconciliation batch. count={}", invoices.size());

        for (Invoice invoice : invoices) {
            reconcileSingleInvoice(invoice);
        }
    }

    private void reconcileSingleInvoice(Invoice invoice) {
        String orderId = invoice.getOrderId().toString();

        try {
            invoiceReconciliationService.reconcileFromExisting(
                    invoice,
                    taxSystemPort.findByOrderId(orderId)
            ).ifPresentOrElse(
                    result -> log.info("Invoice reconciled asynchronously. invoiceId={}, orderId={}, result={}",
                            invoice.getId(), invoice.getOrderId(), result.getClass().getSimpleName()),
                    () -> log.debug("Invoice still not found in tax system. invoiceId={}, orderId={}",
                            invoice.getId(), invoice.getOrderId())
            );
        } catch (TaxSystemTemporaryException ex) {
            log.warn("Temporary error during asynchronous reconciliation. invoiceId={}, orderId={}",
                    invoice.getId(), invoice.getOrderId(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during asynchronous reconciliation. invoiceId={}, orderId={}",
                    invoice.getId(), invoice.getOrderId(), ex);
        }
    }
}