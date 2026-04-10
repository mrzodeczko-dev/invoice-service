package com.rzodeczko.application.port.input;
import java.util.List;
import java.util.UUID;

public record GenerateInvoiceCommand(
        UUID orderId,
        String taxId,
        String buyerName,
        List<ItemCommand> items
) {
    public GenerateInvoiceCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("OrderId cannot be null");
        }

        if (taxId == null || taxId.isBlank()) {
            throw new IllegalArgumentException("TaxId cannot be blank");
        }

        if (buyerName == null || buyerName.isBlank()) {
            throw new IllegalArgumentException("BuyerName cannot be blank");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty");
        }
    }
}
