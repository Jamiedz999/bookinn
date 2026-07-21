package com.bookinn.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Price preview for a prospective booking. Returned before the guest commits, so the detail page
 * can show the breakdown; it also confirms the dates are currently available (an unavailable range
 * is a 409, not a quote).
 *
 * @param listingId the quoted listing
 * @param checkIn inclusive check-in
 * @param checkOut exclusive check-out
 * @param nights number of booked nights
 * @param pricePerNight nightly price
 * @param totalPrice nights × nightly price
 */
public record QuoteResponse(
    Long listingId,
    LocalDate checkIn,
    LocalDate checkOut,
    long nights,
    BigDecimal pricePerNight,
    BigDecimal totalPrice) {}
