package com.atypon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "spoonacular.health")
public class SpoonacularHealthProperties {

    /** If true, perform a lightweight live check (can consume upstream quota). */
    private boolean liveCheckEnabled = false;

    /** Cache TTL for live check result to avoid hammering upstream. */
    private Duration cacheTtl = Duration.ofMinutes(1);

    public boolean isLiveCheckEnabled() {
        return liveCheckEnabled;
    }

    public void setLiveCheckEnabled(boolean liveCheckEnabled) {
        this.liveCheckEnabled = liveCheckEnabled;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
}
