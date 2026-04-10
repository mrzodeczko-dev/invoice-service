package com.rzodeczko.domain.repository;


import com.rzodeczko.domain.model.Invoice;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    void save(Invoice invoice);

    boolean existsByOrderId(UUID orderId);

    Optional<Invoice> findById(UUID id);

    Optional<byte[]> findPdfContent(UUID invoiceId);

    void savePdfContent(UUID invoiceId, byte[] content);
}
