package com.rzodeczko.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Invoice domain model.
 */
class InvoiceTest {
    @Test
    void constructor_shouldSetFieldsAndStatusDraft() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String taxId = "1234567890";
        String buyerName = "Buyer";
        List<InvoiceItem> items = List.of(new InvoiceItem("item", 2, BigDecimal.TEN));
        Invoice invoice = new Invoice(id, orderId, taxId, buyerName, items);
        assertEquals(id, invoice.getId());
        assertEquals(orderId, invoice.getOrderId());
        assertEquals(taxId, invoice.getTaxId());
        assertEquals(buyerName, invoice.getBuyerName());
        assertEquals(items, invoice.getItems());
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
    }

    @Test
    void markAsIssued_shouldChangeStatusAndSetExternalId() {
        Invoice invoice = new Invoice(UUID.randomUUID(), UUID.randomUUID(), "1234567890", "Buyer",
                List.of(new InvoiceItem("item", 2, BigDecimal.TEN)));
        invoice.markAsIssued("ext-1");
        assertEquals(InvoiceStatus.ISSUED, invoice.getStatus());
        assertEquals("ext-1", invoice.getExternalId());
        assertTrue(invoice.isIssued());
    }

    @Test
    void markAsIssued_shouldThrowIfAlreadyIssued() {
        Invoice invoice = new Invoice(UUID.randomUUID(), UUID.randomUUID(), "1234567890", "Buyer",
                List.of(new InvoiceItem("item", 2, BigDecimal.TEN)));
        invoice.markAsIssued("ext-1");
        assertThrows(IllegalStateException.class, () -> invoice.markAsIssued("ext-2"));
    }

    @Test
    void validate_shouldThrowIfNoItems() {
        assertThrows(IllegalStateException.class, () ->
                new Invoice(UUID.randomUUID(), UUID.randomUUID(), "1234567890", "Buyer", List.of()));
    }

    @Test
    void validate_shouldThrowIfTaxIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Invoice(UUID.randomUUID(), UUID.randomUUID(), "", "Buyer", List.of(new InvoiceItem("item", 2, BigDecimal.TEN))));
    }

    @Test
    void validate_shouldThrowIfBuyerNameBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Invoice(UUID.randomUUID(), UUID.randomUUID(), "1234567890", "", List.of(new InvoiceItem("item", 2, BigDecimal.TEN))));
    }
}

