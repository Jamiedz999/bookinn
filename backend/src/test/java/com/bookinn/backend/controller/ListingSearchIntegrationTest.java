package com.bookinn.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end tests for the public search endpoint {@code GET /api/listings}: it is reachable without
 * authentication, filters by city prefix, hides INACTIVE listings, and — the core of M3 — excludes
 * listings whose requested dates overlap a CONFIRMED booking.
 *
 * <p>Listings and bookings are seeded directly through {@link JdbcTemplate}. Booking rows are
 * inserted with raw SQL because the Booking entity and its write path only arrive in M4; the search
 * query already reads the {@code booking} table via native SQL. Each test uses a distinct city so
 * its listings are isolated from other tests sharing the same container.
 */
class ListingSearchIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;

  // --- seeding helpers ----------------------------------------------------

  private long insertHost(String email) {
    jdbc.update(
        "INSERT INTO users (email, password_hash, name) VALUES (?, ?, ?)",
        email,
        "x".repeat(60),
        "Host");
    return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
  }

  private long insertListing(long hostId, String title, String city, String status) {
    jdbc.update(
        "INSERT INTO listing (host_id, title, description, city, address, price_per_night, "
            + "max_guests, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        hostId,
        title,
        "desc",
        city,
        "1 St",
        new BigDecimal("100.00"),
        4,
        status);
    return jdbc.queryForObject("SELECT id FROM listing WHERE title = ?", Long.class, title);
  }

  private void insertBooking(
      long listingId, long guestId, LocalDate checkIn, LocalDate checkOut, String status) {
    jdbc.update(
        "INSERT INTO booking (listing_id, guest_id, check_in, check_out, guest_count, "
            + "total_price, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
        listingId,
        guestId,
        Date.valueOf(checkIn),
        Date.valueOf(checkOut),
        2,
        new BigDecimal("200.00"),
        status);
  }

  /** Runs a search and returns the ids in its {@code content}, in response order. */
  private List<Long> searchIds(String query) throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/listings" + query)).andExpect(status().isOk()).andReturn();
    JsonNode content =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
    List<Long> ids = new ArrayList<>();
    content.forEach(node -> ids.add(node.get("id").asLong()));
    return ids;
  }

  // --- tests --------------------------------------------------------------

  @Test
  void searchIsPublicAndReturnsPagedActiveListings() throws Exception {
    long host = insertHost("search-basic@example.com");
    long id = insertListing(host, "Basic active loft", "Reykjavik", "ACTIVE");

    // No Authorization header: the endpoint must be public and paged.
    mockMvc
        .perform(get("/api/listings?city=Reykjavik"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(12))
        .andExpect(jsonPath("$.content[0].id").value(id));
  }

  @Test
  void resultsAreSplitAcrossPagesOfTwelve() throws Exception {
    long host = insertHost("search-paging@example.com");
    for (int i = 0; i < 13; i++) {
      insertListing(host, "Trondheim loft " + i, "Trondheim", "ACTIVE");
    }

    // Page 0: full page of 12, with totals reflecting all 13 matches across 2 pages.
    mockMvc
        .perform(get("/api/listings?city=Trondheim&page=0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.content.length()").value(12))
        .andExpect(jsonPath("$.totalElements").value(13))
        .andExpect(jsonPath("$.totalPages").value(2));

    // Page 1: the remaining single listing.
    mockMvc
        .perform(get("/api/listings?city=Trondheim&page=1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  void cityFilterMatchesCaseInsensitivePrefixOnly() throws Exception {
    long host = insertHost("search-city@example.com");
    long id = insertListing(host, "City prefix loft", "Barcelona", "ACTIVE");

    assertThat(searchIds("?city=bar")).contains(id); // prefix, case-insensitive
    assertThat(searchIds("?city=celona")).doesNotContain(id); // not a prefix
  }

  @Test
  void inactiveListingsAreExcludedFromSearch() throws Exception {
    long host = insertHost("search-inactive@example.com");
    long active = insertListing(host, "Oslo visible loft", "Oslo", "ACTIVE");
    long inactive = insertListing(host, "Oslo hidden loft", "Oslo", "INACTIVE");

    List<Long> ids = searchIds("?city=Oslo");

    assertThat(ids).contains(active).doesNotContain(inactive);
  }

  @Test
  void overlappingConfirmedBookingHidesListingButAdjacentDatesDoNot() throws Exception {
    long host = insertHost("search-overlap@example.com");
    long id = insertListing(host, "Helsinki overlap loft", "Helsinki", "ACTIVE");
    insertBooking(id, host, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 15), "CONFIRMED");

    // Contained within [10,15) -> overlaps -> hidden.
    assertThat(searchIds("?city=Helsinki&checkIn=2026-09-12&checkOut=2026-09-14"))
        .doesNotContain(id);
    // Starts exactly on the checkout day (half-open interval) -> no overlap -> shown.
    assertThat(searchIds("?city=Helsinki&checkIn=2026-09-15&checkOut=2026-09-17")).contains(id);
    // Ends exactly on the check-in day -> no overlap -> shown.
    assertThat(searchIds("?city=Helsinki&checkIn=2026-09-05&checkOut=2026-09-10")).contains(id);
  }

  @Test
  void partiallyOverlappingBookingHidesListing() throws Exception {
    long host = insertHost("search-partial@example.com");
    long id = insertListing(host, "Aarhus partial loft", "Aarhus", "ACTIVE");
    insertBooking(id, host, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 15), "CONFIRMED");

    // [14,20) straddles the tail of [10,15) -> overlaps -> hidden.
    assertThat(searchIds("?city=Aarhus&checkIn=2026-09-14&checkOut=2026-09-20")).doesNotContain(id);
  }

  @Test
  void cancelledBookingDoesNotBlockAvailability() throws Exception {
    long host = insertHost("search-cancelled@example.com");
    long id = insertListing(host, "Bergen cancelled loft", "Bergen", "ACTIVE");
    insertBooking(id, host, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 15), "CANCELLED");

    // Same dates as the cancelled booking, but only CONFIRMED bookings occupy -> shown.
    assertThat(searchIds("?city=Bergen&checkIn=2026-09-11&checkOut=2026-09-13")).contains(id);
  }

  @Test
  void inconsistentDateRangeIsRejectedWith400() throws Exception {
    mockMvc
        .perform(get("/api/listings?checkIn=2026-09-15&checkOut=2026-09-10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    mockMvc
        .perform(get("/api/listings?checkIn=2026-09-15"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
