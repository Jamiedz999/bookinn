package com.bookinn.backend.controller;

import com.bookinn.backend.dto.DashboardSummaryResponse;
import com.bookinn.backend.dto.ListingOccupancyResponse;
import com.bookinn.backend.dto.MonthlyRevenueResponse;
import com.bookinn.backend.security.AuthenticatedUser;
import com.bookinn.backend.service.DashboardService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Host dashboard endpoints. All three require the HOST role and report only on the authenticated
 * host's own listings and bookings; a GUEST gets 403, an anonymous caller 401. Everything is
 * computed live (PRD decision D6) — there is no pre-aggregation table.
 */
@RestController
@RequestMapping("/api/host/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * Creates the controller.
   *
   * @param dashboardService the dashboard aggregation service
   */
  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /**
   * Current-month revenue, all-time non-cancelled booking count, and check-ins in the next 7 days.
   *
   * @param principal the authenticated host
   * @return the summary KPIs
   */
  @GetMapping("/summary")
  @PreAuthorize("hasRole('HOST')")
  public DashboardSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser principal) {
    return dashboardService.getSummary(principal.id());
  }

  /**
   * Revenue for the last 12 months, oldest first, with empty months zero-filled.
   *
   * @param principal the authenticated host
   * @return the 12-month revenue series
   */
  @GetMapping("/revenue-trend")
  @PreAuthorize("hasRole('HOST')")
  public List<MonthlyRevenueResponse> revenueTrend(
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return dashboardService.getRevenueTrend(principal.id());
  }

  /**
   * Current-month occupancy per listing.
   *
   * @param principal the authenticated host
   * @return per-listing occupancy
   */
  @GetMapping("/occupancy")
  @PreAuthorize("hasRole('HOST')")
  public List<ListingOccupancyResponse> occupancy(
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return dashboardService.getOccupancy(principal.id());
  }
}
