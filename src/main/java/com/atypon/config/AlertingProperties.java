package com.atypon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "alerting")
public class AlertingProperties {

    /** Enable alert publishing (default: logs + metrics). */
    private boolean enabled = true;

    /** Minimum time between repeating the same alert key. */
    private Duration throttle = Duration.ofSeconds(60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getThrottle() {
        return throttle;
    }

    public void setThrottle(Duration throttle) {
        this.throttle = throttle;
    }
}
