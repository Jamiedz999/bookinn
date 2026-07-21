package com.bookinn.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.domain.BookingStatus;
import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Exercises the overlap SQL in {@link BookingRepository#existsOverlapping} against real MySQL — the
 * "overlap matrix" the PRD calls out. A single CONFIRMED booking over {@code [Aug 10, Aug 15)} is
 * the fixture; every case probes a different relationship to it.
 */
class BookingRepositoryIntegrationTest extends AbstractIntegrationTest {

  @Autowired private BookingRepository bookingRepository;
  @Autowired private ListingRepository listingRepository;
  @Autowired private UserRepository userRepository;

  private Long listingId;

  @BeforeEach
  void setUp() {
    bookingRepository.deleteAll();
    listingRepository.deleteAll();
    userRepository.deleteAll();

    User host =
        userRepository.save(
            new User("host@example.com", "enc", "Holly", false, EnumSet.of(Role.HOST)));
    User guest =
        userRepository.save(
            new User("guest@example.com", "enc", "Greta", false, EnumSet.of(Role.GUEST)));

    Listing listing = new Listing();
    listing.setHost(host);
    listing.setTitle("Sea view loft");
    listing.setCity("Lisbon");
    listing.setAddress("12 Rua Azul");
    listing.setPricePerNight(new BigDecimal("100.00"));
    listing.setMaxGuests(4);
    listing = listingRepository.save(listing);
    listingId = listing.getId();

    saveBooking(listing, guest, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 15),
        BookingStatus.CONFIRMED);
  }

  private void saveBooking(
      Listing listing, User guest, LocalDate checkIn, LocalDate checkOut, BookingStatus status) {
    Booking booking =
        new Booking(listing, guest, checkIn, checkOut, 2, new BigDecimal("500.00"));
    booking.setStatus(status);
    bookingRepository.save(booking);
  }

  private boolean overlaps(LocalDate checkIn, LocalDate checkOut) {
    return bookingRepository.existsOverlapping(listingId, checkIn, checkOut);
  }

  @Test
  void adjacentBeforeDoesNotOverlap() {
    assertThat(overlaps(LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 10))).isFalse();
  }

  @Test
  void adjacentAfterDoesNotOverlap() {
    assertThat(overlaps(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 20))).isFalse();
  }

  @Test
  void fullyBeforeDoesNotOverlap() {
    assertThat(overlaps(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5))).isFalse();
  }

  @Test
  void fullyAfterDoesNotOverlap() {
    assertThat(overlaps(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 25))).isFalse();
  }

  @Test
  void identicalRangeOverlaps() {
    assertThat(overlaps(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 15))).isTrue();
  }

  @Test
  void containedRangeOverlaps() {
    assertThat(overlaps(LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 13))).isTrue();
  }

  @Test
  void containingRangeOverlaps() {
    assertThat(overlaps(LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 18))).isTrue();
  }

  @Test
  void straddlingStartOverlaps() {
    assertThat(overlaps(LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 12))).isTrue();
  }

  @Test
  void straddlingEndOverlaps() {
    assertThat(overlaps(LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 18))).isTrue();
  }

  @Test
  void cancelledBookingsDoNotCountAsOverlap() {
    User guest = userRepository.findAll().stream()
        .filter(u -> u.getRoles().contains(Role.GUEST)).findFirst().orElseThrow();
    Listing listing = listingRepository.findById(listingId).orElseThrow();
    saveBooking(listing, guest, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        BookingStatus.CANCELLED);

    assertThat(overlaps(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5))).isFalse();
  }
}
