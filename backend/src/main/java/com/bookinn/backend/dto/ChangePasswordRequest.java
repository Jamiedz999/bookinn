package com.bookinn.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to change the current user's password.
 *
 * @param currentPassword the user's existing password, verified before the change
 * @param newPassword the replacement password
 */
public record ChangePasswordRequest(
    @NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 100) String newPassword) {}
