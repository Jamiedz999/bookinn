package com.bookinn.backend.dto;

/**
 * Current-month occupancy for one of the host's listings. {@code bookedNights} is the sum, across
 * the listing's CONFIRMED/COMPLETED bookings, of the nights that fall inside the current month —
 * bookings spanning a month boundary are clipped to the part inside the month. {@code rate} is
 * {@code bookedNights / daysInMonth}, so it is always between 0 and 1 (bookings on one listing
 * cannot overlap).
 *
 * @param listingId the listing
 * @param listingTitle the listing's headline, shown as the bar label
 * @param bookedNights booked nights inside the current month
 * @param daysInMonth number of days in the current month (the denominator / "available nights")
 * @param rate {@code bookedNights / daysInMonth}, in {@code [0, 1]}
 */
public record ListingOccupancyResponse(
    Long listingId, String listingTitle, long bookedNights, int daysInMonth, double rate) {}
