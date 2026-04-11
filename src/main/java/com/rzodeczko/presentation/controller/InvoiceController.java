package com.rzodeczko.presentation.controller;


import com.rzodeczko.application.port.input.GenerateInvoiceCommand;
import com.rzodeczko.application.port.input.GenerateInvoiceUseCase;
import com.rzodeczko.application.port.input.GetInvoicePdfUseCase;
import com.rzodeczko.application.port.input.ItemCommand;
import com.rzodeczko.presentation.dto.CreateInvoiceRequestDto;
import com.rzodeczko.presentation.dto.CreateInvoiceResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing invoices.
 * <p>
 * Provides endpoints to create invoices and download invoice PDFs.
 * <ul>
 *   <li><b>POST /invoices</b> - Create a new invoice</li>
 *   <li><b>GET /invoices/{id}/pdf</b> - Download invoice PDF by UUID</li>
 * </ul>
 * <p>
 * Uses UUIDs for security and API stability reasons.
 */
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final GenerateInvoiceUseCase generateInvoiceUseCase;
    private final GetInvoicePdfUseCase getInvoicePdfUseCase;

    @PostMapping
    public ResponseEntity<CreateInvoiceResponseDto> createInvoice(
            @RequestBody @Valid CreateInvoiceRequestDto request) {
        System.out.println("Received create invoice request: " + request);
        var items = request
                .items()
                .stream()
                .map(i -> new ItemCommand(i.name(), i.quantity(), i.price()))
                .toList();

        var command = new GenerateInvoiceCommand(
                request.orderId(),
                request.taxId(),
                request.buyerName(),
                items
        );

        UUID invoiceId = generateInvoiceUseCase.generate(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateInvoiceResponseDto(invoiceId));
    }

    /**
     * Retrieves the PDF file for the invoice with the specified UUID.
     * <p>
     * <b>Security rationale:</b> The system uses UUIDs from the database instead of sequential external IDs (e.g., from Fakturownia) to prevent enumeration attacks. Sequential IDs can be easily guessed, potentially exposing sensitive invoice data of other clients. UUIDs are cryptographically secure and not guessable.
     * <p>
     * <b>API stability:</b> Using UUIDs decouples the API from the external provider's identifier format. This ensures that changing the invoice provider does not require changes to the API or frontend, maintaining backward compatibility and stability.
     *
     * @param id the UUID of the invoice
     * @return the PDF file as a byte array in the response body, with appropriate headers to prevent caching and ensure secure download
     */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable UUID id) {
        byte[] pdfContent = getInvoicePdfUseCase.getPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice_" + id + ".pdf");

        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }
}
