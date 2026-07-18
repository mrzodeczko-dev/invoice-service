package com.rzodeczko.infrastructure.reconciliation

import com.rzodeczko.application.exception.TaxSystemTemporaryException
import com.rzodeczko.application.port.input.InvoiceIssueResult
import com.rzodeczko.application.port.output.TaxSystemPort
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary
import com.rzodeczko.presentation.dto.FakturowniaGetInvoiceDto
import spock.lang.Specification
import spock.lang.Subject

class InvoiceIssueUnknownReconciliationJobSpec extends Specification {

    InvoiceTransactionBoundary transactionBoundary = Mock()
    TaxSystemPort taxSystemPort = Mock()
    InvoiceReconciliationService reconciliationService = Mock()

    @Subject
    InvoiceIssueUnknownReconciliationJob job = new InvoiceIssueUnknownReconciliationJob(
            transactionBoundary, taxSystemPort, reconciliationService
    )

    def validItems = [new InvoiceItem("Item", 1, BigDecimal.TEN)]

    def "should do nothing when no ISSUE_UNKNOWN invoices found"() {
        given:
        transactionBoundary.findIssueUnknownBatch(50) >> []

        when:
        job.reconcileIssueUnknownInvoices()

        then:
        0 * taxSystemPort._
        0 * reconciliationService._
    }

    def "should reconcile each invoice in batch"() {
        given:
        def id1 = UUID.randomUUID()
        def id2 = UUID.randomUUID()
        def orderId1 = UUID.randomUUID()
        def orderId2 = UUID.randomUUID()

        def inv1 = Invoice.restore(id1, orderId1, "TAX-1", "Buyer", null, InvoiceStatus.ISSUE_UNKNOWN, validItems, null)
        def inv2 = Invoice.restore(id2, orderId2, "TAX-2", "Buyer2", null, InvoiceStatus.ISSUE_UNKNOWN, validItems, null)

        transactionBoundary.findIssueUnknownBatch(50) >> [inv1, inv2]
        taxSystemPort.findByOrderId(orderId1.toString()) >> [new FakturowniaGetInvoiceDto("ext-1", orderId1.toString())]
        taxSystemPort.findByOrderId(orderId2.toString()) >> []

        when:
        job.reconcileIssueUnknownInvoices()

        then:
        1 * reconciliationService.reconcileFromExisting(inv1, _) >> Optional.of(new InvoiceIssueResult.Issued(id1))
        1 * reconciliationService.reconcileFromExisting(inv2, _) >> Optional.empty()
    }

    def "should continue processing batch when one invoice throws TaxSystemTemporaryException"() {
        given:
        def id1 = UUID.randomUUID()
        def id2 = UUID.randomUUID()
        def orderId1 = UUID.randomUUID()
        def orderId2 = UUID.randomUUID()

        def inv1 = Invoice.restore(id1, orderId1, "TAX-1", "Buyer", null, InvoiceStatus.ISSUE_UNKNOWN, validItems, null)
        def inv2 = Invoice.restore(id2, orderId2, "TAX-2", "Buyer2", null, InvoiceStatus.ISSUE_UNKNOWN, validItems, null)

        transactionBoundary.findIssueUnknownBatch(50) >> [inv1, inv2]
        taxSystemPort.findByOrderId(orderId1.toString()) >> { throw new TaxSystemTemporaryException("timeout") }
        taxSystemPort.findByOrderId(orderId2.toString()) >> []

        when:
        job.reconcileIssueUnknownInvoices()

        then:
        noExceptionThrown()
        1 * reconciliationService.reconcileFromExisting(inv2, []) >> Optional.empty()
    }

    def "should continue processing batch when one invoice throws unexpected exception"() {
        given:
        def id1 = UUID.randomUUID()
        def id2 = UUID.randomUUID()
        def orderId1 = UUID.randomUUID()
        def orderId2 = UUID.randomUUID()

        def inv1 = Invoice.restore(id1, orderId1, "TAX-1", "Buyer", null, InvoiceStatus.ISSUE_UNKNOWN, validItems, null)
        def inv2 = Invoice.restore(id2, orderId2, "TAX-2", "Buyer2", null, InvoiceStatus.ISSUE_UNKNOWN, validItems, null)

        transactionBoundary.findIssueUnknownBatch(50) >> [inv1, inv2]
        taxSystemPort.findByOrderId(orderId1.toString()) >> { throw new RuntimeException("unexpected") }
        taxSystemPort.findByOrderId(orderId2.toString()) >> []

        when:
        job.reconcileIssueUnknownInvoices()

        then:
        noExceptionThrown()
        1 * reconciliationService.reconcileFromExisting(inv2, []) >> Optional.empty()
    }
}
