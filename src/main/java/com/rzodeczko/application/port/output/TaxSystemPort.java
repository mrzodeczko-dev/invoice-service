package com.rzodeczko.application.port.output;


import com.rzodeczko.domain.model.Invoice;

public interface TaxSystemPort {
    String issueInvoice(Invoice invoice);

    byte[] getPdf(String externalId);
}
