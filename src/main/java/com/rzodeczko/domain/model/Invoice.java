package com.rzodeczko.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;


public class Invoice {
    private final UUID id;
    private final UUID orderId;
    private final String taxId;
    private final String buyerName;
    private String externalId;
    private InvoiceStatus status;
    private final List<InvoiceItem> items;

    public Invoice(UUID id, UUID orderId, String taxId, String buyerName, List<InvoiceItem> items) {
        this.id = id;
        this.orderId = orderId;
        this.taxId = taxId;
        this.buyerName = buyerName;
        this.items = items != null ? List.copyOf(items) : Collections.emptyList();
        this.status = InvoiceStatus.DRAFT;
        validate();
    }

    private Invoice(
            UUID id,
            UUID orderId,
            String taxId,
            String buyerName,
            String externalId,
            InvoiceStatus status,
            List<InvoiceItem> items
    ) {
        this.id = id;
        this.orderId = orderId;
        this.taxId = taxId;
        this.buyerName = buyerName;
        this.externalId = externalId;
        this.status = status;
        this.items = items != null ? List.copyOf(items) : Collections.emptyList();
    }


    public static Invoice restore(
            UUID id,
            UUID orderId,
            String taxId,
            String buyerName,
            String externalId,
            InvoiceStatus status,
            List<InvoiceItem> items
    ) {
        return new Invoice(id, orderId, taxId, buyerName, externalId, status, items);
    }

    public void markAsIssued(String externalId) {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Invoice is already marked as issued");
        }
        this.externalId = externalId;
        this.status = InvoiceStatus.ISSUED;
    }

    public boolean isIssued() {
        return this.status == InvoiceStatus.ISSUED && externalId != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getExternalId() {
        return externalId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    private void validate() {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Invoice must contain at least one item");
        }

        if (taxId == null || taxId.isBlank()) {
            throw new IllegalArgumentException("Tax ID required");
        }

        if (buyerName == null || buyerName.isBlank()) {
            throw new IllegalArgumentException("Buyer name required");
        }
    }
}
