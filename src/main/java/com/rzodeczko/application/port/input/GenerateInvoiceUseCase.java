package com.rzodeczko.application.port.input;
import java.util.UUID;


public interface GenerateInvoiceUseCase {
    UUID generate(GenerateInvoiceCommand command);
}
