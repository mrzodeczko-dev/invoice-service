package com.rzodeczko.infrastructure.persistence.mapper

import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.domain.vo.TaxRate
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity
import com.rzodeczko.infrastructure.persistence.entity.InvoiceItemEmbeddable
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class InvoiceMapperSpec extends Specification {

    @Subject
    InvoiceMapper mapper = new InvoiceMapper()

    def invoiceId = UUID.randomUUID()
    def orderId = UUID.randomUUID()

    // --- toEntity ---

    def "toEntity should map all domain fields to entity"() {
        given:
        def now = Instant.now()
        def invoice = Invoice.restore(
                invoiceId, orderId, "TAX-1", "Buyer Co", "ext-1",
                InvoiceStatus.ISSUED,
                [new InvoiceItem("Widget", 2, new BigDecimal("19.99"), TaxRate.of(23))],
                now
        )

        when:
        def entity = mapper.toEntity(invoice)

        then:
        entity.id == invoiceId
        entity.orderId == orderId
        entity.taxId == "TAX-1"
        entity.buyerName == "Buyer Co"
        entity.status == "ISSUED"
        entity.externalId == "ext-1"
        entity.createdAt == now
        entity.items.size() == 1
        entity.items[0].name == "Widget"
        entity.items[0].quantity == 2
        entity.items[0].unitPrice == new BigDecimal("19.99")
        entity.items[0].taxRate == new BigDecimal("23")
    }

    def "toEntity should handle multiple items"() {
        given:
        def invoice = Invoice.restore(
                invoiceId, orderId, "TAX-1", "Buyer", "ext-1",
                InvoiceStatus.ISSUED,
                [
                        new InvoiceItem("A", 1, BigDecimal.TEN, TaxRate.of(23)),
                        new InvoiceItem("B", 3, BigDecimal.ONE, TaxRate.of(8))
                ],
                null
        )

        when:
        def entity = mapper.toEntity(invoice)

        then:
        entity.items.size() == 2
    }

    def "toEntity should map null externalId for DRAFT invoice"() {
        given:
        def invoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer",
                [new InvoiceItem("Item", 1, BigDecimal.TEN)])

        when:
        def entity = mapper.toEntity(invoice)

        then:
        entity.externalId == null
        entity.status == "DRAFT"
    }

    // --- toDomain ---

    def "toDomain should map all entity fields to domain"() {
        given:
        def now = Instant.now()
        def entity = InvoiceEntity.builder()
                .id(invoiceId)
                .orderId(orderId)
                .taxId("TAX-1")
                .buyerName("Buyer Co")
                .status("ISSUED")
                .externalId("ext-1")
                .createdAt(now)
                .items([new InvoiceItemEmbeddable("Widget", 2, new BigDecimal("19.99"), new BigDecimal("23"))])
                .build()

        when:
        def invoice = mapper.toDomain(entity)

        then:
        invoice.id == invoiceId
        invoice.orderId == orderId
        invoice.taxId == "TAX-1"
        invoice.buyerName == "Buyer Co"
        invoice.status == InvoiceStatus.ISSUED
        invoice.externalId == "ext-1"
        invoice.createdAt == now
        invoice.items.size() == 1
        invoice.items[0].name() == "Widget"
        invoice.items[0].quantity() == 2
        invoice.items[0].unitPrice() == new BigDecimal("19.99")
        invoice.items[0].taxRate().intValue() == 23
    }

    def "toDomain should map DRAFT entity without externalId"() {
        given:
        def entity = InvoiceEntity.builder()
                .id(invoiceId)
                .orderId(orderId)
                .taxId("TAX-1")
                .buyerName("Buyer")
                .status("DRAFT")
                .externalId(null)
                .items([new InvoiceItemEmbeddable("Item", 1, BigDecimal.TEN, new BigDecimal("23"))])
                .build()

        when:
        def invoice = mapper.toDomain(entity)

        then:
        invoice.status == InvoiceStatus.DRAFT
        invoice.externalId == null
    }

    // --- round-trip ---

    def "toEntity then toDomain should preserve all data"() {
        given:
        def original = Invoice.restore(
                invoiceId, orderId, "TAX-1", "Buyer", "ext-1",
                InvoiceStatus.ISSUED,
                [new InvoiceItem("Widget", 2, BigDecimal.TEN, TaxRate.of(23))],
                Instant.parse("2026-01-15T10:30:00Z")
        )

        when:
        def entity = mapper.toEntity(original)
        def restored = mapper.toDomain(entity)

        then:
        restored.id == original.id
        restored.orderId == original.orderId
        restored.taxId == original.taxId
        restored.buyerName == original.buyerName
        restored.status == original.status
        restored.externalId == original.externalId
        restored.createdAt == original.createdAt
        restored.items.size() == original.items.size()
        restored.items[0].name() == original.items[0].name()
        restored.items[0].quantity() == original.items[0].quantity()
        restored.items[0].unitPrice() == original.items[0].unitPrice()
        restored.items[0].taxRate().intValue() == original.items[0].taxRate().intValue()
    }
}
