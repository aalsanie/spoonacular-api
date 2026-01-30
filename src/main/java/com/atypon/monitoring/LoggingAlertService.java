package com.atypon.monitoring;

import com.atypon.config.AlertingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default alerting: logs throttled alerts and emits a counter.
 */
public class LoggingAlertService implements AlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAlertService.class);

    private final AlertingProperties props;
    private final MeterRegistry meterRegistry;
    private final Map<String, Long> lastEmittedAtMillis = new ConcurrentHashMap<>();

    public LoggingAlertService(AlertingProperties props, MeterRegistry meterRegistry) {
        this.props = props;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void alert(String key, String message, Throwable error) {
        if (!props.isEnabled()) {
            return;
        }

        Duration throttle = props.getThrottle() == null ? Duration.ofSeconds(60) : props.getThrottle();
        long now = System.currentTimeMillis();
        long last = lastEmittedAtMillis.getOrDefault(key, 0L);

        if (now - last < throttle.toMillis()) {
            return;
        }
        lastEmittedAtMillis.put(key, now);

        meterRegistry.counter("alerts", "key", key).increment();
        if (error == null) {
            LOGGER.error("ALERT [{}] {}", key, message);
        } else {
            LOGGER.error("ALERT [{}] {}", key, message, error);
        }
    }
}
