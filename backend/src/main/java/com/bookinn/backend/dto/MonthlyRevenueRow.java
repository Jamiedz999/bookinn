package com.bookinn.backend.dto;

import java.math.BigDecimal;

/**
 * Spring Data projection for one row of the revenue-trend query — a single month that had revenue.
 * The service turns the sparse list of these into a dense, zero-filled 12-month series.
 */
public interface MonthlyRevenueRow {

  /**
   * The month in ISO {@code yyyy-MM} form (from {@code DATE_FORMAT(check_in, '%Y-%m')}).
   *
   * @return the month key
   */
  String getMonth();

  /**
   * Total revenue for that month.
   *
   * @return the summed {@code total_price}
   */
  BigDecimal getRevenue();
}
