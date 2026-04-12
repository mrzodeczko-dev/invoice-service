package com.rzodeczko.infrastructure.persistence.mapper;

import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.model.InvoiceItem;
import com.rzodeczko.domain.model.InvoiceStatus;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceItemEmbeddable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvoiceMapper.
 */
class InvoiceMapperTest {
    private final InvoiceMapper mapper = new InvoiceMapper();

    @Test
    void toEntity_shouldMapDomainToEntity() {
        Invoice invoice = new Invoice(UUID.randomUUID(), UUID.randomUUID(), "1234567890", "Buyer",
                List.of(new InvoiceItem("item", 2, BigDecimal.TEN)));
        InvoiceEntity entity = mapper.toEntity(invoice);
        assertEquals(invoice.getId(), entity.getId());
        assertEquals(invoice.getOrderId(), entity.getOrderId());
        assertEquals(invoice.getBuyerName(), entity.getBuyerName());
        assertEquals(invoice.getTaxId(), entity.getTaxId());
        assertEquals(invoice.getStatus().name(), entity.getStatus());
        assertEquals(invoice.getItems().size(), entity.getItems().size());
    }

    @Test
    void toDomain_shouldMapEntityToDomain() {
        InvoiceItemEmbeddable item = new InvoiceItemEmbeddable("item", 2, BigDecimal.TEN);
        InvoiceEntity entity = InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .buyerName("Buyer")
                .taxId("1234567890")
                .status(InvoiceStatus.DRAFT.name())
                .externalId("ext-1")
                .items(List.of(item))
                .build();
        Invoice invoice = mapper.toDomain(entity);
        assertEquals(entity.getId(), invoice.getId());
        assertEquals(entity.getOrderId(), invoice.getOrderId());
        assertEquals(entity.getBuyerName(), invoice.getBuyerName());
        assertEquals(entity.getTaxId(), invoice.getTaxId());
        assertEquals(entity.getStatus(), invoice.getStatus().name());
        assertEquals(entity.getItems().size(), invoice.getItems().size());
    }
}

