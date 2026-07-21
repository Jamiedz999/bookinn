package com.bookinn.backend.repository;

import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.domain.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Booking} reservations. */
public interface BookingRepository extends JpaRepository<Booking, Long> {

  /**
   * Whether the listing already has a CONFIRMED booking overlapping the half-open range
   * {@code [checkIn, checkOut)}. Overlap is {@code existing.checkIn < newCheckOut AND
   * existing.checkOut > newCheckIn}; an adjacent booking (checkout == the other's check-in) does
   * not count. This is the create-time double-booking guard, run inside the transaction holding the
   * pessimistic lock on the listing row.
   *
   * @param listingId the listing being booked
   * @param checkIn requested check-in (inclusive)
   * @param checkOut requested check-out (exclusive)
   * @return {@code true} if a conflicting CONFIRMED booking exists
   */
  @Query(
      "SELECT COUNT(b) > 0 FROM Booking b "
          + "WHERE b.listing.id = :listingId AND b.status = com.bookinn.backend.domain."
          + "BookingStatus.CONFIRMED "
          + "AND b.checkIn < :checkOut AND b.checkOut > :checkIn")
  boolean existsOverlapping(
      @Param("listingId") Long listingId,
      @Param("checkIn") LocalDate checkIn,
      @Param("checkOut") LocalDate checkOut);

  /**
   * The guest's own bookings, newest first, for the "my bookings" page.
   *
   * @param guestId the booking owner's id
   * @return the guest's bookings
   */
  List<Booking> findByGuestIdOrderByCreatedAtDesc(Long guestId);

  /**
   * Bookings received across all listings owned by the host, newest first, for the "bookings
   * received" page.
   *
   * @param hostId the owning host's id
   * @return bookings on the host's listings
   */
  List<Booking> findByListingHostIdOrderByCreatedAtDesc(Long hostId);

  /**
   * Bookings in the given status whose checkout day is strictly before the cutoff — the daily job
   * uses this to find CONFIRMED bookings whose stay has ended so it can complete them.
   *
   * @param status the status to match
   * @param cutoff exclusive checkout cutoff (typically today)
   * @return the matching bookings
   */
  List<Booking> findByStatusAndCheckOutBefore(BookingStatus status, LocalDate cutoff);
}
