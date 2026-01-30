package com.atypon.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<Map<String, Object>> badRequest(Exception ex, HttpServletRequest req) {
        // Keep the response shape stable for tests.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody("Invalid request format", req));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> rateLimited(RateLimitExceededException ex, HttpServletRequest req) {
        HttpHeaders headers = new HttpHeaders();
        Duration retryAfter = ex.getRetryAfter();
        if (retryAfter != null) {
            headers.add("Retry-After", String.valueOf(Math.max(1, retryAfter.toSeconds())));
        }

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(errorBody(ex.getMessage() != null ? ex.getMessage() : "Too Many Requests", req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex, HttpServletRequest req) {
        // Don’t leak internals to clients. Keep a server-side log.
        log.error("Unhandled exception for {} {}", req.getMethod(), req.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Internal Server Error", req));
    }

    private static Map<String, Object> errorBody(String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("path", req.getRequestURI());
        return body;
    }
}
