package com.atypon.client;

import com.atypon.web.RequestIdFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
public class OutboundRequestInterceptor implements ClientHttpRequestInterceptor {

    private final MeterRegistry meterRegistry;

    public OutboundRequestInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null && !requestId.isBlank()) {
            request.getHeaders().set(RequestIdFilter.HEADER, requestId);
        }

        URI uri = request.getURI();
        String host = uri.getHost() == null ? "unknown" : uri.getHost();
        String path = uri.getPath() == null ? "/" : uri.getPath();
        String method = request.getMethod() == null ? "UNKNOWN" : request.getMethod().name();

        long start = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            record(host, path, method, String.valueOf(response.getStatusCode().value()), start);
            return response;
        } catch (IOException e) {
            record(host, path, method, "IO_ERROR", start);
            throw e;
        } catch (RuntimeException e) {
            record(host, path, method, "RUNTIME_ERROR", start);
            throw e;
        }
    }

    private void record(String host, String path, String method, String status, long startNanos) {
        long duration = System.nanoTime() - startNanos;
        Timer.builder("http.client.requests")
                .tag("host", host)
                .tag("path", path)
                .tag("method", method)
                .tag("status", status)
                .register(meterRegistry)
                .record(duration, TimeUnit.NANOSECONDS);
    }
}
