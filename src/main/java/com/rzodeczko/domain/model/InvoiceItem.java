package com.rzodeczko.domain.model;

import java.math.BigDecimal;

public record InvoiceItem(
        String name,
        int quantity,
        BigDecimal unitPrice) {
    public InvoiceItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }
}
