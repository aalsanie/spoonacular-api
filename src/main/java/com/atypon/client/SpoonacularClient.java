package com.atypon.client;

import com.atypon.config.SpoonacularConfig;
import com.atypon.exception.ExternalServiceException;
import com.atypon.exception.RateLimitExceededException;
import com.atypon.monitoring.AlertService;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Component
public class SpoonacularClient {

    private final RestTemplate restTemplate;
    private final SpoonacularConfig config;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final AlertService alertService;

    public SpoonacularClient(
            RestTemplate restTemplate,
            SpoonacularConfig config,
            RetryRegistry retryRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            MeterRegistry meterRegistry,
            AlertService alertService
    ) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.retry = retryRegistry.retry("spoonacular");
        this.rateLimiter = rateLimiterRegistry.rateLimiter("spoonacular");
        this.meterRegistry = meterRegistry;
        this.alertService = alertService;
    }

    public ResponseEntity<JsonNode> search(String query, String cuisine) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(config.getBaseUrl())
                .path("/recipes/complexSearch")
                .queryParam("query", query)
                .queryParamIfPresent("cuisine", cuisine == null || cuisine.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(cuisine))
                .queryParam("apiKey", config.getApiKey())
                .build(true)
                .toUri();
        return execute("search", () -> restTemplate.getForEntity(uri, JsonNode.class));
    }

    public ResponseEntity<com.atypon.model.Recipe> recipeInfo(int recipeId) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(config.getBaseUrl())
                .path("/recipes/{id}/information")
                .queryParam("apiKey", config.getApiKey())
                .queryParam("includeNutrition", false)
                .buildAndExpand(recipeId)
                .encode()
                .toUri();
        return execute("info", () -> restTemplate.getForEntity(uri, com.atypon.model.Recipe.class));
    }

    private <T> ResponseEntity<T> execute(String operation, java.util.function.Supplier<ResponseEntity<T>> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Retry wraps the supplier, so each attempt still passes through the rate limiter.
            java.util.function.Supplier<ResponseEntity<T>> decorated = Retry.decorateSupplier(
                    retry,
                    RateLimiter.decorateSupplier(rateLimiter, supplier)
            );
            return decorated.get();
        } catch (RequestNotPermitted e) {
            meterRegistry.counter("spoonacular.client.rate_limited", "op", operation).increment();
            alertService.alert(
                    "spoonacular." + operation + ".client_rate_limited",
                    "Spoonacular client-side rate limiter denied request (" + operation + ")",
                    e
            );
            throw new RateLimitExceededException("Upstream rate limiter denied request", Duration.ofSeconds(1));
        } catch (RestClientResponseException e) {
            HttpStatusCode status = HttpStatusCode.valueOf(e.getRawStatusCode());
            meterRegistry.counter("spoonacular.client.failures", "op", operation, "status", String.valueOf(status.value())).increment();
            alertService.alert(
                    "spoonacular." + operation + ".http_" + status.value(),
                    "Spoonacular call failed after retries (" + operation + ") status=" + status.value(),
                    e
            );
            throw new ExternalServiceException("spoonacular", "Upstream failure (" + operation + ")", status, e);
        } catch (RuntimeException e) {
            meterRegistry.counter("spoonacular.client.failures", "op", operation, "status", "exception").increment();
            alertService.alert(
                    "spoonacular." + operation + ".exception",
                    "Spoonacular call failed after retries (" + operation + ")",
                    e
            );
            throw e;
        } finally {
            sample.stop(Timer.builder("spoonacular.client.latency")
                    .tag("op", operation)
                    .register(meterRegistry));
        }
    }
}
