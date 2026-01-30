package com.atypon.config;

import com.atypon.idempotency.IdempotencyStore;
import com.atypon.idempotency.InMemoryIdempotencyStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a default {@link IdempotencyStore} implementation.
 *
 * <p>Production note: teams can override this by defining their own {@code IdempotencyStore}
 * bean (e.g., Redis-backed) without touching the filter.</p>
 */
@Configuration
public class IdempotencyStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    public IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }
}
