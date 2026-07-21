package com.bookinn.backend.repository;

import com.bookinn.backend.domain.Listing;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Listing} properties. */
public interface ListingRepository extends JpaRepository<Listing, Long> {

  /**
   * Loads a listing for update, taking a row-level pessimistic write lock ({@code SELECT ... FOR
   * UPDATE}). This is the single-line concurrency guard from PRD decision D4: the booking
   * transaction locks the listing row first, so two guests racing for the same dates are serialised
   * — the second waits here until the first commits, then sees its CONFIRMED booking in the overlap
   * check and is rejected. Must be called inside a transaction.
   *
   * @param id the listing id
   * @return the locked listing, if it exists
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM Listing l WHERE l.id = :id")
  Optional<Listing> findByIdForUpdate(@Param("id") Long id);

  /**
   * Lists a host's own properties, newest first.
   *
   * @param hostId the owning host's id
   * @return the host's listings
   */
  List<Listing> findByHostIdOrderByCreatedAtDesc(Long hostId);


  /**
   * Public search without a date window: ACTIVE listings, optionally filtered by a
   case-insensitive
   * city prefix. Pass an empty string for {@code city} to skip the city filter (avoids
   binding a
   * nullable parameter into a native query).
   *
   * @param city city prefix, or {@code ""} for no city filter
   * @param pageable page request (page + size)
   * @return the matching page, newest first
   */
  @Query(
          value =
                  "SELECT * FROM listing l WHERE l.status = 'ACTIVE' "
                          + "AND (:city = '' OR LOWER(l.city) LIKE LOWER(CONCAT(:city, '%'))) "
                          + "ORDER BY l.created_at DESC",
          countQuery =
                  "SELECT COUNT(*) FROM listing l WHERE l.status = 'ACTIVE' "
                          + "AND (:city = '' OR LOWER(l.city) LIKE LOWER(CONCAT(:city, '%')))",
          nativeQuery = true)
  Page<Listing> searchActive(@Param("city") String city, Pageable pageable);

  /**
   * Public search with a date window: ACTIVE listings matching the city prefix that have no
   * overlapping CONFIRMED booking. Overlap uses half-open intervals {@code [checkIn,
  checkOut)}, so
   * an adjacent booking (checkout == new check-in) does not block. References the {@code
  booking}
   * table directly via native SQL because the Booking entity is introduced in M4.
   *
   * @param city city prefix, or {@code ""} for no city filter
   * @param checkIn requested check-in (inclusive)
   * @param checkOut requested check-out (exclusive)
   * @param pageable page request (page + size)
   * @return the available matching page, newest first
   */
  @Query(
          value =
                  "SELECT * FROM listing l WHERE l.status = 'ACTIVE' "
                          + "AND (:city = '' OR LOWER(l.city) LIKE LOWER(CONCAT(:city, '%'))) "
                          + "AND NOT EXISTS (SELECT 1 FROM booking b WHERE b.listing_id = l.id "
                          + "AND b.status = 'CONFIRMED' "
                          + "AND b.check_in < :checkOut AND b.check_out > :checkIn) "
                          + "ORDER BY l.created_at DESC",
          countQuery =
                  "SELECT COUNT(*) FROM listing l WHERE l.status = 'ACTIVE' "
                          + "AND (:city = '' OR LOWER(l.city) LIKE LOWER(CONCAT(:city, '%'))) "
                          + "AND NOT EXISTS (SELECT 1 FROM booking b WHERE b.listing_id = l.id "
                          + "AND b.status = 'CONFIRMED' "
                          + "AND b.check_in < :checkOut AND b.check_out > :checkIn)",
          nativeQuery = true)
  Page<Listing> searchAvailable(
          @Param("city") String city,
          @Param("checkIn") LocalDate checkIn,
          @Param("checkOut") LocalDate checkOut,
          Pageable pageable);
}

