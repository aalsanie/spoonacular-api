package com.atypon.idempotency;

import java.util.concurrent.CompletableFuture;

public final class IdempotencyEntry {

    private final String key;
    private final String fingerprint;
    private final CompletableFuture<StoredResponse> future;
    private final boolean created;

    public IdempotencyEntry(String key, String fingerprint, CompletableFuture<StoredResponse> future, boolean created) {
        this.key = key;
        this.fingerprint = fingerprint;
        this.future = future;
        this.created = created;
    }

    public String key() {
        return key;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public CompletableFuture<StoredResponse> future() {
        return future;
    }

    public boolean created() {
        return created;
    }
}
