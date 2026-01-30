package com.atypon.web;

import com.atypon.config.IdempotencyProperties;
import com.atypon.exception.IdempotencyConflictException;
import com.atypon.idempotency.IdempotencyEntry;
import com.atypon.idempotency.IdempotencyStore;
import com.atypon.idempotency.InMemoryIdempotencyStore;
import com.atypon.idempotency.StoredResponse;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER_STATUS = "Idempotency-Status";
    public static final String STATUS_HIT = "HIT";
    public static final String STATUS_MISS = "MISS";

    private final IdempotencyProperties props;
    private final IdempotencyStore store;
    private final MeterRegistry meterRegistry;

    public IdempotencyFilter(IdempotencyProperties props, IdempotencyStore store, MeterRegistry meterRegistry) {
        this.props = props;
        this.store = store;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) {
            return true;
        }
        String method = request.getMethod();
        return HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String keyHeader = props.getHeader() == null ? "Idempotency-Key" : props.getHeader();
        String key = request.getHeader(keyHeader);
        if (key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, bodyBytes);

        String fingerprint = fingerprint(cachedRequest, bodyBytes);

        IdempotencyEntry entry = store.getOrCreate(key, fingerprint, props.getTtl());
        if (!fingerprint.equals(entry.fingerprint())) {
            throw new IdempotencyConflictException("Idempotency key reuse with a different request payload");
        }

        // If already completed, short-circuit.
        if (entry.future().isDone() && !entry.future().isCompletedExceptionally()) {
            StoredResponse stored = entry.future().getNow(null);
            if (stored != null) {
                meterRegistry.counter("idempotency.hit").increment();
                writeStored(response, keyHeader, key, stored, STATUS_HIT);
                return;
            }
        }

        // If duplicate in-flight and not the creator, optionally wait.
        if (!entry.created() && !entry.future().isDone()) {
            Duration wait = props.getInFlightWait();
            if (wait != null && !wait.isZero() && !wait.isNegative()) {
                try {
                    StoredResponse stored = entry.future().get(wait.toMillis(), TimeUnit.MILLISECONDS);
                    meterRegistry.counter("idempotency.hit").increment();
                    writeStored(response, keyHeader, key, stored, STATUS_HIT);
                    return;
                } catch (Exception ignored) {
                    // fall through to conflict response
                }
            }
            meterRegistry.counter("idempotency.in_flight_conflict").increment();
            response.setStatus(409);
            response.setHeader(keyHeader, key);
            response.setHeader(HEADER_STATUS, "IN_FLIGHT");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Idempotency conflict\",\"status\":409,\"detail\":\"Request with the same idempotency key is still in-flight\"}");
            return;
        }

        // Process as the key owner.
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, cachingResponse);

            byte[] respBody = cachingResponse.getContentAsByteArray();
            HttpHeaders headers = new HttpHeaders();
            cachingResponse.getHeaderNames().forEach(name -> headers.put(name, new java.util.ArrayList<>(cachingResponse.getHeaders(name))));
            StoredResponse stored = new StoredResponse(cachingResponse.getStatus(), headers, respBody);

            // ensure bounded memory for in-memory default
            if (store instanceof InMemoryIdempotencyStore mem) {
                mem.evictOldestUntil(props.getMaxEntries());
            }

            if (!entry.future().isDone()) {
                entry.future().complete(stored);
            }

            response.setHeader(keyHeader, key);
            response.setHeader(HEADER_STATUS, STATUS_MISS);
            meterRegistry.counter("idempotency.miss").increment();
        } catch (Exception e) {
            if (!entry.future().isDone()) {
                entry.future().completeExceptionally(e);
            }
            throw e;
        } finally {
            cachingResponse.copyBodyToResponse();
        }
    }

    private void writeStored(HttpServletResponse response, String keyHeader, String key, StoredResponse stored, String status)
            throws IOException {

        response.setStatus(stored.status());
        stored.headers().forEach((name, values) -> {
            if (name == null) {
                return;
            }
            // Do not re-write content-length; container will handle.
            if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                return;
            }
            for (String v : values) {
                response.addHeader(name, v);
            }
        });
        response.setHeader(keyHeader, key);
        response.setHeader(HEADER_STATUS, status);
        if (stored.body() != null && stored.body().length > 0) {
            response.getOutputStream().write(stored.body());
        }
    }

    private String fingerprint(HttpServletRequest request, byte[] body) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString() == null ? "" : request.getQueryString();
        String contentType = request.getContentType() == null ? "" : request.getContentType();

        String bodyHash = sha256Base64(body == null ? new byte[0] : body);
        return method + "|" + uri + "?" + query + "|" + contentType + "|" + bodyHash;
    }

    private String sha256Base64(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            // should never happen
            return Base64.getUrlEncoder().withoutPadding().encodeToString(("fallback:" + new String(bytes, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
        }
    }
}
