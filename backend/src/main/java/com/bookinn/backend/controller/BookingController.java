package com.bookinn.backend.controller;

import com.bookinn.backend.dto.BookingResponse;
import com.bookinn.backend.dto.CreateBookingRequest;
import com.bookinn.backend.security.AuthenticatedUser;
import com.bookinn.backend.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booking endpoints. Creating and listing one's own bookings require the GUEST role; the host view
 * requires HOST. Cancellation only requires authentication — ownership is enforced in the service,
 * which also returns 403 for a non-owner or missing booking.
 */
@RestController
public class BookingController {

  private final BookingService bookingService;

  /**
   * Creates the controller.
   *
   * @param bookingService booking operations
   */
  public BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  /**
   * Creates a booking for the authenticated guest.
   *
   * @param principal the authenticated guest
   * @param request the booking payload
   * @return 201 with the created booking
   */
  @PostMapping("/api/bookings")
  @PreAuthorize("hasRole('GUEST')")
  public ResponseEntity<BookingResponse> create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody CreateBookingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(bookingService.create(principal.id(), request));
  }

  /**
   * Lists the authenticated guest's own bookings, newest first.
   *
   * @param principal the authenticated guest
   * @return the guest's bookings
   */
  @GetMapping("/api/bookings/my")
  @PreAuthorize("hasRole('GUEST')")
  public List<BookingResponse> myBookings(@AuthenticationPrincipal AuthenticatedUser principal) {
    return bookingService.getMyBookings(principal.id());
  }

  /**
   * Lists bookings received on the authenticated host's listings, newest first.
   *
   * @param principal the authenticated host
   * @return bookings on the host's listings
   */
  @GetMapping("/api/host/bookings")
  @PreAuthorize("hasRole('HOST')")
  public List<BookingResponse> hostBookings(@AuthenticationPrincipal AuthenticatedUser principal) {
    return bookingService.getHostBookings(principal.id());
  }

  /**
   * Cancels one of the caller's own bookings, subject to the 48h policy and state machine.
   *
   * @param principal the authenticated caller
   * @param id id of the booking to cancel
   * @return 200 with the booking after cancellation
   */
  @PostMapping("/api/bookings/{id}/cancel")
  @PreAuthorize("isAuthenticated()")
  public BookingResponse cancel(
      @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
    return bookingService.cancel(principal.id(), id);
  }
}
