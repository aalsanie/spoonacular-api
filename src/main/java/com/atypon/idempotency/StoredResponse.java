package com.atypon.idempotency;

import org.springframework.http.HttpHeaders;

public record StoredResponse(int status, HttpHeaders headers, byte[] body) {
}
