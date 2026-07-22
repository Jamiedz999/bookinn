package com.bookinn.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end tests for the host dashboard against real MySQL, with the clock pinned to 15 Jul 2026
 * so "current month" is July (31 days) and the trend window is Aug 2025 → Jul 2026. A fixed fixture
 * of bookings on one host lets every KPI, trend point, and occupancy rate be asserted to an exact
 * number (PRD test discipline). The fixture deliberately includes a cancelled booking, a
 * month-straddling stay, a booking just outside the 12-month window, a listing with no bookings, and
 * a second host — so exclusion, clipping, zero-fill, and per-host scoping are all covered.
 */
@Import(DashboardIntegrationTest.FixedClockConfig.class)
class DashboardIntegrationTest extends AbstractIntegrationTest {

  /** Pins the app clock so the dashboard's "current month" is deterministic under test. */
  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;

  private String hostToken;
  private String guestToken;

  @BeforeEach
  void setUp() throws Exception {
    jdbc.update("DELETE FROM booking");
    jdbc.update("DELETE FROM listing_amenity");
    jdbc.update("DELETE FROM listing_photo");
    jdbc.update("DELETE FROM listing");
    jdbc.update("DELETE FROM refresh_token");
    jdbc.update("DELETE FROM user_role");
    jdbc.update("DELETE FROM users");

    hostToken = hostToken("host1@example.com");
    long l1 = createListing(hostToken, "Sea view loft");
    long l2 = createListing(hostToken, "Garden cabin");
    createListing(hostToken, "Empty studio"); // L3: no bookings this month

    guestToken = guestToken("guest@example.com");
    long guestId = userId("guest@example.com");

    // Current month (July 2026): 580 revenue, 2 upcoming check-ins.
    insertBooking(l1, guestId, "2026-07-05", "2026-07-08", "300.00", "CONFIRMED"); // past check-in
    insertBooking(l1, guestId, "2026-07-20", "2026-07-22", "200.00", "CONFIRMED"); // upcoming
    insertBooking(l2, guestId, "2026-07-18", "2026-07-19", "80.00", "CONFIRMED"); // upcoming
    // Straddles the June/July boundary: only 2 nights (Jul 1–3) fall in July; revenue is June's.
    insertBooking(l1, guestId, "2026-06-29", "2026-07-03", "400.00", "CONFIRMED");
    // Cancelled: excluded from every figure.
    insertBooking(l2, guestId, "2026-07-10", "2026-07-12", "999.00", "CANCELLED");
    // Earlier months, inside the 12-month window.
    insertBooking(l1, guestId, "2026-05-10", "2026-05-15", "500.00", "COMPLETED");
    insertBooking(l2, guestId, "2025-09-01", "2025-09-03", "250.00", "COMPLETED");
    // Just outside the trend window (Jul 2025): counts toward all-time total, not the trend.
    insertBooking(l1, guestId, "2025-07-01", "2025-07-02", "111.00", "COMPLETED");

    // A second host's booking must never leak into host1's dashboard.
    String otherHostToken = hostToken("host2@example.com");
    long otherListing = createListing(otherHostToken, "Someone else's place");
    insertBooking(otherListing, guestId, "2026-07-06", "2026-07-09", "5000.00", "CONFIRMED");
  }

  @Test
  void summaryReportsCurrentMonthRevenueTotalBookingsAndUpcomingCheckIns() throws Exception {
    MvcResult result =
        mockMvc
            .perform(authorized(get("/api/host/dashboard/summary"), hostToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalBookings").value(7))
            .andExpect(jsonPath("$.upcomingCheckIns").value(2))
            .andReturn();

    assertThat(tree(result).get("currentMonthRevenue").asDouble()).isEqualTo(580.0);
  }

  @Test
  void revenueTrendHasTwelveMonthsZeroFilledAndExcludesOutOfWindowBookings() throws Exception {
    MvcResult result =
        mockMvc
            .perform(authorized(get("/api/host/dashboard/revenue-trend"), hostToken))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode trend = tree(result);
    assertThat(trend).hasSize(12);
    assertThat(trend.get(0).get("month").asString()).isEqualTo("2025-08");
    assertThat(trend.get(11).get("month").asString()).isEqualTo("2026-07");
    assertThat(revenueForMonth(trend, "2025-08")).isEqualTo(0.0);
    assertThat(revenueForMonth(trend, "2025-09")).isEqualTo(250.0);
    assertThat(revenueForMonth(trend, "2026-05")).isEqualTo(500.0);
    assertThat(revenueForMonth(trend, "2026-06")).isEqualTo(400.0);
    assertThat(revenueForMonth(trend, "2026-07")).isEqualTo(580.0);

    // Sum proves the Jul 2025 booking (111) was excluded from the window.
    double sum = 0;
    for (JsonNode point : trend) {
      sum += point.get("revenue").asDouble();
    }
    assertThat(sum).isEqualTo(1730.0);
  }

  @Test
  void occupancyClipsMonthBoundariesAndIncludesListingsWithNoBookings() throws Exception {
    mockMvc
        .perform(authorized(get("/api/host/dashboard/occupancy"), hostToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].listingTitle").value("Sea view loft"))
        .andExpect(jsonPath("$[0].bookedNights").value(7)) // 3 + 2 + 2 (clipped)
        .andExpect(jsonPath("$[0].daysInMonth").value(31))
        .andExpect(jsonPath("$[1].listingTitle").value("Garden cabin"))
        .andExpect(jsonPath("$[1].bookedNights").value(1))
        .andExpect(jsonPath("$[2].listingTitle").value("Empty studio"))
        .andExpect(jsonPath("$[2].bookedNights").value(0));
  }

  @Test
  void guestIsForbidden() throws Exception {
    mockMvc
        .perform(authorized(get("/api/host/dashboard/summary"), guestToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousIsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/host/dashboard/summary")).andExpect(status().isUnauthorized());
  }

  // --- helpers -------------------------------------------------------------

  private static double revenueForMonth(JsonNode trend, String month) {
    for (JsonNode point : trend) {
      if (point.get("month").asString().equals(month)) {
        return point.get("revenue").asDouble();
      }
    }
    throw new AssertionError("month not present: " + month);
  }

  private void insertBooking(
      long listingId, long guestId, String checkIn, String checkOut, String price, String status) {
    jdbc.update(
        "INSERT INTO booking "
            + "(listing_id, guest_id, check_in, check_out, guest_count, total_price, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
        listingId,
        guestId,
        checkIn,
        checkOut,
        2,
        new BigDecimal(price),
        status);
  }

  private MockHttpServletRequestBuilder authorized(
      MockHttpServletRequestBuilder builder, String token) {
    return builder.header("Authorization", "Bearer " + token);
  }

  private String hostToken(String email) throws Exception {
    register(email, "Host");
    String token = accessToken(login(email));
    mockMvc
        .perform(post("/api/users/me/become-host").header("Authorization", "Bearer " + token))
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

  private long userId(String email) {
    Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    return id == null ? 0L : id;
  }

  private long createListing(String hostToken, String title) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/listings")
                    .header("Authorization", "Bearer " + hostToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json(
                            Map.of(
                                "title", title,
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

  private String accessToken(MvcResult result) throws Exception {
    return tree(result).get("accessToken").asString();
  }

  private JsonNode tree(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String json(Map<String, ?> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }
}
