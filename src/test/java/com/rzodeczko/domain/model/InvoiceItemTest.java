package com.rzodeczko.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvoiceItem record.
 */
class InvoiceItemTest {
    @Test
    void constructor_shouldSetFields() {
        InvoiceItem item = new InvoiceItem("item", 2, BigDecimal.TEN);
        assertEquals("item", item.name());
        assertEquals(2, item.quantity());
        assertEquals(BigDecimal.TEN, item.unitPrice());
    }

    @Test
    void constructor_shouldThrowIfQuantityNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> new InvoiceItem("item", 0, BigDecimal.TEN));
    }

    @Test
    void constructor_shouldThrowIfUnitPriceNullOrNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> new InvoiceItem("item", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new InvoiceItem("item", 1, BigDecimal.ZERO));
    }
}

