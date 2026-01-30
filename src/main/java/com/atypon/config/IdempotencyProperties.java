package com.atypon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    /** Enable idempotency support for non-GET requests when the idempotency header is present. */
    private boolean enabled = true;

    /** Header name used for idempotency keys. */
    private String header = "Idempotency-Key";

    /** How long to keep completed idempotency keys. */
    private Duration ttl = Duration.ofMinutes(30);

    /** Upper bound for in-memory store size. Oldest entries are evicted. */
    private int maxEntries = 5000;

    /**
     * How long to wait for an in-flight duplicate request with the same key.
     * 0 = return immediately (409).
     */
    private Duration inFlightWait = Duration.ZERO;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public Duration getInFlightWait() {
        return inFlightWait;
    }

    public void setInFlightWait(Duration inFlightWait) {
        this.inFlightWait = inFlightWait;
    }
}
