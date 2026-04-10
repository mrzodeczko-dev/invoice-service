package com.rzodeczko.application.exception;

public class ExternalTaxSystemException extends RuntimeException {
    public ExternalTaxSystemException(String message) {
        super(message);
    }

    public ExternalTaxSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
