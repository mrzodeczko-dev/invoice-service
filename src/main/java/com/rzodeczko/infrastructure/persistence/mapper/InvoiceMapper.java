package com.rzodeczko.infrastructure.persistence.mapper;


import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.model.InvoiceItem;
import com.rzodeczko.domain.model.InvoiceStatus;
import com.rzodeczko.domain.vo.TaxRate;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceItemEmbeddable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceMapper {
    public InvoiceEntity toEntity(Invoice domain) {
        List<InvoiceItemEmbeddable> entityItems = domain
                .getItems()
                .stream()
                .map(i -> new InvoiceItemEmbeddable(i.name(), i.quantity(), i.unitPrice(), i.taxRate().value()))
                .toList();

        return InvoiceEntity
                .builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .buyerName(domain.getBuyerName())
                .taxId(domain.getTaxId())
                .status(domain.getStatus().name())
                .externalId(domain.getExternalId())
                .items(entityItems)
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public Invoice toDomain(InvoiceEntity entity) {
        List<InvoiceItem> items = entity
                .getItems()
                .stream()
                .map(i -> new InvoiceItem(i.getName(), i.getQuantity(), i.getUnitPrice(), TaxRate.of(i.getTaxRate())))
                .toList();

        return Invoice.restore(
                entity.getId(),
                entity.getOrderId(),
                entity.getTaxId(),
                entity.getBuyerName(),
                entity.getExternalId(),
                InvoiceStatus.valueOf(entity.getStatus()),
                items,
                entity.getCreatedAt()
        );
    }
}
