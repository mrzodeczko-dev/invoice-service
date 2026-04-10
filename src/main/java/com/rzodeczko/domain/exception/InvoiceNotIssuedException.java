package com.rzodeczko.domain.exception;

import java.util.UUID;

/**
 * Rzucany, gdy proba pobrania PDF faktury, ktora jeszcze nie zostala wyslana do zewnetrznego
 * systemu podatkowego.
 */
public class InvoiceNotIssuedException extends RuntimeException {
    public InvoiceNotIssuedException(UUID invoiceId) {
        super("Invoice has not been issued to external system yet. invoiceId=" + invoiceId);
    }
}
