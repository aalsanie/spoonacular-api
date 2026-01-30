package com.atypon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Outbound HTTP client knobs (RestTemplate).
 */
@ConfigurationProperties(prefix = "http.client")
public class HttpClientProperties {

    /** Connection timeout for outbound HTTP calls. */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /** Read timeout for outbound HTTP calls. */
    private Duration readTimeout = Duration.ofSeconds(4);

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
