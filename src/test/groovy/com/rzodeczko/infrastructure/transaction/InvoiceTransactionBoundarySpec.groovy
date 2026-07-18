package com.rzodeczko.infrastructure.transaction

import com.rzodeczko.application.service.InvoiceService
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.repository.InvoiceRepository
import spock.lang.Specification
import spock.lang.Subject

class InvoiceTransactionBoundarySpec extends Specification {

    InvoiceService invoiceService = Mock()
    InvoiceRepository invoiceRepository = Mock()

    @Subject
    InvoiceTransactionBoundary boundary = new InvoiceTransactionBoundary(invoiceService, invoiceRepository)

    def invoiceId = UUID.randomUUID()
    def orderId = UUID.randomUUID()

    def "existsByOrderId should delegate to invoiceService"() {
        given:
        invoiceService.existsByOrderId(orderId) >> true

        expect:
        boundary.existsByOrderId(orderId)
    }

    def "findByOrderId should delegate to repository"() {
        given:
        def invoice = Mock(Invoice)
        invoiceRepository.findByOrderId(orderId) >> Optional.of(invoice)

        when:
        def result = boundary.findByOrderId(orderId)

        then:
        result.isPresent()
        result.get() == invoice
    }

    def "saveNewInvoice should delegate to invoiceService"() {
        given:
        def invoice = Mock(Invoice)
        invoiceService.saveNewInvoice(invoice) >> invoice

        when:
        def result = boundary.saveNewInvoice(invoice)

        then:
        result == invoice
    }

    def "markInvoiceAsIssued should delegate to invoiceService"() {
        given:
        def invoice = Mock(Invoice)

        when:
        boundary.markInvoiceAsIssued(invoice, "ext-1")

        then:
        1 * invoiceService.markInvoiceAsIssued(invoice, "ext-1")
    }

    def "markAsIssuing should delegate to invoiceService"() {
        given:
        def invoice = Mock(Invoice)

        when:
        boundary.markAsIssuing(invoice)

        then:
        1 * invoiceService.markInvoiceAsIssuing(invoice)
    }

    def "findById should delegate to repository"() {
        given:
        def invoice = Mock(Invoice)
        invoiceRepository.findById(invoiceId) >> Optional.of(invoice)

        when:
        def result = boundary.findById(invoiceId)

        then:
        result.isPresent()
    }

    def "findById should return empty when not found"() {
        given:
        invoiceRepository.findById(invoiceId) >> Optional.empty()

        expect:
        boundary.findById(invoiceId).isEmpty()
    }

    def "findPdfContent should delegate to repository"() {
        given:
        def pdf = [1, 2, 3] as byte[]
        invoiceRepository.findPdfContent(invoiceId) >> Optional.of(pdf)

        when:
        def result = boundary.findPdfContent(invoiceId)

        then:
        result.isPresent()
        result.get() == pdf
    }

    def "savePdfContent should delegate to repository"() {
        given:
        def pdf = [4, 5, 6] as byte[]

        when:
        boundary.savePdfContent(invoiceId, pdf)

        then:
        1 * invoiceRepository.savePdfContent(invoiceId, pdf)
    }

    def "findByExternalId should delegate to repository"() {
        given:
        def invoice = Mock(Invoice)
        invoiceRepository.findByExternalId("ext-1") >> Optional.of(invoice)

        when:
        def result = boundary.findByExternalId("ext-1")

        then:
        result.isPresent()
    }

    def "markIssueUnknown should delegate to invoiceService"() {
        given:
        def invoice = Mock(Invoice)

        when:
        boundary.markIssueUnknown(invoice)

        then:
        1 * invoiceService.markInvoiceAsUnknown(invoice)
    }

    def "markIssueFailed should delegate to invoiceService"() {
        given:
        def invoice = Mock(Invoice)

        when:
        boundary.markIssueFailed(invoice)

        then:
        1 * invoiceService.markIssueFailed(invoice)
    }

    def "markReconciliationRequired should delegate to invoiceService"() {
        given:
        def invoice = Mock(Invoice)

        when:
        boundary.markReconciliationRequired(invoice)

        then:
        1 * invoiceService.markReconciliationRequired(invoice)
    }

    def "findIssueUnknownBatch should delegate to invoiceService"() {
        given:
        def invoices = [Mock(Invoice), Mock(Invoice)]
        invoiceService.findIssueUnknownBatch(50) >> invoices

        when:
        def result = boundary.findIssueUnknownBatch(50)

        then:
        result.size() == 2
    }
}
