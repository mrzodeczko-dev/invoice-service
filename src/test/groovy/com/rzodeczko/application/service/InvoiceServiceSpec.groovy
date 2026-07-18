package com.rzodeczko.application.service

import com.rzodeczko.application.port.input.GenerateInvoiceCommand
import com.rzodeczko.application.port.input.ItemCommand
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.domain.repository.InvoiceRepository
import spock.lang.Specification
import spock.lang.Subject

class InvoiceServiceSpec extends Specification {

    InvoiceRepository invoiceRepository = Mock()

    @Subject
    InvoiceService invoiceService = new InvoiceService(invoiceRepository)

    // --- buildInvoice ---

    def "buildInvoice should create DRAFT invoice from command"() {
        given:
        def orderId = UUID.randomUUID()
        def command = new GenerateInvoiceCommand(
                orderId, "TAX-123", "Buyer Co",
                [new ItemCommand("Widget", 2, BigDecimal.TEN, new BigDecimal("23"))]
        )

        when:
        def invoice = invoiceService.buildInvoice(command)

        then:
        invoice.orderId == orderId
        invoice.taxId == "TAX-123"
        invoice.buyerName == "Buyer Co"
        invoice.status == InvoiceStatus.DRAFT
        invoice.items.size() == 1
        invoice.items[0].name() == "Widget"
        invoice.items[0].quantity() == 2
    }

    def "buildInvoice should use default tax rate when not specified"() {
        given:
        def command = new GenerateInvoiceCommand(
                UUID.randomUUID(), "TAX-123", "Buyer Co",
                [new ItemCommand("Widget", 1, BigDecimal.TEN, null)]
        )

        when:
        def invoice = invoiceService.buildInvoice(command)

        then:
        invoice.items[0].taxRate().intValue() == 23
    }

    def "buildInvoice should use explicit tax rate when specified"() {
        given:
        def command = new GenerateInvoiceCommand(
                UUID.randomUUID(), "TAX-123", "Buyer Co",
                [new ItemCommand("Widget", 1, BigDecimal.TEN, new BigDecimal("8"))]
        )

        when:
        def invoice = invoiceService.buildInvoice(command)

        then:
        invoice.items[0].taxRate().intValue() == 8
    }

    def "buildInvoice should handle multiple items"() {
        given:
        def command = new GenerateInvoiceCommand(
                UUID.randomUUID(), "TAX-123", "Buyer Co",
                [
                        new ItemCommand("A", 1, BigDecimal.TEN, null),
                        new ItemCommand("B", 2, BigDecimal.ONE, new BigDecimal("5")),
                        new ItemCommand("C", 3, new BigDecimal("99.99"), new BigDecimal("23"))
                ]
        )

        when:
        def invoice = invoiceService.buildInvoice(command)

        then:
        invoice.items.size() == 3
    }

    // --- existsByOrderId ---

    def "existsByOrderId should return true when repository returns true"() {
        given:
        def orderId = UUID.randomUUID()
        invoiceRepository.existsByOrderId(orderId) >> true

        expect:
        invoiceService.existsByOrderId(orderId)
    }

    def "existsByOrderId should return false when repository returns false"() {
        given:
        def orderId = UUID.randomUUID()
        invoiceRepository.existsByOrderId(orderId) >> false

        expect:
        !invoiceService.existsByOrderId(orderId)
    }

    // --- save and mark methods ---

    def "saveNewInvoice should delegate to repository"() {
        given:
        def invoice = Mock(Invoice)
        invoiceRepository.save(invoice) >> invoice

        when:
        def result = invoiceService.saveNewInvoice(invoice)

        then:
        result == invoice
    }

    def "markInvoiceAsIssued should call markAsIssued and save"() {
        given:
        def invoice = Mock(Invoice)

        when:
        invoiceService.markInvoiceAsIssued(invoice, "ext-1")

        then:
        1 * invoice.markAsIssued("ext-1")
        1 * invoiceRepository.save(invoice)
    }

    def "markInvoiceAsIssuing should call markAsIssuing and save"() {
        given:
        def invoice = Mock(Invoice)

        when:
        invoiceService.markInvoiceAsIssuing(invoice)

        then:
        1 * invoice.markAsIssuing()
        1 * invoiceRepository.save(invoice)
    }

    def "markInvoiceAsUnknown should call markAsIssueUnknown and save"() {
        given:
        def invoice = Mock(Invoice)

        when:
        invoiceService.markInvoiceAsUnknown(invoice)

        then:
        1 * invoice.markAsIssueUnknown()
        1 * invoiceRepository.save(invoice)
    }

    def "markIssueFailed should call markAsIssueFailed and save"() {
        given:
        def invoice = Mock(Invoice)

        when:
        invoiceService.markIssueFailed(invoice)

        then:
        1 * invoice.markAsIssueFailed()
        1 * invoiceRepository.save(invoice)
    }

    def "markReconciliationRequired should call markAsReconciliationRequired and save"() {
        given:
        def invoice = Mock(Invoice)

        when:
        invoiceService.markReconciliationRequired(invoice)

        then:
        1 * invoice.markAsReconciliationRequired()
        1 * invoiceRepository.save(invoice)
    }

    def "findIssueUnknownBatch should delegate to repository"() {
        given:
        def invoices = [Mock(Invoice), Mock(Invoice)]
        invoiceRepository.findIssueUnknownBatch(10) >> invoices

        when:
        def result = invoiceService.findIssueUnknownBatch(10)

        then:
        result.size() == 2
    }
}
