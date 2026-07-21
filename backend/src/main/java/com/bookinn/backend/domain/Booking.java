package com.bookinn.backend.domain;

import com.bookinn.backend.exception.IllegalBookingTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A guest's reservation of a listing over the half-open date range {@code [checkIn, checkOut)} —
 * the checkout day is not occupied, so an adjacent booking (checkout == next check-in) does not
 * overlap. Carries its own state machine: {@link #cancel} and {@link #complete} are the only
 * transitions and
 * both reject a booking already in a terminal state, which is the "illegal transition" defence from
 * PRD §4. Maps to the {@code booking} table defined in Flyway V1.
 */
@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Booked listing. Its host is the recipient shown on the "bookings received" page. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "listing_id", nullable = false)
  private Listing listing;

  /** Guest who placed and owns the booking; only they may cancel it. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "guest_id", nullable = false)
  private User guest;

  /** Inclusive first night. */
  @Column(name = "check_in", nullable = false)
  private LocalDate checkIn;

  /** Exclusive checkout day; not itself a booked night. */
  @Column(name = "check_out", nullable = false)
  private LocalDate checkOut;

  @Column(name = "guest_count", nullable = false)
  private int guestCount;

  @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingStatus status = BookingStatus.CONFIRMED;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** When the booking was cancelled, or {@code null} if it was never cancelled. */
  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  /**
   * Creates a CONFIRMED booking. Callers are responsible for having validated availability and
   * computed {@code totalPrice}; this constructor does not re-check them.
   *
   * @param listing the booked listing
   * @param guest the booking guest and owner
   * @param checkIn inclusive first night
   * @param checkOut exclusive checkout day
   * @param guestCount number of guests
   * @param totalPrice nights × nightly price
   */
  public Booking(
      Listing listing,
      User guest,
      LocalDate checkIn,
      LocalDate checkOut,
      int guestCount,
      BigDecimal totalPrice) {
    this.listing = listing;
    this.guest = guest;
    this.checkIn = checkIn;
    this.checkOut = checkOut;
    this.guestCount = guestCount;
    this.totalPrice = totalPrice;
    this.status = BookingStatus.CONFIRMED;
  }

  /**
   * Number of booked nights, i.e. {@code checkOut − checkIn} for the half-open range.
   *
   * @return the night count
   */
  public long nights() {
    return ChronoUnit.DAYS.between(checkIn, checkOut);
  }

  /**
   * Cancels the booking, recording when. Only a CONFIRMED booking may be cancelled; cancelling a
   * booking already in a terminal state is an illegal transition. The 48h free-cancellation policy
   * and owner check are enforced by the service before this is called — the entity guards only the
   * state machine.
   *
   * @param when the cancellation timestamp
   * @throws IllegalBookingTransitionException if the booking is not CONFIRMED
   */
  public void cancel(Instant when) {
    requireConfirmed("cancel");
    this.status = BookingStatus.CANCELLED;
    this.cancelledAt = when;
  }

  /**
   * Marks the booking COMPLETED, used by the daily job once checkout has passed. Only a CONFIRMED
   * booking may complete.
   *
   * @throws IllegalBookingTransitionException if the booking is not CONFIRMED
   */
  public void complete() {
    requireConfirmed("complete");
    this.status = BookingStatus.COMPLETED;
  }

  private void requireConfirmed(String action) {
    if (status != BookingStatus.CONFIRMED) {
      throw new IllegalBookingTransitionException(
          "Cannot " + action + " a booking in terminal state " + status);
    }
  }
}
