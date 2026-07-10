package com.bookinn.backend.dto;

import com.bookinn.backend.domain.Role;
import jakarta.validation.constraints.NotNull;

/**
 * One-click demo login payload.
 *
 * @param role which demo persona to sign in as
 */
public record DemoLoginRequest(@NotNull Role role) {}
