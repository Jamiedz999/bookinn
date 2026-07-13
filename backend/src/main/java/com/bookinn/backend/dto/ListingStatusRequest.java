package com.bookinn.backend.dto;

import com.bookinn.backend.domain.ListingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload to activate or deactivate a listing.
 *
 * @param status the target status
 */
public record ListingStatusRequest(@NotNull ListingStatus status) {}
