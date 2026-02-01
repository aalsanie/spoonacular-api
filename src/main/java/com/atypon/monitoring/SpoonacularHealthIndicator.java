package com.atypon.monitoring;

import com.atypon.config.SpoonacularConfig;
import com.atypon.config.SpoonacularHealthProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Component
public class SpoonacularHealthIndicator implements HealthIndicator {

    private final SpoonacularConfig config;
    private final SpoonacularHealthProperties props;
    private final RestTemplate restTemplate;

    private volatile long lastCheckAtMillis = 0;
    private volatile boolean lastLiveOk = true;
    private volatile String lastLiveError = null;

    public SpoonacularHealthIndicator(SpoonacularConfig config, SpoonacularHealthProperties props, RestTemplate restTemplate) {
        this.config = config;
        this.props = props;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        boolean hasBaseUrl = config.getBaseUrl() != null && !config.getBaseUrl().isBlank();
        boolean hasApiKey = config.getApiKey() != null && !config.getApiKey().isBlank();

        Health.Builder builder = (hasBaseUrl && hasApiKey) ? Health.up() : Health.down();
        builder.withDetail("baseUrlConfigured", hasBaseUrl);
        builder.withDetail("apiKeyConfigured", hasApiKey);

        if (props.isLiveCheckEnabled() && hasBaseUrl && hasApiKey) {
            maybeRefreshLiveCheck();
            builder.withDetail("liveCheckEnabled", true);
            builder.withDetail("liveCheckOk", lastLiveOk);
            if (!lastLiveOk && lastLiveError != null) {
                builder.withDetail("liveCheckError", lastLiveError);
            }
            if (!lastLiveOk) {
                // Force overall status to DOWN while preserving details.
                return Health.down().withDetails(builder.build().getDetails()).build();
            }
        } else {
            builder.withDetail("liveCheckEnabled", false);
        }

        return builder.build();
    }

    private void maybeRefreshLiveCheck() {
        long now = System.currentTimeMillis();
        Duration ttl = props.getCacheTtl() == null ? Duration.ofMinutes(1) : props.getCacheTtl();
        if (now - lastCheckAtMillis < ttl.toMillis()) {
            return;
        }

        // Lightweight endpoint (still counts against quota). Cache and keep it opt-in.
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(config.getBaseUrl())
                    .path("/recipes/complexSearch")
                    .queryParam("query", "apple")
                    .queryParam("number", 1)
                    .queryParam("apiKey", config.getApiKey())
                    .build(true)
                    .toUri();

            restTemplate.getForEntity(uri, String.class);
            lastLiveOk = true;
            lastLiveError = null;
        } catch (Exception e) {
            lastLiveOk = false;
            lastLiveError = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            lastCheckAtMillis = now;
        }
    }
}
