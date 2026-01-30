package com.atypon.monitoring;

import com.atypon.config.AlertingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AlertingConfiguration {

    @Bean
    @ConditionalOnMissingBean(AlertService.class)
    AlertService alertService(AlertingProperties properties, MeterRegistry meterRegistry) {
        return new LoggingAlertService(properties, meterRegistry);
    }

}
