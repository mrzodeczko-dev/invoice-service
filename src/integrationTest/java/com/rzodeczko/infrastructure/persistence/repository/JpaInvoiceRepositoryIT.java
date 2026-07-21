package com.rzodeczko.infrastructure.persistence.repository;

import com.rzodeczko.domain.model.InvoiceStatus;
import com.rzodeczko.infrastructure.configuration.JpaAuditingConfig;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceItemEmbeddable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("integration-test")
class JpaInvoiceRepositoryIT {

    @Autowired
    private JpaInvoiceRepository jpaInvoiceRepository;

    @Test
    void save_shouldPersistInvoice() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(orderId)
                .taxId("123-456-78-90")
                .buyerName("John Doe")
                .status(InvoiceStatus.DRAFT)
                .items(List.of(
                        new InvoiceItemEmbeddable("Item1", 1, BigDecimal.TEN, BigDecimal.valueOf(23))
                ))
                .build();

        InvoiceEntity saved = jpaInvoiceRepository.save(entity);

        assertEquals(id, saved.getId());
        assertEquals(orderId, saved.getOrderId());
        assertEquals("John Doe", saved.getBuyerName());
    }

    @Test
    void findById_shouldReturnInvoice() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(orderId)
                .taxId("123-456-78-90")
                .buyerName("John Doe")
                .status(InvoiceStatus.ISSUED)
                .items(List.of())
                .build();
        jpaInvoiceRepository.save(entity);

        Optional<InvoiceEntity> found = jpaInvoiceRepository.findById(id);

        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getBuyerName());
    }

    @Test
    void existsByOrderId_shouldReturnTrueIfExists() {
        UUID orderId = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .taxId("123")
                .buyerName("Buyer")
                .status(InvoiceStatus.DRAFT)
                .items(List.of())
                .build();
        jpaInvoiceRepository.save(entity);

        boolean exists = jpaInvoiceRepository.existsByOrderId(orderId);

        assertTrue(exists);
    }

    @Test
    void existsByOrderId_shouldReturnFalseIfNotExists() {
        UUID orderId = UUID.randomUUID();

        boolean exists = jpaInvoiceRepository.existsByOrderId(orderId);

        assertFalse(exists);
    }

    @Test
    void findByExternalId_shouldReturnInvoice() {
        String externalId = "ext-123";
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .externalId(externalId)
                .status(InvoiceStatus.ISSUED)
                .items(List.of())
                .build();
        jpaInvoiceRepository.save(entity);

        Optional<InvoiceEntity> found = jpaInvoiceRepository.findByExternalId(externalId);

        assertTrue(found.isPresent());
        assertEquals(externalId, found.get().getExternalId());
    }

    @Test
    void findByExternalId_shouldReturnEmptyIfNotFound() {
        Optional<InvoiceEntity> found = jpaInvoiceRepository.findByExternalId("non-existent");

        assertTrue(found.isEmpty());
    }

    @Test
    @Transactional
    void findPdfContentById_shouldReturnPdfContent() {
        UUID id = UUID.randomUUID();
        byte[] pdfContent = new byte[]{1, 2, 3, 4, 5};
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .status(InvoiceStatus.ISSUED)
                .items(List.of())
                .build();
        jpaInvoiceRepository.save(entity);
        jpaInvoiceRepository.updatePdfContent(id, pdfContent);

        Optional<byte[]> found = jpaInvoiceRepository.findPdfContentById(id);

        assertTrue(found.isPresent());
        assertArrayEquals(pdfContent, found.get());
    }

    @Test
    @Transactional
    void updatePdfContent_shouldUpdatePdf() {
        UUID id = UUID.randomUUID();
        byte[] initialPdf = new byte[]{1, 2, 3};
        byte[] updatedPdf = new byte[]{4, 5, 6, 7};
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .status(InvoiceStatus.ISSUED)
                .pdfContent(initialPdf)
                .items(List.of())
                .build();
        jpaInvoiceRepository.save(entity);

        jpaInvoiceRepository.updatePdfContent(id, updatedPdf);

        Optional<byte[]> found = jpaInvoiceRepository.findPdfContentById(id);
        assertTrue(found.isPresent());
        assertArrayEquals(updatedPdf, found.get());
    }
}
