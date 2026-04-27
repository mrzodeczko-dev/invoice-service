package com.rzodeczko.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an invoice in the system.
 */
public class Invoice {
    private final UUID id;
    private final UUID orderId;
    private final String taxId;
    private final String buyerName;
    private String externalId;
    private InvoiceStatus status;
    private final List<InvoiceItem> items;
    private final Instant createdAt;

    public Invoice(UUID id, UUID orderId, String taxId, String buyerName, List<InvoiceItem> items) {
        this(id, orderId, taxId, buyerName, null, InvoiceStatus.DRAFT, items, null);
    }

    private Invoice(
            UUID id,
            UUID orderId,
            String taxId,
            String buyerName,
            String externalId,
            InvoiceStatus status,
            List<InvoiceItem> items,
            Instant createdAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.taxId = taxId;
        this.buyerName = buyerName;
        this.externalId = externalId;
        this.status = status;
        this.items = items != null ? List.copyOf(items) : Collections.emptyList();
        this.createdAt = createdAt;
        validate();
    }

    public static Invoice restore(
            UUID id,
            UUID orderId,
            String taxId,
            String buyerName,
            String externalId,
            InvoiceStatus status,
            List<InvoiceItem> items,
            Instant createdAt
    ) {
        return new Invoice(id, orderId, taxId, buyerName, externalId, status, items, createdAt);
    }

    public void markAsIssuing() {
        if (this.status == InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Invoice cannot be marked as issuing from status " + this.status);
        }
        this.status = InvoiceStatus.ISSUING;
    }

    public void markAsIssued(String externalId) {
        if (this.status != InvoiceStatus.DRAFT && this.status != InvoiceStatus.ISSUING) {
            throw new IllegalStateException("Invoice cannot be marked as issued from status " + this.status);
        }
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId must not be blank");
        }

        this.externalId = externalId;
        this.status = InvoiceStatus.ISSUED;
    }

    public boolean isReconciliationRequired() {
        return status == InvoiceStatus.RECONCILIATION_REQUIRED;
    }

    public void markAsIssueUnknown() {
        this.status = InvoiceStatus.ISSUE_UNKNOWN;
    }

    public boolean isIssued() {
        return this.status == InvoiceStatus.ISSUED;
    }

    public boolean isIssuing() {
        return this.status == InvoiceStatus.ISSUING;
    }

    public boolean isDraft() {
        return this.status == InvoiceStatus.DRAFT;
    }

    public boolean isIssueUnknown() {
        return this.status == InvoiceStatus.ISSUE_UNKNOWN;
    }

    private void validate() {
        if (id == null) {
            throw new IllegalArgumentException("Invoice ID required");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID required");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Invoice must contain at least one item");
        }
        if (taxId == null || taxId.isBlank()) {
            throw new IllegalArgumentException("Tax ID required");
        }
        if (buyerName == null || buyerName.isBlank()) {
            throw new IllegalArgumentException("Buyer name required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Invoice status required");
        }
        if (status == InvoiceStatus.ISSUED && (externalId == null || externalId.isBlank())) {
            throw new IllegalStateException("Issued invoice must have externalId");
        }
        if (status != InvoiceStatus.ISSUED && externalId != null) {
            throw new IllegalStateException("Only issued invoice can have externalId");
        }
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markAsIssueFailed() {
        this.status = InvoiceStatus.ISSUE_FAILED;
    }

    public void markAsReconciliationRequired() {
        this.status = InvoiceStatus.RECONCILIATION_REQUIRED;
        externalId = null;
    }
}
