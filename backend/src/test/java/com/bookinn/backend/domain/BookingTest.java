package com.bookinn.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookinn.backend.exception.IllegalBookingTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Booking} state machine and night calculation. */
class BookingTest {

  private static final Instant WHEN = Instant.parse("2026-07-21T10:00:00Z");

  private Booking confirmed() {
    return new Booking(
        new Listing(),
        new User(),
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 8, 4),
        2,
        new BigDecimal("300.00"));
  }

  @Test
  void nightsIsHalfOpenDayCount() {
    assertThat(confirmed().nights()).isEqualTo(3);
  }

  @Test
  void cancelMovesToCancelledAndRecordsTime() {
    Booking booking = confirmed();

    booking.cancel(WHEN);

    assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    assertThat(booking.getCancelledAt()).isEqualTo(WHEN);
  }

  @Test
  void completeMovesToCompleted() {
    Booking booking = confirmed();

    booking.complete();

    assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
  }

  @Test
  void cancellingATerminalBookingIsRejected() {
    Booking booking = confirmed();
    booking.cancel(WHEN);

    assertThatThrownBy(() -> booking.cancel(WHEN))
        .isInstanceOf(IllegalBookingTransitionException.class);
  }

  @Test
  void completingACancelledBookingIsRejected() {
    Booking booking = confirmed();
    booking.cancel(WHEN);

    assertThatThrownBy(booking::complete).isInstanceOf(IllegalBookingTransitionException.class);
  }

  @Test
  void cancellingACompletedBookingIsRejected() {
    Booking booking = confirmed();
    booking.complete();

    assertThatThrownBy(() -> booking.cancel(WHEN))
        .isInstanceOf(IllegalBookingTransitionException.class);
  }
}
