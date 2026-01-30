package com.atypon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rate-limits.inbound")
public class InboundRateLimitProperties {

    /** Protect this API from being hammered. */
    private boolean enabled = true;

    /** Tokens added each refresh period per client. */
    private int limitForPeriod = 120;

    /** Token bucket refresh period. */
    private Duration refreshPeriod = Duration.ofSeconds(60);

    /** How long to wait for a token. 0 = fail fast with 429. */
    private Duration timeout = Duration.ZERO;

    /** Max number of distinct clients to track in memory before evicting oldest. */
    private int maxClients = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public void setLimitForPeriod(int limitForPeriod) {
        this.limitForPeriod = limitForPeriod;
    }

    public Duration getRefreshPeriod() {
        return refreshPeriod;
    }

    public void setRefreshPeriod(Duration refreshPeriod) {
        this.refreshPeriod = refreshPeriod;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }
}
