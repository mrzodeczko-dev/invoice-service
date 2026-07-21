package com.rzodeczko.infrastructure.persistence.entity;

import com.rzodeczko.domain.model.InvoiceStatus;
import com.rzodeczko.infrastructure.configuration.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("integration-test")
class InvoiceEntityIT {

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void persist_shouldSaveInvoiceEntity() {
        // given
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal taxRate = BigDecimal.valueOf(23);
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(orderId)
                .taxId("123-456-78-90")
                .buyerName("John Doe")
                .status(InvoiceStatus.ISSUED)
                .externalId("ext-123")
                .items(List.of(
                        new InvoiceItemEmbeddable("Item1", 1, BigDecimal.TEN, taxRate)
                ))
                .build();

        // when
        testEntityManager.persistAndFlush(entity);
        testEntityManager.clear();

        // then
        InvoiceEntity found = testEntityManager.find(InvoiceEntity.class, id);
        assertThat(found).isNotNull();
        assertThat(found.getBuyerName()).isEqualTo("John Doe");
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getTaxRate()).isEqualByComparingTo(taxRate);
    }

    @Test
    void itemsCollection_shouldBePersisted() {
        // given
        UUID id = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .status(InvoiceStatus.DRAFT)
                .items(List.of(
                        new InvoiceItemEmbeddable("Item1", 1, BigDecimal.TEN, BigDecimal.valueOf(23)),
                        new InvoiceItemEmbeddable("Item2", 2, BigDecimal.valueOf(20), BigDecimal.valueOf(8)),
                        new InvoiceItemEmbeddable("Item3", 3, BigDecimal.valueOf(30), BigDecimal.valueOf(5))
                ))
                .build();

        // when
        testEntityManager.persistAndFlush(entity);
        testEntityManager.clear();

        // then
        InvoiceEntity found = testEntityManager.find(InvoiceEntity.class, id);
        assertThat(found).isNotNull();
        assertThat(found.getItems()).hasSize(3);
        assertThat(found.getItems().get(1).getTaxRate()).isEqualByComparingTo(BigDecimal.valueOf(8));
    }

    @Test
    void pdfContent_shouldNotBePersisted() {
        // given
        UUID id = UUID.randomUUID();
        byte[] pdfContent = new byte[]{1, 2, 3, 4, 5};
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Buyer")
                .status(InvoiceStatus.ISSUED)
                .pdfContent(pdfContent)
                .items(List.of())
                .build();

        // when
        testEntityManager.persistAndFlush(entity);
        testEntityManager.clear();

        // then
        InvoiceEntity found = testEntityManager.find(InvoiceEntity.class, id);
        assertThat(found).isNotNull();
        assertThat(found.getPdfContent()).isNull();
    }

    @Test
    void version_shouldBeUpdatedOnUpdate() {
        // given
        UUID id = UUID.randomUUID();
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .taxId("123")
                .buyerName("Original")
                .status(InvoiceStatus.DRAFT)
                .items(List.of())
                .build();

        testEntityManager.persistAndFlush(entity);
        Long version1 = entity.getVersion();

        // when
        entity.setBuyerName("Updated");
        testEntityManager.persistAndFlush(entity);
        Long version2 = entity.getVersion();

        // then
        assertThat(version2).isNotEqualTo(version1);
    }
}
