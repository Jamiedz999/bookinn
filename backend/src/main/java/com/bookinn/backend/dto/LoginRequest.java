package com.bookinn.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login payload.
 *
 * @param email login email
 * @param password plaintext password
 */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
