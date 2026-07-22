package com.bookinn.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to change the current user's login email.
 *
 * @param newEmail the replacement email; must be unique
 */
public record ChangeEmailRequest(@NotBlank @Email String newEmail) {}
