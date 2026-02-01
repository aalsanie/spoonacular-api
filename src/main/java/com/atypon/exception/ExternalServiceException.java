package com.atypon.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Raised when an outbound dependency (e.g., Spoonacular) fails after retries/backoff.
 */
public class ExternalServiceException extends RuntimeException {

    private final String service;
    private final HttpStatusCode statusCode;

    public ExternalServiceException(String service, String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.service = service;
        this.statusCode = statusCode;
    }

    public String getService() {
        return service;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
