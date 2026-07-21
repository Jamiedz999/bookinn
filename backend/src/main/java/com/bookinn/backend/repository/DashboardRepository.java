package com.bookinn.backend.repository;

import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.dto.ListingOccupancyRow;
import com.bookinn.backend.dto.MonthlyRevenueRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Live-aggregation queries behind the host dashboard (PRD decision D6: compute on the fly, no
 * pre-aggregation table). Every query is scoped to a single host and, by joining {@code listing} to
 * {@code booking}, never leaks another host's data. All revenue/count queries ignore CANCELLED
 * bookings and attribute a booking to a month by its {@code check_in} date.
 *
 * <p>Native SQL is used throughout because the interesting parts — {@code DATE_FORMAT} for the
 * month key and {@code DATEDIFF(LEAST(...), GREATEST(...))} for clipping a stay to the current
 * month — are database functions with no JPQL equivalent. This is a read-only marker repository
 * over {@link Booking}; it exposes no write methods.
 */
public interface DashboardRepository extends Repository<Booking, Long> {

  /**
   * Sum of {@code total_price} for the host's CONFIRMED/COMPLETED bookings whose check-in falls in
   * the half-open month {@code [monthStart, monthEnd)}. Returns 0, never {@code null}.
   *
   * @param hostId the owning host
   * @param monthStart first day of the current month (inclusive)
   * @param monthEnd first day of the next month (exclusive)
   * @return the month's revenue
   */
  @Query(
      value =
          "SELECT COALESCE(SUM(b.total_price), 0) FROM booking b "
              + "JOIN listing l ON l.id = b.listing_id "
              + "WHERE l.host_id = :hostId AND b.status IN ('CONFIRMED', 'COMPLETED') "
              + "AND b.check_in >= :monthStart AND b.check_in < :monthEnd",
      nativeQuery = true)
  BigDecimal currentMonthRevenue(
      @Param("hostId") Long hostId,
      @Param("monthStart") LocalDate monthStart,
      @Param("monthEnd") LocalDate monthEnd);

  /**
   * Count of the host's non-cancelled bookings, all time.
   *
   * @param hostId the owning host
   * @return the running booking count
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM booking b JOIN listing l ON l.id = b.listing_id "
              + "WHERE l.host_id = :hostId AND b.status IN ('CONFIRMED', 'COMPLETED')",
      nativeQuery = true)
  long totalBookings(@Param("hostId") Long hostId);

  /**
   * Count of the host's CONFIRMED bookings checking in within the half-open window
   * {@code [today, weekEnd)}.
   *
   * @param hostId the owning host
   * @param today lower bound (inclusive)
   * @param weekEnd upper bound (exclusive), typically {@code today + 7 days}
   * @return the number of upcoming check-ins
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM booking b JOIN listing l ON l.id = b.listing_id "
              + "WHERE l.host_id = :hostId AND b.status = 'CONFIRMED' "
              + "AND b.check_in >= :today AND b.check_in < :weekEnd",
      nativeQuery = true)
  long upcomingCheckIns(
      @Param("hostId") Long hostId,
      @Param("today") LocalDate today,
      @Param("weekEnd") LocalDate weekEnd);

  /**
   * Revenue grouped by check-in month for the host's CONFIRMED/COMPLETED bookings within the
   * half-open window {@code [windowStart, windowEnd)}. Only months that actually had revenue are
   * returned; the service zero-fills the rest. Ordered oldest month first.
   *
   * @param hostId the owning host
   * @param windowStart first day of the oldest month in the window (inclusive)
   * @param windowEnd first day of the month after the newest (exclusive)
   * @return one row per month that had revenue
   */
  @Query(
      value =
          "SELECT DATE_FORMAT(b.check_in, '%Y-%m') AS month, SUM(b.total_price) AS revenue "
              + "FROM booking b JOIN listing l ON l.id = b.listing_id "
              + "WHERE l.host_id = :hostId AND b.status IN ('CONFIRMED', 'COMPLETED') "
              + "AND b.check_in >= :windowStart AND b.check_in < :windowEnd "
              + "GROUP BY month ORDER BY month",
      nativeQuery = true)
  List<MonthlyRevenueRow> revenueByMonth(
      @Param("hostId") Long hostId,
      @Param("windowStart") LocalDate windowStart,
      @Param("windowEnd") LocalDate windowEnd);

  /**
   * Booked nights inside the current month per listing owned by the host. Every listing appears (a
   * {@code LEFT JOIN} keeps those with no bookings, showing 0 nights); a booking contributes only
   * the nights that fall inside {@code [monthStart, monthEnd)}, so a stay straddling a month
   * boundary is clipped by {@code DATEDIFF(LEAST(check_out, monthEnd), GREATEST(check_in,
   * monthStart))}. The join predicate keeps only bookings that actually intersect the month, so the
   * clipped value is always positive. Cast to SIGNED so the driver returns a plain integer. Ordered
   * by listing id.
   *
   * @param hostId the owning host
   * @param monthStart first day of the current month (inclusive)
   * @param monthEnd first day of the next month (exclusive)
   * @return one row per listing with its in-month booked nights
   */
  @Query(
      value =
          "SELECT l.id AS listingId, l.title AS listingTitle, "
              + "CAST(COALESCE(SUM(DATEDIFF(LEAST(b.check_out, :monthEnd), "
              + "GREATEST(b.check_in, :monthStart))), 0) AS SIGNED) AS bookedNights "
              + "FROM listing l LEFT JOIN booking b ON b.listing_id = l.id "
              + "AND b.status IN ('CONFIRMED', 'COMPLETED') "
              + "AND b.check_in < :monthEnd AND b.check_out > :monthStart "
              + "WHERE l.host_id = :hostId "
              + "GROUP BY l.id, l.title ORDER BY l.id",
      nativeQuery = true)
  List<ListingOccupancyRow> bookedNightsByListing(
      @Param("hostId") Long hostId,
      @Param("monthStart") LocalDate monthStart,
      @Param("monthEnd") LocalDate monthEnd);
}
