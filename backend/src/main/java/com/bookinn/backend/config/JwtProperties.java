package com.bookinn.backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code bookinn.jwt} configuration.
 *
 * @param secret HMAC signing secret (at least 32 bytes)
 * @param accessTtl access-token lifetime
 * @param refreshTtl refresh-token lifetime
 */
@ConfigurationProperties(prefix = "bookinn.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {}
