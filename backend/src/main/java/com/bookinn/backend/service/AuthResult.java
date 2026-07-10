package com.bookinn.backend.service;

import com.bookinn.backend.domain.User;

/**
 * Result of a successful authentication. The controller turns {@code accessToken} + {@code user}
 * into the response body and {@code rawRefreshToken} into an httpOnly cookie.
 *
 * @param accessToken signed JWT access token
 * @param rawRefreshToken raw refresh token for the cookie
 * @param user the authenticated user
 */
public record AuthResult(String accessToken, String rawRefreshToken, User user) {}
