package com.rzodeczko.application.service;

import com.rzodeczko.application.port.input.GenerateInvoiceCommand;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.model.InvoiceItem;
import com.rzodeczko.domain.repository.InvoiceRepository;

import java.util.List;
import java.util.UUID;

public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public boolean existsByOrderId(UUID orderId) {
        return invoiceRepository.existsByOrderId(orderId);
    }

    public Invoice buildInvoice(GenerateInvoiceCommand command) {
        List<InvoiceItem> items = command
                .items()
                .stream()
                .map(i -> new InvoiceItem(i.name(), i.quantity(), i.price()))
                .toList();
        return new Invoice(
                UUID.randomUUID(),
                command.orderId(),
                command.taxId(),
                command.buyerName(),
                items
        );
    }

    // Zapisuje nowa fakture. Wywolywana wewnatrz transakcji.
    public void saveNewInvoice(Invoice invoice) {
        invoiceRepository.save(invoice);
    }

    public void markInvoiceAsIssued(Invoice invoice, String externalId) {
        invoice.markAsIssued(externalId);
        invoiceRepository.save(invoice);
    }
}
