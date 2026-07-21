package com.bookinn.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.service.BookingService;
import com.bookinn.backend.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end tests for the booking core: create (201), the overlap guard (409), the 48h/ownership
 * cancellation rules (409/403), auth (401), guest-count validation (400), the quote endpoint, and —
 * as the PRD's optional stretch — a two-thread race asserting exactly one of two concurrent bookings
 * for the same dates succeeds.
 */
class BookingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private BookingService bookingService;

  private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

  // --- auth + fixture helpers ---------------------------------------------

  private String hostToken(String email) throws Exception {
    register(email, "Host");
    String guestToken = accessToken(login(email));
    mockMvc
        .perform(post("/api/users/me/become-host").header("Authorization", "Bearer " + guestToken))
        .andExpect(status().isOk());
    return accessToken(login(email));
  }

  private String guestToken(String email) throws Exception {
    register(email, "Guest");
    return accessToken(login(email));
  }

  private void register(String email, String name) throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123", "name", name))))
        .andExpect(status().isCreated());
  }

  private MvcResult login(String email) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123"))))
        .andExpect(status().isOk())
        .andReturn();
  }

  private String accessToken(MvcResult result) throws Exception {
    return tree(result).get("accessToken").asString();
  }

  private long createListing(String hostToken) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/listings")
                    .header("Authorization", "Bearer " + hostToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json(
                            Map.of(
                                "title", "Sea view loft",
                                "description", "Bright and airy",
                                "city", "Lisbon",
                                "address", "12 Rua Azul",
                                "pricePerNight", 100.00,
                                "maxGuests", 4,
                                "amenityIds", List.of(),
                                "photoUrls", List.of("cover.jpg")))))
            .andExpect(status().isCreated())
            .andReturn();
    return tree(result).get("id").asLong();
  }

  private String bookingBody(long listingId, LocalDate checkIn, LocalDate checkOut, int guestCount)
      throws Exception {
    return json(
        Map.of(
            "listingId", listingId,
            "checkIn", checkIn.toString(),
            "checkOut", checkOut.toString(),
            "guestCount", guestCount));
  }

  private JsonNode tree(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String json(Map<String, ?> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  // --- create + quote -----------------------------------------------------

  @Test
  void guestCreatesBookingWithComputedPrice() throws Exception {
    long listing = createListing(hostToken("book-create-host@example.com"));
    String guest = guestToken("book-create-guest@example.com");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + guest)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, TODAY.plusDays(30), TODAY.plusDays(33), 2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.nights").value(3))
        .andExpect(jsonPath("$.totalPrice").value(300.00))
        .andExpect(jsonPath("$.cancellable").value(true));
  }

  @Test
  void quoteIsPublicAndPricesTheStay() throws Exception {
    long listing = createListing(hostToken("book-quote-host@example.com"));

    mockMvc
        .perform(
            get("/api/listings/" + listing + "/quote")
                .param("checkIn", TODAY.plusDays(10).toString())
                .param("checkOut", TODAY.plusDays(12).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nights").value(2))
        .andExpect(jsonPath("$.totalPrice").value(200.00));
  }

  @Test
  void overlappingBookingIsRejectedWith409() throws Exception {
    long listing = createListing(hostToken("book-overlap-host@example.com"));
    String first = guestToken("book-overlap-a@example.com");
    String second = guestToken("book-overlap-b@example.com");
    LocalDate checkIn = TODAY.plusDays(40);
    LocalDate checkOut = TODAY.plusDays(45);

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + first)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, checkIn, checkOut, 2)))
        .andExpect(status().isCreated());

    // A contained range on the same listing overlaps the CONFIRMED booking.
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + second)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, checkIn.plusDays(1), checkOut.minusDays(1), 2)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void adjacentBookingOnTheCheckoutDayIsAllowed() throws Exception {
    long listing = createListing(hostToken("book-adjacent-host@example.com"));
    String guest = guestToken("book-adjacent-guest@example.com");
    LocalDate checkIn = TODAY.plusDays(50);
    LocalDate checkOut = TODAY.plusDays(53);

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + guest)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, checkIn, checkOut, 2)))
        .andExpect(status().isCreated());

    // Starts exactly on the previous checkout day: half-open, so no overlap.
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + guest)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, checkOut, checkOut.plusDays(2), 2)))
        .andExpect(status().isCreated());
  }

  @Test
  void guestCountOverCapacityIsRejectedWith400() throws Exception {
    long listing = createListing(hostToken("book-capacity-host@example.com"));
    String guest = guestToken("book-capacity-guest@example.com");

    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + guest)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, TODAY.plusDays(30), TODAY.plusDays(32), 5)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void creatingBookingRequiresAuthentication() throws Exception {
    long listing = createListing(hostToken("book-anon-host@example.com"));

    mockMvc
        .perform(
            post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, TODAY.plusDays(30), TODAY.plusDays(32), 2)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  // --- cancel -------------------------------------------------------------

  @Test
  void ownerCancelsBookingOutsideThe48hWindow() throws Exception {
    long listing = createListing(hostToken("book-cancel-host@example.com"));
    String guest = guestToken("book-cancel-guest@example.com");
    long bookingId = createBooking(guest, listing, TODAY.plusDays(30), TODAY.plusDays(33));

    mockMvc
        .perform(post("/api/bookings/" + bookingId + "/cancel")
            .header("Authorization", "Bearer " + guest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    // Cancelling again hits the terminal-state defence.
    mockMvc
        .perform(post("/api/bookings/" + bookingId + "/cancel")
            .header("Authorization", "Bearer " + guest))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void cancellingWithin48hIsRejectedWith409() throws Exception {
    long listing = createListing(hostToken("book-late-host@example.com"));
    String guest = guestToken("book-late-guest@example.com");
    // Check-in today: "now" is already inside the 48h window.
    long bookingId = createBooking(guest, listing, TODAY, TODAY.plusDays(2));

    mockMvc
        .perform(post("/api/bookings/" + bookingId + "/cancel")
            .header("Authorization", "Bearer " + guest))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void nonOwnerCannotCancelWith403() throws Exception {
    long listing = createListing(hostToken("book-403-host@example.com"));
    String owner = guestToken("book-403-owner@example.com");
    String other = guestToken("book-403-other@example.com");
    long bookingId = createBooking(owner, listing, TODAY.plusDays(30), TODAY.plusDays(33));

    mockMvc
        .perform(post("/api/bookings/" + bookingId + "/cancel")
            .header("Authorization", "Bearer " + other))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  // --- my / host listings -------------------------------------------------

  @Test
  void guestSeesOwnBookingsAndHostSeesReceived() throws Exception {
    String host = hostToken("book-lists-host@example.com");
    long listing = createListing(host);
    String guest = guestToken("book-lists-guest@example.com");
    createBooking(guest, listing, TODAY.plusDays(30), TODAY.plusDays(33));

    mockMvc
        .perform(get("/api/bookings/my").header("Authorization", "Bearer " + guest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].listingId").value(listing));

    mockMvc
        .perform(get("/api/host/bookings").header("Authorization", "Bearer " + host))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].listingId").value(listing));
  }

  // --- completion sweep ---------------------------------------------------

  @Test
  void completionSweepMovesPastConfirmedBookingsToCompleted() throws Exception {
    long listing = createListing(hostToken("book-sweep-host@example.com"));
    long guestId =
        jdbc.queryForObject(
            "SELECT id FROM users WHERE email = ?", Long.class, "book-sweep-host@example.com");
    // A CONFIRMED stay whose checkout has passed, inserted directly (the API forbids past dates).
    jdbc.update(
        "INSERT INTO booking (listing_id, guest_id, check_in, check_out, guest_count, "
            + "total_price, status) VALUES (?, ?, ?, ?, ?, ?, 'CONFIRMED')",
        listing,
        guestId,
        Date.valueOf(TODAY.minusDays(5)),
        Date.valueOf(TODAY.minusDays(2)),
        2,
        new BigDecimal("300.00"));
    Long bookingId =
        jdbc.queryForObject(
            "SELECT id FROM booking WHERE listing_id = ? AND status = 'CONFIRMED'",
            Long.class,
            listing);

    // Run the real service method the daily job delegates to.
    int completed = bookingService.completePastBookings();

    assertThat(completed).isGreaterThanOrEqualTo(1);
    String status =
        jdbc.queryForObject("SELECT status FROM booking WHERE id = ?", String.class, bookingId);
    assertThat(status).isEqualTo("COMPLETED");
  }

  // --- concurrency (PRD stretch) ------------------------------------------

  @Test
  void twoConcurrentBookingsForSameDatesLeaveExactlyOneWinner() throws Exception {
    long listing = createListing(hostToken("book-race-host@example.com"));
    String a = guestToken("book-race-a@example.com");
    String b = guestToken("book-race-b@example.com");
    LocalDate checkIn = TODAY.plusDays(60);
    LocalDate checkOut = TODAY.plusDays(63);

    // Both threads block on the latch, then fire as simultaneously as the JVM allows. The
    // pessimistic lock on the listing row must serialise them so only one CONFIRMED booking lands.
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first = pool.submit(() -> book(listing, checkIn, checkOut, a, start));
      Future<Integer> second = pool.submit(() -> book(listing, checkIn, checkOut, b, start));
      start.countDown();
      List<Integer> statuses = List.of(first.get(), second.get());

      assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
      assertThat(statuses).filteredOn(code -> code == 409).hasSize(1);
    } finally {
      pool.shutdownNow();
    }
  }

  private int book(
      long listing, LocalDate checkIn, LocalDate checkOut, String token, CountDownLatch start)
      throws Exception {
    start.await();
    return mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listing, checkIn, checkOut, 2)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  private long createBooking(String token, long listing, LocalDate checkIn, LocalDate checkOut)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/bookings")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bookingBody(listing, checkIn, checkOut, 2)))
            .andExpect(status().isCreated())
            .andReturn();
    return tree(result).get("id").asLong();
  }
}
