package com.bookinn.backend.dto;

/**
 * Login/refresh response body. The refresh token is not included here; it travels in an httpOnly
 * cookie so JavaScript cannot read it.
 *
 * @param accessToken short-lived JWT access token
 * @param user the signed-in user
 */
public record AuthResponse(String accessToken, UserResponse user) {}
