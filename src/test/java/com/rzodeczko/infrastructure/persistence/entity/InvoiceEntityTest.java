package com.rzodeczko.infrastructure.persistence.entity;

import com.rzodeczko.domain.model.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvoiceEntity.
 */
class InvoiceEntityTest {

    @Test
    void builder_shouldCreateEntity() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String taxId = "123";
        String buyerName = "Buyer";
        InvoiceStatus status = InvoiceStatus.ISSUED;
        String externalId = "ext-1";
        List<InvoiceItemEmbeddable> items = List.of(
                InvoiceItemEmbeddable.builder()
                        .name("Item1")
                        .quantity(1)
                        .unitPrice(BigDecimal.ONE)
                        .build()
        );
        byte[] pdfContent = new byte[]{1, 2, 3};
        Long version = 1L;

        InvoiceEntity entity = InvoiceEntity.builder()
                .id(id)
                .orderId(orderId)
                .taxId(taxId)
                .buyerName(buyerName)
                .status(status)
                .externalId(externalId)
                .items(items)
                .pdfContent(pdfContent)
                .version(version)
                .build();

        assertEquals(id, entity.getId());
        assertEquals(orderId, entity.getOrderId());
        assertEquals(taxId, entity.getTaxId());
        assertEquals(buyerName, entity.getBuyerName());
        assertEquals(status, entity.getStatus());
        assertEquals(externalId, entity.getExternalId());
        assertEquals(items, entity.getItems());
        assertArrayEquals(pdfContent, entity.getPdfContent());
        assertEquals(version, entity.getVersion());
    }

    @Test
    void equals_shouldReturnTrueForSameId() {
        UUID id = UUID.randomUUID();
        InvoiceEntity entity1 = InvoiceEntity.builder().id(id).build();
        InvoiceEntity entity2 = InvoiceEntity.builder().id(id).build();
        assertEquals(entity1, entity2);
    }

    @Test
    void hashCode_shouldBeSameForSameId() {
        UUID id = UUID.randomUUID();
        InvoiceEntity entity1 = InvoiceEntity.builder().id(id).build();
        InvoiceEntity entity2 = InvoiceEntity.builder().id(id).build();
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }
}
