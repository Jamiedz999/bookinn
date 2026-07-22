package com.bookinn.backend.config;

import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.domain.BookingStatus;
import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.repository.BookingRepository;
import com.bookinn.backend.repository.ListingRepository;
import com.bookinn.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A tiny, opt-in seed so the host dashboard has something to draw during local development. It is
 * <strong>not</strong> the real demo seed — that arrives in M6 (issue #7) with the nightly reset —
 * so it is disabled by default and only runs when {@code bookinn.dev-seed.enabled=true} (e.g.
 * {@code ./mvnw spring-boot:run -Dspring-boot.run.arguments=--bookinn.dev-seed.enabled=true}). It
 * creates one host, one guest, three listings, and a spread of bookings across the last several
 * months plus a couple of upcoming ones, so every dashboard widget shows a plausible shape. It is
 * idempotent: if the seed host already exists it does nothing.
 */
@Configuration
@ConditionalOnProperty(name = "bookinn.dev-seed.enabled", havingValue = "true")
public class DevSeedConfig {

  private static final String HOST_EMAIL = "demo-host@bookinn.local";
  private static final String GUEST_EMAIL = "demo-guest@bookinn.local";

  /**
   * Registers the seeding runner.
   *
   * @param users user store
   * @param listings listing store
   * @param bookings booking store
   * @param passwordEncoder encoder for the seeded accounts' password
   * @param clock time source, so seeded dates are relative to "today"
   * @return the runner
   */
  @Bean
  public CommandLineRunner dashboardDevSeed(
      UserRepository users,
      ListingRepository listings,
      BookingRepository bookings,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    return args -> {
      if (users.findByEmail(HOST_EMAIL).isPresent()) {
        return;
      }
      String password = passwordEncoder.encode("password123");
      // Not demo-flagged: the protected "demo account" semantics (is_demo) belong to M6/D7.
      User host =
          users.save(new User(HOST_EMAIL, password, "Demo Host", false, EnumSet.of(Role.HOST)));
      User guest =
          users.save(new User(GUEST_EMAIL, password, "Demo Guest", false, EnumSet.of(Role.GUEST)));

      Listing loft = listings.save(listing(host, "Sea view loft", "Lisbon", "120.00", 4));
      Listing cabin = listings.save(listing(host, "Garden cabin", "Sintra", "90.00", 2));
      Listing studio = listings.save(listing(host, "City studio", "Porto", "70.00", 2));

      LocalDate today = LocalDate.now(clock);
      LocalDate monthStart = today.withDayOfMonth(1);

      // Past months (completed) → give the revenue-trend line some shape.
      for (int monthsAgo = 6; monthsAgo >= 1; monthsAgo--) {
        LocalDate start = monthStart.minusMonths(monthsAgo).plusDays(5);
        bookings.save(
            booking(loft, guest, start, start.plusDays(3), "360.00", BookingStatus.COMPLETED));
        if (monthsAgo % 2 == 0) {
          bookings.save(
              booking(cabin, guest, start.plusDays(2), start.plusDays(6), "360.00",
                  BookingStatus.COMPLETED));
        }
      }

      // This month → drives current-month revenue and occupancy.
      bookings.save(
          booking(loft, guest, monthStart.plusDays(2), monthStart.plusDays(6), "480.00",
              BookingStatus.CONFIRMED));
      bookings.save(
          booking(studio, guest, monthStart.plusDays(10), monthStart.plusDays(13), "210.00",
              BookingStatus.CONFIRMED));

      // Upcoming within 7 days → drives the "upcoming check-ins" KPI.
      bookings.save(
          booking(cabin, guest, today.plusDays(3), today.plusDays(5), "180.00",
              BookingStatus.CONFIRMED));
    };
  }

  private static Listing listing(
      User host, String title, String city, String pricePerNight, int maxGuests) {
    Listing listing = new Listing();
    listing.setHost(host);
    listing.setTitle(title);
    listing.setDescription("Seeded for local dashboard development.");
    listing.setCity(city);
    listing.setAddress("1 Demo Street");
    listing.setPricePerNight(new BigDecimal(pricePerNight));
    listing.setMaxGuests(maxGuests);
    return listing;
  }

  private static Booking booking(
      Listing listing,
      User guest,
      LocalDate checkIn,
      LocalDate checkOut,
      String totalPrice,
      BookingStatus status) {
    Booking booking =
        new Booking(listing, guest, checkIn, checkOut, 2, new BigDecimal(totalPrice));
    booking.setStatus(status);
    return booking;
  }
}
