package com.bookinn.backend.dto;

import java.math.BigDecimal;

/**
 * One point on the 12-month revenue-trend line. Months in which the host had no revenue are still
 * present with a zero amount — the "zero-fill" is done in the service after the SQL, which only
 * returns months that actually had bookings.
 *
 * @param month the calendar month in ISO {@code yyyy-MM} form, e.g. {@code 2026-07}
 * @param revenue total revenue attributed to that month (0 when the host had none)
 */
public record MonthlyRevenueResponse(String month, BigDecimal revenue) {}
