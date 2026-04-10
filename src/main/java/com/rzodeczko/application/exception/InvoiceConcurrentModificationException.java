package com.rzodeczko.application.exception;

import java.util.UUID;

public class InvoiceConcurrentModificationException extends RuntimeException {
    public InvoiceConcurrentModificationException(UUID invoiceId) {
        super("Invoice was modified concurrently. invoiceId=" + invoiceId);
    }
}
