package com.rzodeczko.infrastructure.persistence.repository.adapter

import com.rzodeczko.JpaIntegrationSpec
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.domain.vo.TaxRate
import com.rzodeczko.infrastructure.persistence.mapper.InvoiceMapper
import com.rzodeczko.infrastructure.persistence.repository.JpaInvoiceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import([InvoiceMapper, JpaInvoiceRepositoryAdapter])
class JpaInvoiceRepositoryAdapterIT extends JpaIntegrationSpec {

    @Autowired
    JpaInvoiceRepositoryAdapter adapter

    @Autowired
    JpaInvoiceRepository jpaInvoiceRepository

    def "should save new invoice and retrieve it by id"() {
        given:
        def invoice = createDraftInvoice()

        when:
        def saved = adapter.save(invoice)

        then:
        saved.id == invoice.id
        saved.orderId == invoice.orderId
        saved.buyerName == "Test Buyer"
        saved.status == InvoiceStatus.DRAFT
        saved.items.size() == 1

        and:
        def found = adapter.findById(invoice.id)
        found.isPresent()
        found.get().buyerName == "Test Buyer"
    }

    def "should update existing invoice status on save"() {
        given:
        def invoice = createDraftInvoice()
        adapter.save(invoice)

        and:
        invoice.markAsIssuing()

        when:
        def updated = adapter.save(invoice)

        then:
        updated.status == InvoiceStatus.ISSUING
    }

    def "should save and update externalId when marking as issued"() {
        given:
        def invoice = createDraftInvoice()
        adapter.save(invoice)

        and:
        invoice.markAsIssuing()
        adapter.save(invoice)
        invoice.markAsIssued("ext-999")

        when:
        def updated = adapter.save(invoice)

        then:
        updated.status == InvoiceStatus.ISSUED
        updated.externalId == "ext-999"
    }

    def "should check existence by orderId"() {
        given:
        def invoice = createDraftInvoice()
        adapter.save(invoice)

        expect:
        adapter.existsByOrderId(invoice.orderId)
        !adapter.existsByOrderId(UUID.randomUUID())
    }

    def "should find by externalId"() {
        given:
        def invoice = createDraftInvoice()
        adapter.save(invoice)
        invoice.markAsIssuing()
        adapter.save(invoice)
        invoice.markAsIssued("ext-abc")
        adapter.save(invoice)

        when:
        def found = adapter.findByExternalId("ext-abc")

        then:
        found.isPresent()
        found.get().id == invoice.id
    }

    def "should find by orderId"() {
        given:
        def invoice = createDraftInvoice()
        adapter.save(invoice)

        when:
        def found = adapter.findByOrderId(invoice.orderId)

        then:
        found.isPresent()
        found.get().id == invoice.id
    }

    def "should save and retrieve pdf content"() {
        given:
        def invoice = createDraftInvoice()
        adapter.save(invoice)
        byte[] pdf = [1, 2, 3, 4, 5] as byte[]

        when:
        adapter.savePdfContent(invoice.id, pdf)

        then:
        def found = adapter.findPdfContent(invoice.id)
        found.isPresent()
        found.get() == pdf
    }

    def "should return empty pdf content when not set"() {
        given:
        def invoice = createDraftInvoice()
        adapter.save(invoice)

        expect:
        adapter.findPdfContent(invoice.id).isEmpty()
    }

    def "should find issue unknown batch ordered by createdAt"() {
        given:
        def invoice1 = createInvoiceWithStatus(InvoiceStatus.ISSUE_UNKNOWN)
        def invoice2 = createInvoiceWithStatus(InvoiceStatus.ISSUE_UNKNOWN)
        def invoice3 = createInvoiceWithStatus(InvoiceStatus.DRAFT)

        adapter.save(invoice1)
        adapter.save(invoice2)
        adapter.save(invoice3)

        when:
        def batch = adapter.findIssueUnknownBatch(10)

        then:
        batch.size() == 2
        batch.every { it.status == InvoiceStatus.ISSUE_UNKNOWN }
    }

    def "should limit issue unknown batch size"() {
        given:
        3.times {
            adapter.save(createInvoiceWithStatus(InvoiceStatus.ISSUE_UNKNOWN))
        }

        when:
        def batch = adapter.findIssueUnknownBatch(2)

        then:
        batch.size() == 2
    }

    private Invoice createDraftInvoice() {
        def items = [new InvoiceItem("Item1", 2, BigDecimal.TEN, TaxRate.of(23))]
        new Invoice(UUID.randomUUID(), UUID.randomUUID(), "PL1234567890", "Test Buyer", items)
    }

    private Invoice createInvoiceWithStatus(InvoiceStatus targetStatus) {
        def invoice = createDraftInvoice()
        switch (targetStatus) {
            case InvoiceStatus.ISSUE_UNKNOWN:
                invoice.markAsIssuing()
                invoice.markAsIssueUnknown()
                break
            case InvoiceStatus.ISSUING:
                invoice.markAsIssuing()
                break
            case InvoiceStatus.ISSUED:
                invoice.markAsIssuing()
                invoice.markAsIssued("ext-" + UUID.randomUUID())
                break
        }
        return invoice
    }
}
