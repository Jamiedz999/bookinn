package com.bookinn.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. New accounts are created with the GUEST role.
 *
 * @param email unique login email
 * @param password plaintext password, hashed with BCrypt before storage
 * @param name display name
 */
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 100) String name) {}
