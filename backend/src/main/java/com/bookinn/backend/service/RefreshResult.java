package com.bookinn.backend.service;

import com.bookinn.backend.domain.User;

/**
 * Outcome of rotating a refresh token: the owning user and the freshly issued raw token that
 * replaces the consumed one.
 *
 * @param user owner of the token
 * @param rawToken the new raw refresh token to hand back to the client
 */
public record RefreshResult(User user, String rawToken) {}
