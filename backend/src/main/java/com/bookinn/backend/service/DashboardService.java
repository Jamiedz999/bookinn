package com.bookinn.backend.service;

import com.bookinn.backend.dto.DashboardSummaryResponse;
import com.bookinn.backend.dto.ListingOccupancyResponse;
import com.bookinn.backend.dto.ListingOccupancyRow;
import com.bookinn.backend.dto.MonthlyRevenueResponse;
import com.bookinn.backend.dto.MonthlyRevenueRow;
import com.bookinn.backend.repository.DashboardRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the host dashboard from the live-aggregation queries in {@link DashboardRepository}.
 * The SQL does the heavy counting/summing; this class only does the two things SQL is awkward at:
 * zero-filling the months a host had no revenue, and turning booked-night counts into occupancy
 * rates. "Current month" and "today" are read from an injected {@link Clock} so tests can pin time.
 */
@Service
public class DashboardService {

  /** Length of the revenue-trend window, in months, including the current month. */
  private static final int TREND_MONTHS = 12;

  /** Look-ahead window for the "upcoming check-ins" KPI, in days. */
  private static final int UPCOMING_DAYS = 7;

  private final DashboardRepository dashboardRepository;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param dashboardRepository the live-aggregation queries
   * @param clock time source for "current month" and "today"
   */
  public DashboardService(DashboardRepository dashboardRepository, Clock clock) {
    this.dashboardRepository = dashboardRepository;
    this.clock = clock;
  }

  /**
   * The three headline numbers: current-month revenue, all-time non-cancelled booking count, and
   * check-ins in the next {@value #UPCOMING_DAYS} days.
   *
   * @param hostId the authenticated host
   * @return the summary KPIs
   */
  @Transactional(readOnly = true)
  public DashboardSummaryResponse getSummary(Long hostId) {
    YearMonth month = YearMonth.now(clock);
    LocalDate monthStart = month.atDay(1);
    LocalDate monthEnd = month.plusMonths(1).atDay(1);
    LocalDate today = LocalDate.now(clock);

    BigDecimal revenue = dashboardRepository.currentMonthRevenue(hostId, monthStart, monthEnd);
    long total = dashboardRepository.totalBookings(hostId);
    long upcoming =
        dashboardRepository.upcomingCheckIns(hostId, today, today.plusDays(UPCOMING_DAYS));

    return new DashboardSummaryResponse(revenue, total, upcoming);
  }

  /**
   * Revenue for each of the last {@value #TREND_MONTHS} months, oldest first, with months that had
   * no revenue filled in as zero. The query returns only non-empty months, so a host who started a
   * few months ago still gets a full 12-point line (the earlier points are zero).
   *
   * @param hostId the authenticated host
   * @return the dense 12-month series
   */
  @Transactional(readOnly = true)
  public List<MonthlyRevenueResponse> getRevenueTrend(Long hostId) {
    YearMonth current = YearMonth.now(clock);
    YearMonth start = current.minusMonths(TREND_MONTHS - 1L);
    LocalDate windowStart = start.atDay(1);
    LocalDate windowEnd = current.plusMonths(1).atDay(1);

    Map<String, BigDecimal> revenueByMonth =
        dashboardRepository.revenueByMonth(hostId, windowStart, windowEnd).stream()
            .collect(
                Collectors.toMap(MonthlyRevenueRow::getMonth, MonthlyRevenueRow::getRevenue));

    return IntStream.range(0, TREND_MONTHS)
        .mapToObj(start::plusMonths)
        .map(YearMonth::toString)
        .map(
            key ->
                new MonthlyRevenueResponse(
                    key, revenueByMonth.getOrDefault(key, BigDecimal.ZERO)))
        .toList();
  }

  /**
   * Current-month occupancy for each of the host's listings (including those with no bookings this
   * month, shown at 0). The rate is in-month booked nights divided by the number of days in the
   * current month.
   *
   * @param hostId the authenticated host
   * @return per-listing occupancy, ordered by listing id
   */
  @Transactional(readOnly = true)
  public List<ListingOccupancyResponse> getOccupancy(Long hostId) {
    YearMonth month = YearMonth.now(clock);
    LocalDate monthStart = month.atDay(1);
    LocalDate monthEnd = month.plusMonths(1).atDay(1);
    int daysInMonth = month.lengthOfMonth();

    return dashboardRepository.bookedNightsByListing(hostId, monthStart, monthEnd).stream()
        .map(row -> toOccupancy(row, daysInMonth))
        .toList();
  }

  private ListingOccupancyResponse toOccupancy(ListingOccupancyRow row, int daysInMonth) {
    double rate = daysInMonth == 0 ? 0.0 : (double) row.getBookedNights() / daysInMonth;
    return new ListingOccupancyResponse(
        row.getListingId(), row.getListingTitle(), row.getBookedNights(), daysInMonth, rate);
  }
}
