package com.bookinn.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code bookinn.auth} configuration.
 *
 * @param refreshCookieName name of the httpOnly cookie that carries the refresh token
 * @param cookieSecure whether the refresh cookie is marked Secure (true behind HTTPS)
 */
@ConfigurationProperties(prefix = "bookinn.auth")
public record AuthProperties(String refreshCookieName, boolean cookieSecure) {}
