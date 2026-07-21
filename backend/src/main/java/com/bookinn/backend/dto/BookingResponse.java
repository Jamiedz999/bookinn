package com.bookinn.backend.dto;

import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.domain.BookingStatus;
import com.bookinn.backend.domain.ListingPhoto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A booking as shown on both the guest's "my bookings" and the host's "bookings received" pages, so
 * it carries listing summary fields and the guest's name. {@code cancellable} is computed by the
 * service against the 48h policy and current time, letting the UI disable the cancel button without
 * re-deriving the rule.
 *
 * @param id booking id
 * @param listingId booked listing id
 * @param listingTitle booked listing title
 * @param listingCity booked listing city
 * @param coverPhotoUrl first listing photo URL, or {@code null}
 * @param guestId booking owner's id
 * @param guestName booking owner's name (for the host view)
 * @param checkIn inclusive check-in
 * @param checkOut exclusive check-out
 * @param nights number of booked nights
 * @param guestCount number of guests
 * @param totalPrice total price paid
 * @param status booking status
 * @param cancellable whether the owner may still cancel it now (CONFIRMED and ≥48h before check-in)
 * @param createdAt when the booking was placed
 * @param cancelledAt when it was cancelled, or {@code null}
 */
public record BookingResponse(
    Long id,
    Long listingId,
    String listingTitle,
    String listingCity,
    String coverPhotoUrl,
    Long guestId,
    String guestName,
    LocalDate checkIn,
    LocalDate checkOut,
    long nights,
    int guestCount,
    BigDecimal totalPrice,
    BookingStatus status,
    boolean cancellable,
    Instant createdAt,
    Instant cancelledAt) {

  /**
   * Projects a {@link Booking} entity to its response view. Must be called inside an open
   * transaction so the lazy listing/guest/photo associations can be read.
   *
   * @param booking the entity
   * @param cancellable whether the owner may still cancel it now
   * @return the response DTO
   */
  public static BookingResponse from(Booking booking, boolean cancellable) {
    String cover =
        booking.getListing().getPhotos().stream()
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(null);
    return new BookingResponse(
        booking.getId(),
        booking.getListing().getId(),
        booking.getListing().getTitle(),
        booking.getListing().getCity(),
        cover,
        booking.getGuest().getId(),
        booking.getGuest().getName(),
        booking.getCheckIn(),
        booking.getCheckOut(),
        booking.nights(),
        booking.getGuestCount(),
        booking.getTotalPrice(),
        booking.getStatus(),
        cancellable,
        booking.getCreatedAt(),
        booking.getCancelledAt());
  }
}
