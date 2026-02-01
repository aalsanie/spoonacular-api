package com.atypon.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {

    IdempotencyEntry getOrCreate(String key, String fingerprint, Duration ttl);

    Optional<StoredResponse> getCompleted(String key);
}
