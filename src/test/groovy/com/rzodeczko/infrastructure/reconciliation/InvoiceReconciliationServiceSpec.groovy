package com.rzodeczko.infrastructure.reconciliation

import com.rzodeczko.application.port.input.InvoiceIssueResult
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary
import com.rzodeczko.presentation.dto.FakturowniaGetInvoiceDto
import spock.lang.Specification
import spock.lang.Subject

class InvoiceReconciliationServiceSpec extends Specification {

    InvoiceTransactionBoundary transactionBoundary = Mock()

    @Subject
    InvoiceReconciliationService service = new InvoiceReconciliationService(transactionBoundary)

    def invoiceId = UUID.randomUUID()
    def orderId = UUID.randomUUID()
    def validItems = [new InvoiceItem("Item", 1, BigDecimal.TEN)]

    // --- no matches ---

    def "should return empty when no external invoices match"() {
        given:
        def localInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        def externals = [new FakturowniaGetInvoiceDto("ext-1", UUID.randomUUID().toString())]

        when:
        def result = service.reconcileFromExisting(localInvoice, externals)

        then:
        result.isEmpty()
    }

    def "should return empty when external list is empty"() {
        given:
        def localInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)

        when:
        def result = service.reconcileFromExisting(localInvoice, [])

        then:
        result.isEmpty()
    }

    // --- single match ---

    def "should issue from single matching external invoice"() {
        given:
        def localInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        def externals = [new FakturowniaGetInvoiceDto("ext-1", orderId.toString())]

        when:
        def result = service.reconcileFromExisting(localInvoice, externals)

        then:
        result.isPresent()
        result.get() instanceof InvoiceIssueResult.Issued
        result.get().invoiceId() == invoiceId
        1 * transactionBoundary.markInvoiceAsIssued(localInvoice, "ext-1")
    }

    // --- existing externalId match ---

    def "should return Issued when local externalId matches one of external invoices"() {
        given:
        def localInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        def externals = [new FakturowniaGetInvoiceDto("ext-1", orderId.toString())]

        when:
        def result = service.reconcileFromExisting(localInvoice, externals)

        then:
        result.isPresent()
        result.get() instanceof InvoiceIssueResult.Issued
        // already issued, should not call markInvoiceAsIssued again
        0 * transactionBoundary.markInvoiceAsIssued(_, _)
    }

    def "should mark as issued when local has externalId match but status is not ISSUED"() {
        given:
        def localInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", null, InvoiceStatus.ISSUING, validItems, null)
        def externals = [new FakturowniaGetInvoiceDto("ext-99", orderId.toString())]

        when:
        def result = service.reconcileFromExisting(localInvoice, externals)

        then:
        result.isPresent()
        result.get() instanceof InvoiceIssueResult.Issued
        1 * transactionBoundary.markInvoiceAsIssued(localInvoice, "ext-99")
    }

    // --- multiple matches ---

    def "should mark reconciliation required when multiple external invoices match"() {
        given:
        def localInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        def externals = [
                new FakturowniaGetInvoiceDto("ext-1", orderId.toString()),
                new FakturowniaGetInvoiceDto("ext-2", orderId.toString())
        ]

        when:
        def result = service.reconcileFromExisting(localInvoice, externals)

        then:
        result.isPresent()
        result.get() instanceof InvoiceIssueResult.ReconciliationRequired
        1 * transactionBoundary.markReconciliationRequired(localInvoice)
    }

    def "should preserve existing mapping when local externalId matches among multiple externals"() {
        given:
        def localInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        def externals = [
                new FakturowniaGetInvoiceDto("ext-1", orderId.toString()),
                new FakturowniaGetInvoiceDto("ext-2", orderId.toString())
        ]

        when:
        def result = service.reconcileFromExisting(localInvoice, externals)

        then:
        result.isPresent()
        result.get() instanceof InvoiceIssueResult.Issued
        0 * transactionBoundary.markReconciliationRequired(_)
    }
}
