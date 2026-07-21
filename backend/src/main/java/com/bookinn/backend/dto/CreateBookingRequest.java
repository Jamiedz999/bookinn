package com.bookinn.backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Payload to create a booking. The half-open date invariant ({@code checkIn < checkOut}) and the
 * {@code guestCount <= maxGuests} rule are enforced in the service, where the listing is available;
 * bean validation here only guards obviously bad input.
 *
 * @param listingId listing to book
 * @param checkIn inclusive check-in; must not be in the past
 * @param checkOut exclusive check-out; must not be in the past
 * @param guestCount number of guests; at least 1
 */
public record CreateBookingRequest(
    @NotNull Long listingId,
    @NotNull @FutureOrPresent LocalDate checkIn,
    @NotNull @FutureOrPresent LocalDate checkOut,
    @Min(1) int guestCount) {}
