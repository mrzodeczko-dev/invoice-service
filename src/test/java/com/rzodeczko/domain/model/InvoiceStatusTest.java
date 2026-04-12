package com.rzodeczko.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvoiceStatus enum.
 */
class InvoiceStatusTest {
    @Test
    void values_shouldContainDraftAndIssued() {
        assertTrue(java.util.Arrays.asList(InvoiceStatus.values()).contains(InvoiceStatus.DRAFT));
        assertTrue(java.util.Arrays.asList(InvoiceStatus.values()).contains(InvoiceStatus.ISSUED));
    }
}

