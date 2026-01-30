package com.atypon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spoonacular API configuration.
 *
 * <p>Plain Java (no Lombok) to avoid annotation-processor/JDK issues.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "spoonacular")
public class SpoonacularConfig {

    private String baseUrl;
    private String apiKey;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}