package com.bookinn.backend.service;

import com.bookinn.backend.domain.Amenity;
import com.bookinn.backend.domain.Booking;
import com.bookinn.backend.domain.BookingStatus;
import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.ListingPhoto;
import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.repository.AmenityRepository;
import com.bookinn.backend.repository.BookingRepository;
import com.bookinn.backend.repository.ListingRepository;
import com.bookinn.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the full demo dataset (D7, M6) so the public sandbox looks alive: two protected demo
 * personas, a handful of hosts, ~30 listings with stock photos and amenities, and enough booking
 * history to fill every dashboard widget and make search date-filtering observable.
 *
 * <p>The dataset is deterministic (fixed {@link Random} seed) and dated relative to an injected
 * {@link Clock}, so it looks the same shape every night after {@link DemoResetService} truncates
 * and reseeds. The demo <em>host</em> in particular owns listings with a completed booking in each
 * of the last 12 months plus current and future bookings, so signing in as the demo host shows a
 * fully populated dashboard.
 *
 * <p>This service assumes it is writing into empty business tables. Use {@link #seedIfEmpty()} for
 * idempotent startup seeding; {@link DemoResetService} truncates first and then reseeds.
 */
@Service
public class DemoSeedService {

  private static final Logger log = LoggerFactory.getLogger(DemoSeedService.class);

  private static final long RANDOM_SEED = 42L;
  private static final int HOST_COUNT = 8;
  private static final int LISTINGS_PER_HOST = 3;
  private static final int DEMO_HOST_LISTINGS = 4;
  private static final int EXTRA_GUEST_COUNT = 3;
  private static final int HISTORY_MONTHS = 12;

  private static final String[] CITIES = {
    "Lisbon", "Porto", "Sintra", "Madrid", "Barcelona",
    "Paris", "Rome", "Amsterdam", "Berlin", "London"
  };
  private static final String[] TITLE_PREFIXES = {
    "Sea view", "Garden", "City centre", "Cosy", "Sunlit", "Riverside", "Historic", "Modern"
  };
  private static final String[] TITLE_TYPES = {"loft", "studio", "cabin", "apartment", "townhouse"};

  private final UserRepository userRepository;
  private final ListingRepository listingRepository;
  private final BookingRepository bookingRepository;
  private final AmenityRepository amenityRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param userRepository user store
   * @param listingRepository listing store
   * @param bookingRepository booking store
   * @param amenityRepository amenity dictionary (reference data, not seeded here)
   * @param passwordEncoder encoder for the seeded accounts' passwords
   * @param clock time source, so seeded dates are relative to "today"
   */
  public DemoSeedService(
      UserRepository userRepository,
      ListingRepository listingRepository,
      BookingRepository bookingRepository,
      AmenityRepository amenityRepository,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.userRepository = userRepository;
    this.listingRepository = listingRepository;
    this.bookingRepository = bookingRepository;
    this.amenityRepository = amenityRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  /**
   * Seeds the demo dataset only if it is not already present, keyed off the demo host account. Safe
   * to call on every startup.
   */
  @Transactional
  public void seedIfEmpty() {
    if (userRepository.findByEmail(DemoAccounts.HOST_EMAIL).isPresent()) {
      log.debug("Demo data already present; skipping seed");
      return;
    }
    seed();
  }

  /**
   * Builds the full demo dataset. Assumes the business tables are empty (e.g. just truncated by
   * {@link DemoResetService}). Not idempotent on its own — use {@link #seedIfEmpty()} for that.
   */
  @Transactional
  public void seed() {
    Random random = new Random(RANDOM_SEED);
    List<Amenity> amenities = amenityRepository.findAllByOrderByNameAsc();
    LocalDate today = LocalDate.now(clock);

    String password = passwordEncoder.encode(DemoAccounts.PASSWORD);
    User demoHost =
        userRepository.save(
            new User(
                DemoAccounts.HOST_EMAIL, password, "Demo Host", true,
                EnumSet.of(Role.GUEST, Role.HOST)));
    User demoGuest =
        userRepository.save(
            new User(
                DemoAccounts.GUEST_EMAIL, password, "Demo Guest", true, EnumSet.of(Role.GUEST)));

    // Guests that bookings are attributed to: the demo guest plus a few extras for variety.
    List<User> guests = new ArrayList<>();
    guests.add(demoGuest);
    for (int i = 1; i <= EXTRA_GUEST_COUNT; i++) {
      guests.add(
          userRepository.save(
              new User(
                  "guest" + i + "@bookinn.app", password, "Guest " + i, false,
                  EnumSet.of(Role.GUEST))));
    }

    // The demo host's own listings — kept fully booked across time so the demo dashboard is rich.
    List<Listing> demoListings =
        createListings(demoHost, DEMO_HOST_LISTINGS, amenities, random);
    for (Listing listing : demoListings) {
      seedFullTimeline(listing, guests, today, random);
    }

    // A spread of other hosts, each with a few listings and some scattered bookings for realism.
    for (int h = 1; h <= HOST_COUNT; h++) {
      User host =
          userRepository.save(
              new User(
                  "host" + h + "@bookinn.app", password, "Host " + h, false,
                  EnumSet.of(Role.GUEST, Role.HOST)));
      List<Listing> listings = createListings(host, LISTINGS_PER_HOST, amenities, random);
      for (Listing listing : listings) {
        seedScatteredBookings(listing, guests, today, random);
      }
    }

    log.info(
        "Seeded demo data: {} users, {} listings, {} bookings",
        userRepository.count(),
        listingRepository.count(),
        bookingRepository.count());
  }

  /** Creates {@code count} listings for a host with photos and a rotating amenity subset. */
  private List<Listing> createListings(
      User host, int count, List<Amenity> amenities, Random random) {
    List<Listing> created = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Listing listing = new Listing();
      listing.setHost(host);
      String city = CITIES[random.nextInt(CITIES.length)];
      listing.setTitle(
          TITLE_PREFIXES[random.nextInt(TITLE_PREFIXES.length)]
              + " "
              + TITLE_TYPES[random.nextInt(TITLE_TYPES.length)]
              + " in "
              + city);
      listing.setDescription("A comfortable place to stay in " + city + ". Seeded demo listing.");
      listing.setCity(city);
      listing.setAddress((random.nextInt(200) + 1) + " Demo Street");
      listing.setPricePerNight(new BigDecimal(60 + random.nextInt(19) * 10)); // 60..240
      listing.setMaxGuests(2 + random.nextInt(5)); // 2..6

      long photoSeed = random.nextInt(1000);
      for (int p = 0; p < 3; p++) {
        listing.addPhoto(
            new ListingPhoto("https://picsum.photos/seed/" + (photoSeed + p) + "/800/600", p));
      }
      if (!amenities.isEmpty()) {
        int amenityCount = 3 + random.nextInt(4); // 3..6 amenities
        for (int a = 0; a < amenityCount; a++) {
          listing.getAmenities().add(amenities.get(random.nextInt(amenities.size())));
        }
      }
      created.add(listingRepository.save(listing));
    }
    return created;
  }

  /**
   * Gives one listing a completed booking in each of the last {@value #HISTORY_MONTHS} months, a
   * current-month confirmed booking, and an upcoming one — the shape the host dashboard draws from.
   */
  private void seedFullTimeline(
      Listing listing, List<User> guests, LocalDate today, Random random) {
    LocalDate monthStart = today.withDayOfMonth(1);
    for (int monthsAgo = HISTORY_MONTHS; monthsAgo >= 1; monthsAgo--) {
      LocalDate checkIn = monthStart.minusMonths(monthsAgo).plusDays(4 + random.nextInt(10));
      LocalDate checkOut = checkIn.plusDays(2 + random.nextInt(4));
      saveBooking(listing, pick(guests, random), checkIn, checkOut, BookingStatus.COMPLETED);
    }
    // Current month: a confirmed stay that started earlier this month.
    LocalDate currentIn = monthStart.plusDays(random.nextInt(3));
    saveBooking(listing, pick(guests, random), currentIn, currentIn.plusDays(3),
        BookingStatus.CONFIRMED);
    // Upcoming within the next week or two: drives the "upcoming check-ins" KPI and search filters.
    LocalDate upcomingIn = today.plusDays(2 + random.nextInt(12));
    saveBooking(listing, pick(guests, random), upcomingIn, upcomingIn.plusDays(3),
        BookingStatus.CONFIRMED);
  }

  /** Scatters a few completed and future bookings on a listing for realistic search results. */
  private void seedScatteredBookings(
      Listing listing, List<User> guests, LocalDate today, Random random) {
    int past = 2 + random.nextInt(3); // 2..4 completed stays
    for (int i = 0; i < past; i++) {
      LocalDate checkIn =
          today.minusMonths(1 + random.nextInt(HISTORY_MONTHS)).plusDays(random.nextInt(20));
      LocalDate checkOut = checkIn.plusDays(2 + random.nextInt(4));
      saveBooking(listing, pick(guests, random), checkIn, checkOut, BookingStatus.COMPLETED);
    }
    if (random.nextBoolean()) {
      LocalDate upcomingIn = today.plusDays(3 + random.nextInt(40));
      LocalDate upcomingOut = upcomingIn.plusDays(2 + random.nextInt(3));
      saveBooking(listing, pick(guests, random), upcomingIn, upcomingOut, BookingStatus.CONFIRMED);
    }
  }

  private void saveBooking(
      Listing listing, User guest, LocalDate checkIn, LocalDate checkOut, BookingStatus status) {
    long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
    BigDecimal total = listing.getPricePerNight().multiply(BigDecimal.valueOf(nights));
    Booking booking = new Booking(listing, guest, checkIn, checkOut, 2, total);
    booking.setStatus(status);
    bookingRepository.save(booking);
  }

  private static User pick(List<User> guests, Random random) {
    return guests.get(random.nextInt(guests.size()));
  }
}
