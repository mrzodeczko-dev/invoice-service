package com.rzodeczko.infrastructure.usecase

import com.rzodeczko.application.exception.ExternalTaxSystemException
import com.rzodeczko.application.port.output.TaxSystemPort
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary
import spock.lang.Specification
import spock.lang.Subject

class InvoiceWebhookUseCaseImplSpec extends Specification {

    InvoiceTransactionBoundary transactionBoundary = Mock()
    TaxSystemPort taxSystemPort = Mock()

    @Subject
    InvoiceWebhookUseCaseImpl useCase = new InvoiceWebhookUseCaseImpl(transactionBoundary, taxSystemPort)

    def invoiceId = UUID.randomUUID()
    def orderId = UUID.randomUUID()
    def validItems = [new InvoiceItem("Item", 1, BigDecimal.TEN)]

    def "should refresh cached PDF when invoice and existing PDF found"() {
        given:
        def invoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        def freshPdf = [7, 8, 9] as byte[]
        transactionBoundary.findByExternalId("ext-1") >> Optional.of(invoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.of([1, 2, 3] as byte[])
        taxSystemPort.getPdf("ext-1") >> freshPdf

        when:
        useCase.handle("ext-1")

        then:
        1 * transactionBoundary.savePdfContent(invoiceId, freshPdf)
    }

    def "should do nothing when invoice not found"() {
        given:
        transactionBoundary.findByExternalId("ext-unknown") >> Optional.empty()

        when:
        useCase.handle("ext-unknown")

        then:
        0 * transactionBoundary.savePdfContent(_, _)
        0 * taxSystemPort.getPdf(_)
    }

    def "should do nothing when no cached PDF exists"() {
        given:
        def invoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        transactionBoundary.findByExternalId("ext-1") >> Optional.of(invoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.empty()

        when:
        useCase.handle("ext-1")

        then:
        0 * taxSystemPort.getPdf(_)
        0 * transactionBoundary.savePdfContent(_, _)
    }

    def "should not save when tax system throws ExternalTaxSystemException"() {
        given:
        def invoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        transactionBoundary.findByExternalId("ext-1") >> Optional.of(invoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.of([1] as byte[])
        taxSystemPort.getPdf("ext-1") >> { throw new ExternalTaxSystemException("fail") }

        when:
        useCase.handle("ext-1")

        then:
        0 * transactionBoundary.savePdfContent(_, _)
    }

    def "should not save when fetched PDF is empty"() {
        given:
        def invoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        transactionBoundary.findByExternalId("ext-1") >> Optional.of(invoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.of([1] as byte[])
        taxSystemPort.getPdf("ext-1") >> new byte[0]

        when:
        useCase.handle("ext-1")

        then:
        0 * transactionBoundary.savePdfContent(_, _)
    }

    def "should not save when fetched PDF is null"() {
        given:
        def invoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        transactionBoundary.findByExternalId("ext-1") >> Optional.of(invoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.of([1] as byte[])
        taxSystemPort.getPdf("ext-1") >> null

        when:
        useCase.handle("ext-1")

        then:
        0 * transactionBoundary.savePdfContent(_, _)
    }
}
