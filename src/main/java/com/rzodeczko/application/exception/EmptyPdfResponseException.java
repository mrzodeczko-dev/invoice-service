package com.rzodeczko.application.exception;

public class EmptyPdfResponseException extends RuntimeException {
    public EmptyPdfResponseException(String externalId) {
        super("External system returned empty PDF for externalId=" + externalId);
    }
}
