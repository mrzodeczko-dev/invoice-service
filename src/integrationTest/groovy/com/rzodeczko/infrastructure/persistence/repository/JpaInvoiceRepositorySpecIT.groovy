package com.rzodeczko.infrastructure.persistence.repository

import com.rzodeczko.JpaIntegrationSpec
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity
import com.rzodeczko.infrastructure.persistence.entity.InvoiceItemEmbeddable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

class JpaInvoiceRepositorySpecIT extends JpaIntegrationSpec {

    @Autowired
    JpaInvoiceRepository jpaInvoiceRepository

    def "should persist and retrieve an invoice"() {
        given:
        def id = UUID.randomUUID()
        def orderId = UUID.randomUUID()
        def entity = InvoiceEntity.builder()
                .id(id)
                .orderId(orderId)
                .taxId("123-456-78-90")
                .buyerName("John Doe")
                .status("DRAFT")
                .items([new InvoiceItemEmbeddable("Item1", 1, BigDecimal.TEN, BigDecimal.valueOf(23))])
                .build()

        when:
        def saved = jpaInvoiceRepository.save(entity)

        then:
        saved.id == id
        saved.orderId == orderId
        saved.buyerName == "John Doe"
    }

    def "should find invoice by id"() {
        given:
        def id = UUID.randomUUID()
        def entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123-456-78-90")
                .buyerName("John Doe")
                .status("ISSUED")
                .items([])
                .build()
        jpaInvoiceRepository.save(entity)

        when:
        def found = jpaInvoiceRepository.findById(id)

        then:
        found.isPresent()
        found.get().buyerName == "John Doe"
    }

    def "should check existence by orderId"() {
        given:
        def orderId = UUID.randomUUID()
        def entity = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .taxId("123")
                .buyerName("Buyer")
                .status("DRAFT")
                .items([])
                .build()
        jpaInvoiceRepository.save(entity)

        expect:
        jpaInvoiceRepository.existsByOrderId(orderId)
        !jpaInvoiceRepository.existsByOrderId(UUID.randomUUID())
    }

    def "should find invoice by externalId"() {
        given:
        def externalId = "ext-123"
        def entity = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .externalId(externalId)
                .status("ISSUED")
                .items([])
                .build()
        jpaInvoiceRepository.save(entity)

        when:
        def found = jpaInvoiceRepository.findByExternalId(externalId)

        then:
        found.isPresent()
        found.get().externalId == externalId
    }

    def "should return empty when externalId not found"() {
        expect:
        jpaInvoiceRepository.findByExternalId("non-existent").isEmpty()
    }

    @Transactional
    def "should update and retrieve pdf content"() {
        given:
        def id = UUID.randomUUID()
        byte[] pdfContent = [1, 2, 3, 4, 5] as byte[]
        def entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .status("ISSUED")
                .items([])
                .build()
        jpaInvoiceRepository.save(entity)

        when:
        jpaInvoiceRepository.updatePdfContent(id, pdfContent)
        def found = jpaInvoiceRepository.findPdfContentById(id)

        then:
        found.isPresent()
        found.get() == pdfContent
    }
}
