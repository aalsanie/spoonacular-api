package com.atypon.idempotency;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class InMemoryIdempotencyStore implements IdempotencyStore {

    private static final class EntryData {
        final String fingerprint;
        final long expiresAtMillis;
        final CompletableFuture<StoredResponse> future;

        EntryData(String fingerprint, long expiresAtMillis, CompletableFuture<StoredResponse> future) {
            this.fingerprint = fingerprint;
            this.expiresAtMillis = expiresAtMillis;
            this.future = future;
        }
    }

    private final Map<String, EntryData> map = new LinkedHashMap<>(256, 0.75f, true);

    /**
     * Size bound is enforced by the filter which knows configured max entries.
     * This store stays generic and deterministic.
     */

    @Override
    public synchronized IdempotencyEntry getOrCreate(String key, String fingerprint, Duration ttl) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);
        EntryData existing = map.get(key);
        if (existing != null) {
            return new IdempotencyEntry(key, existing.fingerprint, existing.future, false);
        }
        long expiresAt = now + (ttl == null ? Duration.ofMinutes(30).toMillis() : ttl.toMillis());
        CompletableFuture<StoredResponse> future = new CompletableFuture<>();
        map.put(key, new EntryData(fingerprint, expiresAt, future));
        return new IdempotencyEntry(key, fingerprint, future, true);
    }

    @Override
    public synchronized Optional<StoredResponse> getCompleted(String key) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);
        EntryData data = map.get(key);
        if (data == null || !data.future.isDone() || data.future.isCompletedExceptionally()) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.future.getNow(null));
    }

    public synchronized void evictOldestUntil(int maxEntries) {
        if (maxEntries <= 0) {
            return;
        }
        while (map.size() > maxEntries) {
            String oldestKey = map.keySet().iterator().next();
            map.remove(oldestKey);
        }
    }

    private void cleanupExpired(long nowMillis) {
        map.entrySet().removeIf(e -> e.getValue().expiresAtMillis <= nowMillis);
    }
}
