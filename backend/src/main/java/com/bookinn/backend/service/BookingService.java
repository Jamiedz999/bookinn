package com.bookinn.backend.service;

import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.domain.BookingStatus;
import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.ListingStatus;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.BookingResponse;
import com.bookinn.backend.dto.CreateBookingRequest;
import com.bookinn.backend.dto.QuoteResponse;
import com.bookinn.backend.exception.BookingConflictException;
import com.bookinn.backend.exception.CancellationNotAllowedException;
import com.bookinn.backend.exception.InvalidBookingRequestException;
import com.bookinn.backend.exception.InvalidCredentialsException;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking core: quote, create (with the double-booking guard), cancel (with the 48h policy and
 * ownership check), the guest/host listing queries, and the daily completion sweep. Time-dependent
 * rules (the 48h window, "past checkout") read a {@link Clock} so tests can pin the clock.
 */
@Service
public class BookingService {

  /** Free-cancellation cutoff: a booking may be cancelled up to this many hours before check-in. */
  private static final long FREE_CANCELLATION_HOURS = 48;

  private final BookingRepository bookingRepository;
  private final ListingRepository listingRepository;
  private final UserRepository userRepository;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param bookingRepository booking store
   * @param listingRepository listing store, also the source of the pessimistic lock
   * @param userRepository user store, for attaching the booking guest
   * @param clock time source for the 48h policy and completion sweep
   */
  public BookingService(
      BookingRepository bookingRepository,
      ListingRepository listingRepository,
      UserRepository userRepository,
      Clock clock) {
    this.bookingRepository = bookingRepository;
    this.listingRepository = listingRepository;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  /**
   * Prices a prospective stay and confirms the dates are currently available, without creating
   * anything. A range with no availability is a {@link BookingConflictException} (409) rather than
   * a quote.
   *
   * @param listingId the listing to quote
   * @param checkIn inclusive check-in
   * @param checkOut exclusive check-out
   * @return the price breakdown
   */
  @Transactional(readOnly = true)
  public QuoteResponse quote(Long listingId, LocalDate checkIn, LocalDate checkOut) {
    validateRange(checkIn, checkOut);
    Listing listing = activeListing(listingId);
    if (bookingRepository.existsOverlapping(listingId, checkIn, checkOut)) {
      throw new BookingConflictException("Listing is not available for the selected dates");
    }
    long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    return new QuoteResponse(
        listingId,
        checkIn,
        checkOut,
        nights,
        listing.getPricePerNight(),
        totalPrice(listing, nights));
  }

  /**
   * Creates a CONFIRMED booking. Runs in one transaction: takes a pessimistic write lock on the
   * listing row (PRD D4) so racing bookings are serialised, then checks for an overlapping
   * CONFIRMED booking, prices the stay, and inserts. The lock makes the overlap check reliable
   * under concurrency — the second of two racing transactions blocks on the lock, then sees the
   * booking and is rejected with 409.
   *
   * @param guestId id of the authenticated guest
   * @param request the booking payload
   * @return the created booking
   */
  @Transactional
  public BookingResponse create(Long guestId, CreateBookingRequest request) {
    validateRange(request.checkIn(), request.checkOut());

    Listing listing =
        listingRepository
            .findByIdForUpdate(request.listingId())
            .filter(candidate -> candidate.getStatus() == ListingStatus.ACTIVE)
            .orElseThrow(
                () -> new ListingNotFoundException("Listing not found: " + request.listingId()));

    if (request.guestCount() > listing.getMaxGuests()) {
      throw new InvalidBookingRequestException(
          "guestCount exceeds the listing's maximum of " + listing.getMaxGuests());
    }

    if (bookingRepository.existsOverlapping(
        listing.getId(), request.checkIn(), request.checkOut())) {
      throw new BookingConflictException("Listing is not available for the selected dates");
    }

    User guest =
        userRepository
            .findById(guestId)
            .orElseThrow(
                () -> new InvalidCredentialsException("Authenticated user no longer exists"));

    long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
    Booking booking =
        new Booking(
            listing,
            guest,
            request.checkIn(),
            request.checkOut(),
            request.guestCount(),
            totalPrice(listing, nights));
    bookingRepository.save(booking);
    return BookingResponse.from(booking, cancellable(booking));
  }

  /**
   * Cancels a booking on behalf of its owner. Enforces, in order: ownership (a non-owner — or a
   * missing booking, to avoid probing ids — is a 403), the 48h free-cancellation policy (409), and
   * the state machine (cancelling a terminal booking is a 409, guarded inside the entity).
   *
   * @param userId id of the authenticated caller
   * @param bookingId id of the booking to cancel
   * @return the booking after cancellation
   */
  @Transactional
  public BookingResponse cancel(Long userId, Long bookingId) {
    Booking booking =
        bookingRepository
            .findById(bookingId)
            .orElseThrow(
                () -> new AccessDeniedException("Not the owner of the requested booking"));
    if (!booking.getGuest().getId().equals(userId)) {
      throw new AccessDeniedException("Not the owner of the requested booking");
    }
    if (!withinFreeCancellationWindow(booking)) {
      throw new CancellationNotAllowedException(
          "Cancellation is only free up to " + FREE_CANCELLATION_HOURS + "h before check-in");
    }
    booking.cancel(Instant.now(clock));
    return BookingResponse.from(booking, cancellable(booking));
  }

  /**
   * The guest's own bookings, newest first.
   *
   * @param guestId id of the authenticated guest
   * @return the guest's bookings
   */
  @Transactional(readOnly = true)
  public List<BookingResponse> getMyBookings(Long guestId) {
    return bookingRepository.findByGuestIdOrderByCreatedAtDesc(guestId).stream()
        .map(booking -> BookingResponse.from(booking, cancellable(booking)))
        .toList();
  }

  /**
   * Bookings received on the host's listings, newest first.
   *
   * @param hostId id of the authenticated host
   * @return bookings on the host's listings
   */
  @Transactional(readOnly = true)
  public List<BookingResponse> getHostBookings(Long hostId) {
    return bookingRepository.findByListingHostIdOrderByCreatedAtDesc(hostId).stream()
        .map(booking -> BookingResponse.from(booking, cancellable(booking)))
        .toList();
  }

  /**
   * Transitions every CONFIRMED booking whose stay has ended (checkout strictly before today) to
   * COMPLETED. Invoked by the daily scheduled job; returns the count for logging/tests.
   *
   * @return the number of bookings completed
   */
  @Transactional
  public int completePastBookings() {
    LocalDate today = LocalDate.now(clock);
    List<Booking> due =
        bookingRepository.findByStatusAndCheckOutBefore(BookingStatus.CONFIRMED, today);
    due.forEach(Booking::complete);
    return due.size();
  }

  private Listing activeListing(Long listingId) {
    return listingRepository
        .findById(listingId)
        .filter(candidate -> candidate.getStatus() == ListingStatus.ACTIVE)
        .orElseThrow(() -> new ListingNotFoundException("Listing not found: " + listingId));
  }

  private void validateRange(LocalDate checkIn, LocalDate checkOut) {
    if (checkIn == null || checkOut == null || !checkIn.isBefore(checkOut)) {
      throw new InvalidDateRangeException("checkIn must be before checkOut");
    }
  }

  private BigDecimal totalPrice(Listing listing, long nights) {
    return listing.getPricePerNight().multiply(BigDecimal.valueOf(nights));
  }

  private boolean cancellable(Booking booking) {
    return booking.getStatus() == BookingStatus.CONFIRMED && withinFreeCancellationWindow(booking);
  }

  /**
   * Whether "now" is at or before {@code checkIn − 48h}. The boundary is inclusive: a request
   * landing exactly 48h before check-in is still allowed (PRD: {@code now <= check_in − 48h}).
   */
  private boolean withinFreeCancellationWindow(Booking booking) {
    Instant deadline =
        booking
            .getCheckIn()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .minus(FREE_CANCELLATION_HOURS, ChronoUnit.HOURS);
    return !Instant.now(clock).isAfter(deadline);
  }
}
