package com.rzodeczko.presentation.exception;

import com.rzodeczko.application.exception.EmptyPdfResponseException;
import com.rzodeczko.application.exception.ExternalTaxSystemException;
import com.rzodeczko.application.exception.InvoiceConcurrentModificationException;
import com.rzodeczko.domain.exception.InvoiceAlreadyExistsException;
import com.rzodeczko.domain.exception.InvoiceNotIssuedException;
import com.rzodeczko.domain.exception.ResourceNotFoundException;
import com.rzodeczko.presentation.dto.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResourceNotFoundException_shouldReturnNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void handleInvoiceAlreadyExistsException_shouldReturnConflict() {
        InvoiceAlreadyExistsException ex = new InvoiceAlreadyExistsException(java.util.UUID.randomUUID());
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void handleInvoiceNotIssuedException_shouldReturnConflict() {
        InvoiceNotIssuedException ex = new InvoiceNotIssuedException(java.util.UUID.randomUUID());
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void handleInvoiceConcurrentModificationException_shouldReturnConflict() {
        InvoiceConcurrentModificationException ex = new InvoiceConcurrentModificationException(java.util.UUID.randomUUID());
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturnConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("violation");
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void handleExternalTaxSystemException_shouldReturnBadGateway() {
        ExternalTaxSystemException ex = new ExternalTaxSystemException("error");
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(502, response.getStatusCode().value());
    }

    @Test
    void handleEmptyPdfResponseException_shouldReturnBadGateway() {
        EmptyPdfResponseException ex = new EmptyPdfResponseException("id");
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(502, response.getStatusCode().value());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new Exception("error");
        ResponseEntity<ErrorResponseDto> response = handler.handle(ex);
        assertEquals(500, response.getStatusCode().value());
    }
}
