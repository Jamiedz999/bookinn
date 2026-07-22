package com.bookinn.backend.dto;

import java.math.BigDecimal;

/**
 * The three headline numbers on the host dashboard, computed live for the authenticated host. All
 * three ignore CANCELLED bookings.
 *
 * @param currentMonthRevenue sum of {@code total_price} for the host's CONFIRMED/COMPLETED bookings
 *     whose check-in falls in the current calendar month
 * @param totalBookings running count of the host's non-cancelled bookings (all time)
 * @param upcomingCheckIns number of CONFIRMED bookings checking in within the next 7 days
 */
public record DashboardSummaryResponse(
    BigDecimal currentMonthRevenue, long totalBookings, long upcomingCheckIns) {}
