package com.rzodeczko.infrastructure.persistence.repository.adapter

import com.rzodeczko.application.exception.InvoiceConcurrentModificationException
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity
import com.rzodeczko.infrastructure.persistence.entity.InvoiceItemEmbeddable
import com.rzodeczko.infrastructure.persistence.mapper.InvoiceMapper
import com.rzodeczko.infrastructure.persistence.repository.JpaInvoiceRepository
import org.springframework.data.domain.PageRequest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import spock.lang.Specification
import spock.lang.Subject

class JpaInvoiceRepositoryAdapterSpec extends Specification {

    JpaInvoiceRepository jpaRepo = Mock()
    InvoiceMapper mapper = Mock()

    @Subject
    JpaInvoiceRepositoryAdapter adapter = new JpaInvoiceRepositoryAdapter(jpaRepo, mapper)

    def invoiceId = UUID.randomUUID()
    def orderId = UUID.randomUUID()
    def validItems = [new InvoiceItem("Item", 1, BigDecimal.TEN)]

    // --- save: new invoice ---

    def "save should create new entity when not found by id"() {
        given:
        def invoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        def entity = InvoiceEntity.builder().id(invoiceId).build()
        def savedEntity = InvoiceEntity.builder().id(invoiceId).build()
        def savedInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)

        jpaRepo.findById(invoiceId) >> Optional.empty()
        mapper.toEntity(invoice) >> entity
        jpaRepo.saveAndFlush(entity) >> savedEntity
        mapper.toDomain(savedEntity) >> savedInvoice

        when:
        def result = adapter.save(invoice)

        then:
        result == savedInvoice
    }

    // --- save: update existing ---

    def "save should update existing entity status and externalId"() {
        given:
        def invoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        def existingEntity = InvoiceEntity.builder()
                .id(invoiceId)
                .orderId(orderId)
                .status("DRAFT")
                .build()
        def savedInvoice = invoice

        jpaRepo.findById(invoiceId) >> Optional.of(existingEntity)
        jpaRepo.saveAndFlush(existingEntity) >> existingEntity
        mapper.toDomain(existingEntity) >> savedInvoice

        when:
        def result = adapter.save(invoice)

        then:
        existingEntity.status == "ISSUED"
        existingEntity.externalId == "ext-1"
        result == savedInvoice
    }

    // --- save: optimistic locking ---

    def "save should throw InvoiceConcurrentModificationException on optimistic lock failure"() {
        given:
        def invoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        def entity = InvoiceEntity.builder().id(invoiceId).build()

        jpaRepo.findById(invoiceId) >> Optional.empty()
        mapper.toEntity(invoice) >> entity
        jpaRepo.saveAndFlush(entity) >> { throw new ObjectOptimisticLockingFailureException("conflict", null) }

        when:
        adapter.save(invoice)

        then:
        thrown(InvoiceConcurrentModificationException)
    }

    // --- existsByOrderId ---

    def "existsByOrderId should delegate to jpa repository"() {
        given:
        jpaRepo.existsByOrderId(orderId) >> true

        expect:
        adapter.existsByOrderId(orderId)
    }

    // --- findById ---

    def "findById should return mapped domain when entity found"() {
        given:
        def entity = InvoiceEntity.builder().id(invoiceId).build()
        def invoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        jpaRepo.findById(invoiceId) >> Optional.of(entity)
        mapper.toDomain(entity) >> invoice

        when:
        def result = adapter.findById(invoiceId)

        then:
        result.isPresent()
        result.get() == invoice
    }

    def "findById should return empty when not found"() {
        given:
        jpaRepo.findById(invoiceId) >> Optional.empty()

        expect:
        adapter.findById(invoiceId).isEmpty()
    }

    // --- findPdfContent ---

    def "findPdfContent should return content when non-empty"() {
        given:
        def pdf = [1, 2, 3] as byte[]
        jpaRepo.findPdfContentById(invoiceId) >> Optional.of(pdf)

        when:
        def result = adapter.findPdfContent(invoiceId)

        then:
        result.isPresent()
        result.get() == pdf
    }

    def "findPdfContent should return empty when content is empty byte array"() {
        given:
        jpaRepo.findPdfContentById(invoiceId) >> Optional.of(new byte[0])

        expect:
        adapter.findPdfContent(invoiceId).isEmpty()
    }

    def "findPdfContent should return empty when not found"() {
        given:
        jpaRepo.findPdfContentById(invoiceId) >> Optional.empty()

        expect:
        adapter.findPdfContent(invoiceId).isEmpty()
    }

    // --- savePdfContent ---

    def "savePdfContent should delegate to jpa repository"() {
        given:
        def pdf = [4, 5, 6] as byte[]

        when:
        adapter.savePdfContent(invoiceId, pdf)

        then:
        1 * jpaRepo.updatePdfContent(invoiceId, pdf)
    }

    // --- findByExternalId ---

    def "findByExternalId should return mapped domain when found"() {
        given:
        def entity = InvoiceEntity.builder().id(invoiceId).externalId("ext-1").build()
        def invoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        jpaRepo.findByExternalId("ext-1") >> Optional.of(entity)
        mapper.toDomain(entity) >> invoice

        when:
        def result = adapter.findByExternalId("ext-1")

        then:
        result.isPresent()
        result.get().externalId == "ext-1"
    }

    def "findByExternalId should return empty when not found"() {
        given:
        jpaRepo.findByExternalId("nonexistent") >> Optional.empty()

        expect:
        adapter.findByExternalId("nonexistent").isEmpty()
    }

    // --- findByOrderId ---

    def "findByOrderId should return mapped domain when found"() {
        given:
        def entity = InvoiceEntity.builder().id(invoiceId).orderId(orderId).build()
        def invoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        jpaRepo.findByOrderId(orderId) >> Optional.of(entity)
        mapper.toDomain(entity) >> invoice

        when:
        def result = adapter.findByOrderId(orderId)

        then:
        result.isPresent()
        result.get().orderId == orderId
    }

    // --- findIssueUnknownBatch ---

    def "findIssueUnknownBatch should map all entities to domain"() {
        given:
        def entity1 = InvoiceEntity.builder().id(UUID.randomUUID()).build()
        def entity2 = InvoiceEntity.builder().id(UUID.randomUUID()).build()
        def inv1 = Mock(Invoice)
        def inv2 = Mock(Invoice)

        jpaRepo.findByStatusOrderByCreatedAtAsc(InvoiceStatus.ISSUE_UNKNOWN, PageRequest.of(0, 10)) >> [entity1, entity2]
        mapper.toDomain(entity1) >> inv1
        mapper.toDomain(entity2) >> inv2

        when:
        def result = adapter.findIssueUnknownBatch(10)

        then:
        result.size() == 2
        result == [inv1, inv2]
    }

    def "findIssueUnknownBatch should return empty list when no results"() {
        given:
        jpaRepo.findByStatusOrderByCreatedAtAsc(InvoiceStatus.ISSUE_UNKNOWN, PageRequest.of(0, 5)) >> []

        expect:
        adapter.findIssueUnknownBatch(5).isEmpty()
    }
}
