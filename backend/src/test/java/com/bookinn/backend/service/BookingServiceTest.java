package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.domain.BookingStatus;
import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.ListingStatus;
import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.BookingResponse;
import com.bookinn.backend.dto.CreateBookingRequest;
import com.bookinn.backend.dto.QuoteResponse;
import com.bookinn.backend.exception.BookingConflictException;
import com.bookinn.backend.exception.CancellationNotAllowedException;
import com.bookinn.backend.exception.InvalidBookingRequestException;
import com.bookinn.backend.exception.InvalidDateRangeException;
import com.bookinn.backend.exception.ListingNotFoundException;
import com.bookinn.backend.repository.BookingRepository;
import com.bookinn.backend.repository.ListingRepository;
import com.bookinn.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link BookingService}: pricing, the overlap-conflict guard, the 48h cancellation
 * boundary, ownership, guest-count validation, and the completion sweep. The overlap SQL itself is
 * exercised against a real database in the repository integration test; here {@code
 * existsOverlapping} is stubbed.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

  private static final long GUEST_ID = 7L;
  private static final long LISTING_ID = 42L;

  @Mock private BookingRepository bookingRepository;
  @Mock private ListingRepository listingRepository;
  @Mock private UserRepository userRepository;

  /** Builds the service with the clock pinned to the given instant. */
  private BookingService serviceAt(Instant now) {
    return new BookingService(
        bookingRepository,
        listingRepository,
        userRepository,
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private User guest(long id) {
    User user = new User("guest@example.com", "enc", "Greta Guest", false, EnumSet.of(Role.GUEST));
    user.setId(id);
    return user;
  }

  private Listing listing(BigDecimal price, int maxGuests, ListingStatus status) {
    Listing listing = new Listing();
    listing.setId(LISTING_ID);
    listing.setHost(guest(1000L));
    listing.setTitle("Sea view loft");
    listing.setCity("Lisbon");
    listing.setPricePerNight(price);
    listing.setMaxGuests(maxGuests);
    listing.setStatus(status);
    return listing;
  }

  private Booking booking(User owner, LocalDate checkIn, LocalDate checkOut) {
    return new Booking(
        listing(new BigDecimal("100.00"), 4, ListingStatus.ACTIVE),
        owner,
        checkIn,
        checkOut,
        2,
        new BigDecimal("100.00"));
  }

  // --- create -------------------------------------------------------------

  @Test
  void createPricesNightsTimesRate() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    when(listingRepository.findByIdForUpdate(LISTING_ID))
        .thenReturn(Optional.of(listing(new BigDecimal("100.00"), 4, ListingStatus.ACTIVE)));
    when(bookingRepository.existsOverlapping(eq(LISTING_ID), any(), any())).thenReturn(false);
    when(userRepository.findById(GUEST_ID)).thenReturn(Optional.of(guest(GUEST_ID)));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

    BookingResponse response =
        service.create(
            GUEST_ID,
            new CreateBookingRequest(
                LISTING_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4), 2));

    assertThat(response.nights()).isEqualTo(3);
    assertThat(response.totalPrice()).isEqualByComparingTo("300.00");
    assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
  }

  @Test
  void createRejectsOverlapWith409() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    when(listingRepository.findByIdForUpdate(LISTING_ID))
        .thenReturn(Optional.of(listing(new BigDecimal("100.00"), 4, ListingStatus.ACTIVE)));
    when(bookingRepository.existsOverlapping(eq(LISTING_ID), any(), any())).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.create(
                    GUEST_ID,
                    new CreateBookingRequest(
                        LISTING_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4), 2)))
        .isInstanceOf(BookingConflictException.class);
    verify(bookingRepository, never()).save(any());
  }

  @Test
  void createRejectsGuestCountOverCapacity() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    when(listingRepository.findByIdForUpdate(LISTING_ID))
        .thenReturn(Optional.of(listing(new BigDecimal("100.00"), 2, ListingStatus.ACTIVE)));

    assertThatThrownBy(
            () ->
                service.create(
                    GUEST_ID,
                    new CreateBookingRequest(
                        LISTING_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4), 3)))
        .isInstanceOf(InvalidBookingRequestException.class);
  }

  @Test
  void createRejectsInactiveListingAsNotFound() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    when(listingRepository.findByIdForUpdate(LISTING_ID))
        .thenReturn(Optional.of(listing(new BigDecimal("100.00"), 4, ListingStatus.INACTIVE)));

    assertThatThrownBy(
            () ->
                service.create(
                    GUEST_ID,
                    new CreateBookingRequest(
                        LISTING_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4), 2)))
        .isInstanceOf(ListingNotFoundException.class);
  }

  @Test
  void createRejectsInvertedDateRange() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));

    assertThatThrownBy(
            () ->
                service.create(
                    GUEST_ID,
                    new CreateBookingRequest(
                        LISTING_ID, LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 1), 2)))
        .isInstanceOf(InvalidDateRangeException.class);
  }

  // --- quote --------------------------------------------------------------

  @Test
  void quoteReturnsBreakdownForAvailableDates() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    when(listingRepository.findById(LISTING_ID))
        .thenReturn(Optional.of(listing(new BigDecimal("120.00"), 4, ListingStatus.ACTIVE)));
    when(bookingRepository.existsOverlapping(eq(LISTING_ID), any(), any())).thenReturn(false);

    QuoteResponse quote =
        service.quote(LISTING_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));

    assertThat(quote.nights()).isEqualTo(2);
    assertThat(quote.totalPrice()).isEqualByComparingTo("240.00");
  }

  @Test
  void quoteRejectsUnavailableDatesWith409() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    when(listingRepository.findById(LISTING_ID))
        .thenReturn(Optional.of(listing(new BigDecimal("120.00"), 4, ListingStatus.ACTIVE)));
    when(bookingRepository.existsOverlapping(eq(LISTING_ID), any(), any())).thenReturn(true);

    assertThatThrownBy(
            () -> service.quote(LISTING_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)))
        .isInstanceOf(BookingConflictException.class);
  }

  // --- cancel -------------------------------------------------------------

  @Test
  void cancelByNonOwnerIsForbidden() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    Booking booking = booking(guest(GUEST_ID), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4));
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> service.cancel(999L, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void cancelOfMissingBookingIsForbidden() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.cancel(GUEST_ID, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void cancelIsAllowedExactly48hBeforeCheckIn() {
    // check-in 2026-08-03 00:00Z; exactly 48h earlier is 2026-08-01 00:00Z.
    BookingService service = serviceAt(Instant.parse("2026-08-01T00:00:00Z"));
    Booking booking = booking(guest(GUEST_ID), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 6));
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

    BookingResponse response = service.cancel(GUEST_ID, 1L);

    assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
  }

  @Test
  void cancelIsRejectedJustInsideThe48hWindow() {
    // One second past the 48h boundary.
    BookingService service = serviceAt(Instant.parse("2026-08-01T00:00:01Z"));
    Booking booking = booking(guest(GUEST_ID), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 6));
    when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> service.cancel(GUEST_ID, 1L))
        .isInstanceOf(CancellationNotAllowedException.class);
  }

  // --- completion sweep ---------------------------------------------------

  @Test
  void completePastBookingsCompletesConfirmedStays() {
    BookingService service = serviceAt(Instant.parse("2026-08-10T00:00:00Z"));
    Booking past = booking(guest(GUEST_ID), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4));
    when(bookingRepository.findByStatusAndCheckOutBefore(
            eq(BookingStatus.CONFIRMED), any(LocalDate.class)))
        .thenReturn(List.of(past));

    int completed = service.completePastBookings();

    assertThat(completed).isEqualTo(1);
    assertThat(past.getStatus()).isEqualTo(BookingStatus.COMPLETED);
  }

  // --- queries ------------------------------------------------------------

  @Test
  void getMyBookingsMapsGuestBookings() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    Booking booking = booking(guest(GUEST_ID), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4));
    when(bookingRepository.findByGuestIdOrderByCreatedAtDesc(GUEST_ID))
        .thenReturn(List.of(booking));

    List<BookingResponse> mine = service.getMyBookings(GUEST_ID);

    assertThat(mine).hasSize(1);
    assertThat(mine.get(0).cancellable()).isTrue();
  }

  @Test
  void getHostBookingsMapsReceivedBookings() {
    BookingService service = serviceAt(Instant.parse("2026-07-21T00:00:00Z"));
    Booking booking = booking(guest(GUEST_ID), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 4));
    when(bookingRepository.findByListingHostIdOrderByCreatedAtDesc(1000L))
        .thenReturn(List.of(booking));

    List<BookingResponse> received = service.getHostBookings(1000L);

    assertThat(received).hasSize(1);
  }
}
