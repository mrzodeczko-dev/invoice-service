package com.rzodeczko.infrastructure.persistence.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@AutoConfigureTestEntityManager
@ActiveProfiles("integration-test")
@Transactional
class InvoiceEntityIT {

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void persist_shouldSaveInvoiceEntity() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(orderId)
                .taxId("123-456-78-90")
                .buyerName("John Doe")
                .status("ISSUED")
                .externalId("ext-123")
                .items(List.of(
                        new InvoiceItemEmbeddable("Item1", 1, BigDecimal.TEN)
                ))
                .build();

        testEntityManager.persistAndFlush(entity);
        testEntityManager.clear();

        InvoiceEntity found = testEntityManager.find(InvoiceEntity.class, id);
        assertNotNull(found);
        assertEquals("John Doe", found.getBuyerName());
        assertEquals(1, found.getItems().size());
    }

    @Test
    void itemsCollection_shouldBePersisted() {
        UUID id = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .status("DRAFT")
                .items(List.of(
                        new InvoiceItemEmbeddable("Item1", 1, BigDecimal.TEN),
                        new InvoiceItemEmbeddable("Item2", 2, BigDecimal.valueOf(20)),
                        new InvoiceItemEmbeddable("Item3", 3, BigDecimal.valueOf(30))
                ))
                .build();

        testEntityManager.persistAndFlush(entity);
        testEntityManager.clear();

        InvoiceEntity found = testEntityManager.find(InvoiceEntity.class, id);
        assertNotNull(found);
        assertEquals(3, found.getItems().size());
    }

    @Test
    void pdfContent_shouldNotBePersisted() {
        UUID id = UUID.randomUUID();
        byte[] pdfContent = new byte[]{1, 2, 3, 4, 5};
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .status("ISSUED")
                .pdfContent(pdfContent)
                .items(List.of())
                .build();

        testEntityManager.persistAndFlush(entity);
        testEntityManager.clear();

        InvoiceEntity found = testEntityManager.find(InvoiceEntity.class, id);
        assertNotNull(found);
        assertNull(found.getPdfContent());
    }

    @Test
    void version_shouldBeUpdatedOnUpdate() {
        UUID id = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Original")
                .status("DRAFT")
                .items(List.of())
                .build();

        testEntityManager.persistAndFlush(entity);
        Long version1 = entity.getVersion();

        entity.setBuyerName("Updated");
        testEntityManager.persistAndFlush(entity);
        Long version2 = entity.getVersion();

        assertNotEquals(version1, version2);
    }
}
