package com.rzodeczko.infrastructure.persistence.mapper;

import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.model.InvoiceItem;
import com.rzodeczko.domain.model.InvoiceStatus;
import com.rzodeczko.domain.vo.TaxRate;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceItemEmbeddable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InvoiceMapper.class)
@ActiveProfiles("integration-test")
@DisplayName("InvoiceMapper Integration Tests")
class InvoiceMapperIT {

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Test
    @DisplayName("Should convert domain Invoice to InvoiceEntity with all fields including taxRate")
    void toEntity_shouldConvertDomainToEntity() {
        // given
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal taxRate = BigDecimal.valueOf(23);
        List<InvoiceItem> items = List.of(
                new InvoiceItem("Item1", 1, BigDecimal.TEN, TaxRate.of(taxRate)),
                new InvoiceItem("Item2", 2, BigDecimal.valueOf(20), TaxRate.of(8))
        );
        Instant createdAt = Instant.parse("2026-04-27T22:00:00Z");
        Invoice domain = Invoice.restore(id, orderId, "123-456", "John Doe", "ext-123", InvoiceStatus.ISSUED, items, createdAt);

        // when
        InvoiceEntity entity = invoiceMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getOrderId()).isEqualTo(orderId);
        assertThat(entity.getTaxId()).isEqualTo("123-456");
        assertThat(entity.getBuyerName()).isEqualTo("John Doe");
        assertThat(entity.getExternalId()).isEqualTo("ext-123");
        assertThat(entity.getStatus()).isEqualTo("ISSUED");
        assertThat(entity.getItems()).hasSize(2);
        
        InvoiceItemEmbeddable firstItem = entity.getItems().get(0);
        assertThat(firstItem.getName()).isEqualTo("Item1");
        assertThat(firstItem.getQuantity()).isEqualTo(1);
        assertThat(firstItem.getTaxRate()).isEqualByComparingTo(taxRate);
    }

    @Test
    @DisplayName("Should convert InvoiceEntity to domain Invoice with all fields including TaxRate VO")
    void toDomain_shouldConvertEntityToDomain() {
        // given
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal taxRate = BigDecimal.valueOf(23);
        List<InvoiceItemEmbeddable> items = List.of(
                new InvoiceItemEmbeddable("Item1", 1, BigDecimal.TEN, taxRate)
        );
        Instant createdAt = Instant.parse("2026-04-27T22:00:00Z");
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(orderId)
                .taxId("123-456")
                .buyerName("John Doe")
                .externalId("ext-123")
                .status("ISSUED")
                .items(items)
                .createdAt(createdAt)
                .build();

        // when
        Invoice domain = invoiceMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getOrderId()).isEqualTo(orderId);
        assertThat(domain.getTaxId()).isEqualTo("123-456");
        assertThat(domain.getBuyerName()).isEqualTo("John Doe");
        assertThat(domain.getExternalId()).isEqualTo("ext-123");
        assertThat(domain.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(domain.getCreatedAt()).isEqualTo(createdAt);
        assertThat(domain.getItems()).hasSize(1);
        
        InvoiceItem firstItem = domain.getItems().get(0);
        assertThat(firstItem.name()).isEqualTo("Item1");
        assertThat(firstItem.taxRate().value()).isEqualByComparingTo(taxRate);
    }

    @Test
    @DisplayName("Round-trip conversion should preserve all data consistency using recursive comparison")
    void roundTripConversion_shouldPreserveData() {
        // given
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        List<InvoiceItem> items = List.of(
                new InvoiceItem("Item 1", 2, new BigDecimal("100.50"), TaxRate.of(23)),
                new InvoiceItem("Item 2", 1, new BigDecimal("50.00"), TaxRate.of(8))
        );
        Invoice originalDomain = Invoice.restore(
                id, orderId, "PL1234567890", "Test Buyer Sp. z o.o.",
                "FA/2023/001", InvoiceStatus.ISSUED, items,
                Instant.parse("2026-04-27T22:00:00Z")
        );

        // when
        InvoiceEntity entity = invoiceMapper.toEntity(originalDomain);
        Invoice restoredDomain = invoiceMapper.toDomain(entity);

        // then
        assertThat(restoredDomain)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(originalDomain);
    }
}
