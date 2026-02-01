package com.atypon.web;

import com.atypon.config.InboundRateLimitProperties;
import com.atypon.exception.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class InboundRateLimitFilter extends OncePerRequestFilter {

    private final InboundRateLimitProperties props;
    private final MeterRegistry meterRegistry;

    private final Map<String, RateLimiter> perClient = new LinkedHashMap<>(256, 0.75f, true);

    public InboundRateLimitFilter(InboundRateLimitProperties props, MeterRegistry meterRegistry) {
        this.props = props;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null
                || path.equals("/")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        RateLimiter limiter = getLimiter(clientKey(request));
        boolean allowed = limiter.acquirePermission();
        if (!allowed) {
            meterRegistry.counter("rate_limit.inbound.denied").increment();
            throw new RateLimitExceededException("Inbound rate limit exceeded", props.getRefreshPeriod());
        }
        meterRegistry.counter("rate_limit.inbound.allowed").increment();
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            String first = fwd.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }

    private RateLimiterConfig config() {
        Duration refresh = props.getRefreshPeriod() == null ? Duration.ofSeconds(60) : props.getRefreshPeriod();
        Duration timeout = props.getTimeout() == null ? Duration.ZERO : props.getTimeout();
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(refresh)
                .limitForPeriod(props.getLimitForPeriod())
                .timeoutDuration(timeout)
                .build();
    }

    private RateLimiter getLimiter(String key) {
        synchronized (perClient) {
            RateLimiter limiter = perClient.get(key);
            if (limiter != null) {
                return limiter;
            }
            limiter = RateLimiter.of("inbound-" + key, config());
            perClient.put(key, limiter);
            evictOldestUntil(props.getMaxClients());
            return limiter;
        }
    }

    private void evictOldestUntil(int maxClients) {
        if (maxClients <= 0) {
            return;
        }
        while (perClient.size() > maxClients) {
            String oldestKey = perClient.keySet().iterator().next();
            perClient.remove(oldestKey);
        }
    }
}
