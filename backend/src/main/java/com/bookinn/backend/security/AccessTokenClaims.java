package com.bookinn.backend.security;

import com.bookinn.backend.domain.Role;
import java.util.Set;

/**
 * The verified contents of an access token.
 *
 * @param userId id of the authenticated user
 * @param email login email of the user
 * @param roles roles granted to the user
 */
public record AccessTokenClaims(Long userId, String email, Set<Role> roles) {}
