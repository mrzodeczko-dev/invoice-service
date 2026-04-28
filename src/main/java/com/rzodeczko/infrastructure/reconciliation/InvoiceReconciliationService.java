package com.rzodeczko.infrastructure.reconciliation;

import com.rzodeczko.application.port.input.InvoiceIssueResult;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary;
import com.rzodeczko.presentation.dto.FakturowniaGetInvoiceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceReconciliationService {

    private final InvoiceTransactionBoundary invoiceTransactionBoundary;

    public Optional<InvoiceIssueResult> reconcileFromExisting(
            Invoice localInvoice,
            List<FakturowniaGetInvoiceDto> externalInvoices
    ) {
        List<FakturowniaGetInvoiceDto> matchingInvoices = externalInvoices.stream()
                .filter(external -> isSameBusinessInvoice(localInvoice, external))
                .toList();

        if (matchingInvoices.isEmpty()) {
            return Optional.empty();
        }

        if (hasExistingExternalIdMatch(localInvoice, matchingInvoices)) {
            logIfDuplicatesExist(localInvoice, matchingInvoices);
            if (!localInvoice.isIssued()) {
                invoiceTransactionBoundary.markInvoiceAsIssued(localInvoice, localInvoice.getExternalId());
            }
            return Optional.of(new InvoiceIssueResult.Issued(localInvoice.getId()));
        }

        if (matchingInvoices.size() == 1) {
            return issueFromSingleMatch(localInvoice, matchingInvoices.getFirst());
        }

        return markManualReconciliationRequired(localInvoice, matchingInvoices);
    }

    private boolean hasExistingExternalIdMatch(
            Invoice localInvoice,
            List<FakturowniaGetInvoiceDto> matchingInvoices
    ) {
        return localInvoice.getExternalId() != null
                && matchingInvoices.stream()
                .anyMatch(external -> Objects.equals(external.id(), localInvoice.getExternalId()));
    }

    private void logIfDuplicatesExist(
            Invoice localInvoice,
            List<FakturowniaGetInvoiceDto> matchingInvoices
    ) {
        if (matchingInvoices.size() > 1) {
            log.warn("Multiple external invoices found for same orderId, but existing mapping is preserved. invoiceId={}, orderId={}, externalId={}, matchingExternalIds={}",
                    localInvoice.getId(),
                    localInvoice.getOrderId(),
                    localInvoice.getExternalId(),
                    matchingInvoices.stream().map(FakturowniaGetInvoiceDto::id).toList());
        }
    }

    private Optional<InvoiceIssueResult> issueFromSingleMatch(
            Invoice localInvoice,
            FakturowniaGetInvoiceDto externalInvoice
    ) {
        invoiceTransactionBoundary.markInvoiceAsIssued(localInvoice, externalInvoice.id());

        log.info("Invoice reconciled from tax system. invoiceId={}, orderId={}, externalId={}",
                localInvoice.getId(), localInvoice.getOrderId(), externalInvoice.id());

        return Optional.of(new InvoiceIssueResult.Issued(localInvoice.getId()));
    }

    private Optional<InvoiceIssueResult> markManualReconciliationRequired(
            Invoice localInvoice,
            List<FakturowniaGetInvoiceDto> matchingInvoices
    ) {
        invoiceTransactionBoundary.markReconciliationRequired(localInvoice);

        log.warn("Reconciliation required. invoiceId={}, orderId={}, matchingExternalIds={}",
                localInvoice.getId(),
                localInvoice.getOrderId(),
                matchingInvoices.stream().map(FakturowniaGetInvoiceDto::id).toList());

        return Optional.of(new InvoiceIssueResult.ReconciliationRequired(localInvoice.getId()));
    }


    private boolean isSameBusinessInvoice(Invoice localInvoice, FakturowniaGetInvoiceDto externalInvoice) {
        return Strings.CS.equals(localInvoice.getOrderId().toString(), externalInvoice.orderId());
    }
}
