package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.DashboardSummaryResponse;
import com.bookinn.backend.dto.MonthlyRevenueResponse;
import com.bookinn.backend.repository.BookingRepository;
import com.bookinn.backend.repository.ListingRepository;
import com.bookinn.backend.repository.UserRepository;
import com.bookinn.backend.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * End-to-end tests for the demo reset (M6): truncate-and-reseed restores the exact seed state, the
 * demo accounts come back protected, the dashboard has a fully populated 12-month trend, and future
 * bookings exist so search date-filtering is observable.
 */
class DemoResetIntegrationTest extends AbstractIntegrationTest {

  @Autowired private DemoResetService demoResetService;
  @Autowired private DemoSeedService demoSeedService;
  @Autowired private UserRepository userRepository;
  @Autowired private ListingRepository listingRepository;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private com.bookinn.backend.service.DashboardService dashboardService;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void resetRestoresSeedStateAndWipesInterloperData() {
    // Reset from whatever the shared container currently holds → a clean seeded baseline.
    demoResetService.reset();
    long users = userRepository.count();
    long listings = listingRepository.count();
    long bookings = bookingRepository.count();
    assertThat(listings).isGreaterThanOrEqualTo(28);
    assertThat(bookings).isGreaterThan(0);

    // An interviewer messes things up: a junk account and a junk listing.
    User junk =
        userRepository.save(new User("junk@bookinn.app", "h", "Junk", false, EnumSet.of(Role.GUEST)));
    assertThat(userRepository.count()).isEqualTo(users + 1);

    // Nightly reset runs → identical counts, junk gone (ids reset by TRUNCATE make it deterministic).
    demoResetService.reset();
    assertThat(userRepository.count()).isEqualTo(users);
    assertThat(listingRepository.count()).isEqualTo(listings);
    assertThat(bookingRepository.count()).isEqualTo(bookings);
    assertThat(userRepository.findByEmail("junk@bookinn.app")).isEmpty();
  }

  @Test
  void reseededDemoAccountsAreProtectedPersonas() {
    demoResetService.reset();

    User demoHost = userRepository.findByEmail(DemoAccounts.HOST_EMAIL).orElseThrow();
    assertThat(demoHost.isDemo()).isTrue();
    assertThat(demoHost.getRoles()).contains(Role.HOST);

    User demoGuest = userRepository.findByEmail(DemoAccounts.GUEST_EMAIL).orElseThrow();
    assertThat(demoGuest.isDemo()).isTrue();
    assertThat(demoGuest.getRoles()).containsExactly(Role.GUEST);
  }

  @Test
  void demoHostDashboardHasFullTrendAndUpcomingCheckIns() {
    demoResetService.reset();
    Long demoHostId = userRepository.findByEmail(DemoAccounts.HOST_EMAIL).orElseThrow().getId();

    List<MonthlyRevenueResponse> trend = dashboardService.getRevenueTrend(demoHostId);
    assertThat(trend).hasSize(12);
    long monthsWithRevenue =
        trend.stream().filter(m -> m.revenue().compareTo(BigDecimal.ZERO) > 0).count();
    assertThat(monthsWithRevenue).isGreaterThanOrEqualTo(10);

    DashboardSummaryResponse summary = dashboardService.getSummary(demoHostId);
    assertThat(summary.totalBookings()).isGreaterThan(0);
    assertThat(summary.upcomingCheckIns()).isGreaterThan(0);
  }

  @Test
  void futureConfirmedBookingsExistForSearchFiltering() {
    demoResetService.reset();

    Integer futureConfirmed =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM booking WHERE status = 'CONFIRMED' AND check_in > CURRENT_DATE",
            Integer.class);
    assertThat(futureConfirmed).isGreaterThan(0);
  }
}
