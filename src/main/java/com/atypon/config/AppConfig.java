package com.atypon.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            HttpClientProperties props,
            List<ClientHttpRequestInterceptor> interceptors
    ) {
        Duration connectTimeout = props.getConnectTimeout();
        Duration readTimeout = props.getReadTimeout();

        return builder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .additionalInterceptors(interceptors)
                .build();
    }
}

