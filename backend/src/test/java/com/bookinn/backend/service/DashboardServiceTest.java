package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bookinn.backend.dto.DashboardSummaryResponse;
import com.bookinn.backend.dto.ListingOccupancyResponse;
import com.bookinn.backend.dto.ListingOccupancyRow;
import com.bookinn.backend.dto.MonthlyRevenueResponse;
import com.bookinn.backend.dto.MonthlyRevenueRow;
import com.bookinn.backend.repository.DashboardRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the two things {@link DashboardService} does on top of the SQL: zero-filling the
 * 12-month revenue series and turning booked-night counts into occupancy rates. The repository is
 * mocked and the clock is pinned to 15 Jul 2026, so "current month" is July (31 days) and the trend
 * window is Aug 2025 → Jul 2026.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC);
  private static final Long HOST_ID = 1L;

  @Mock private DashboardRepository dashboardRepository;

  private DashboardService service() {
    return new DashboardService(dashboardRepository, CLOCK);
  }

  @Test
  void summaryPassesMonthAndWeekBoundariesAndCombinesResults() {
    when(dashboardRepository.currentMonthRevenue(
            eq(HOST_ID), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 8, 1))))
        .thenReturn(new BigDecimal("580.00"));
    when(dashboardRepository.totalBookings(HOST_ID)).thenReturn(7L);
    when(dashboardRepository.upcomingCheckIns(
            eq(HOST_ID), eq(LocalDate.of(2026, 7, 15)), eq(LocalDate.of(2026, 7, 22))))
        .thenReturn(2L);

    DashboardSummaryResponse summary = service().getSummary(HOST_ID);

    assertThat(summary.currentMonthRevenue()).isEqualByComparingTo("580.00");
    assertThat(summary.totalBookings()).isEqualTo(7L);
    assertThat(summary.upcomingCheckIns()).isEqualTo(2L);
  }

  @Test
  void revenueTrendReturnsTwelveMonthsOldestFirstZeroFillingGaps() {
    when(dashboardRepository.revenueByMonth(
            eq(HOST_ID), eq(LocalDate.of(2025, 8, 1)), eq(LocalDate.of(2026, 8, 1))))
        .thenReturn(List.of(row("2025-09", "250.00"), row("2026-07", "580.00")));

    List<MonthlyRevenueResponse> trend = service().getRevenueTrend(HOST_ID);

    assertThat(trend).hasSize(12);
    assertThat(trend.get(0).month()).isEqualTo("2025-08");
    assertThat(trend.get(11).month()).isEqualTo("2026-07");
    assertThat(trend.get(0).revenue()).isEqualByComparingTo("0");
    assertThat(monthOf(trend, "2025-09")).isEqualByComparingTo("250.00");
    assertThat(monthOf(trend, "2026-07")).isEqualByComparingTo("580.00");
    assertThat(monthOf(trend, "2026-06")).isEqualByComparingTo("0");
  }

  @Test
  void occupancyDividesBookedNightsByDaysInMonthAndKeepsZeroListings() {
    when(dashboardRepository.bookedNightsByListing(
            eq(HOST_ID), eq(LocalDate.of(2026, 7, 1)), eq(LocalDate.of(2026, 8, 1))))
        .thenReturn(List.of(occ(10L, "Loft", 7L), occ(20L, "Cabin", 0L)));

    List<ListingOccupancyResponse> occupancy = service().getOccupancy(HOST_ID);

    assertThat(occupancy).hasSize(2);
    assertThat(occupancy.get(0).listingId()).isEqualTo(10L);
    assertThat(occupancy.get(0).bookedNights()).isEqualTo(7L);
    assertThat(occupancy.get(0).daysInMonth()).isEqualTo(31);
    assertThat(occupancy.get(0).rate()).isEqualTo(7.0 / 31);
    assertThat(occupancy.get(1).bookedNights()).isEqualTo(0L);
    assertThat(occupancy.get(1).rate()).isEqualTo(0.0);
  }

  private static BigDecimal monthOf(List<MonthlyRevenueResponse> trend, String month) {
    return trend.stream()
        .filter(point -> point.month().equals(month))
        .findFirst()
        .orElseThrow()
        .revenue();
  }

  private static MonthlyRevenueRow row(String month, String revenue) {
    return new MonthlyRevenueRow() {
      @Override
      public String getMonth() {
        return month;
      }

      @Override
      public BigDecimal getRevenue() {
        return new BigDecimal(revenue);
      }
    };
  }

  private static ListingOccupancyRow occ(Long id, String title, long nights) {
    return new ListingOccupancyRow() {
      @Override
      public Long getListingId() {
        return id;
      }

      @Override
      public String getListingTitle() {
        return title;
      }

      @Override
      public long getBookedNights() {
        return nights;
      }
    };
  }
}
