package com.bookinn.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** End-to-end tests for listing management: ownership 403, auth 401, amenity/photo round-trip. */
class ListingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  // --- helpers ------------------------------------------------------------

  /** Registers a GUEST, upgrades to HOST, and returns a fresh access token carrying ROLE_HOST. */
  private String hostToken(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123", "name", "Host"))))
        .andExpect(status().isCreated());
    String guestToken = accessToken(login(email));
    mockMvc
        .perform(post("/api/users/me/become-host").header("Authorization", "Bearer " + guestToken))
        .andExpect(status().isOk());
    // Roles are embedded in the JWT, so a fresh login is needed to pick up the new HOST role.
    return accessToken(login(email));
  }

  private String guestToken(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123", "name", "Guest"))))
        .andExpect(status().isCreated());
    return accessToken(login(email));
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

  private List<Long> firstTwoAmenityIds() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/amenities")).andExpect(status().isOk()).andReturn();
    JsonNode array = objectMapper.readTree(result.getResponse().getContentAsString());
    return List.of(array.get(0).get("id").asLong(), array.get(1).get("id").asLong());
  }

  private String createBody(List<Long> amenityIds, List<String> photoUrls) throws Exception {
    return json(
        Map.of(
            "title", "Sea view loft",
            "description", "Bright and airy",
            "city", "Lisbon",
            "address", "12 Rua Azul",
            "pricePerNight", 120.00,
            "maxGuests", 4,
            "amenityIds", amenityIds,
            "photoUrls", photoUrls));
  }

  private long createListing(String hostToken, List<Long> amenityIds, List<String> photoUrls)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/listings")
                    .header("Authorization", "Bearer " + hostToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody(amenityIds, photoUrls)))
            .andExpect(status().isCreated())
            .andReturn();
    return tree(result).get("id").asLong();
  }

  private JsonNode tree(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String json(Map<String, ?> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  // --- tests --------------------------------------------------------------

  @Test
  void hostCreatesListingAndAmenitiesAndPhotosRoundTrip() throws Exception {
    String token = hostToken("roundtrip-host@example.com");
    List<Long> amenityIds = firstTwoAmenityIds();

    mockMvc
        .perform(
            post("/api/listings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(amenityIds, List.of("cover.jpg", "room.jpg"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.amenities.length()").value(2))
        .andExpect(jsonPath("$.photoUrls[0]").value("cover.jpg"))
        .andExpect(jsonPath("$.photoUrls[1]").value("room.jpg"));
  }

  @Test
  void publicDetailReturnsActiveListingWithItsAmenitiesAndPhotos() throws Exception {
    String token = hostToken("public-detail-host@example.com");
    long id = createListing(token, firstTwoAmenityIds(), List.of("cover.jpg"));

    mockMvc
        .perform(get("/api/listings/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.amenities.length()").value(2))
        .andExpect(jsonPath("$.photoUrls[0]").value("cover.jpg"));
  }

  @Test
  void creatingListingRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(List.of(), List.of())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void guestCannotCreateListing() throws Exception {
    String token = guestToken("guest-create@example.com");

    mockMvc
        .perform(
            post("/api/listings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(List.of(), List.of())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void hostCannotModifyAnotherHostsListing() throws Exception {
    String hostA = hostToken("owner-a@example.com");
    String hostB = hostToken("intruder-b@example.com");
    long listingId = createListing(hostA, List.of(), List.of());

    mockMvc
        .perform(
            put("/api/listings/" + listingId)
                .header("Authorization", "Bearer " + hostB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(List.of(), List.of())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void deactivatedListingIsHiddenFromPublicDetail() throws Exception {
    String token = hostToken("deactivate-host@example.com");
    long listingId = createListing(token, List.of(), List.of());

    mockMvc.perform(get("/api/listings/" + listingId)).andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/listings/" + listingId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "INACTIVE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));

    mockMvc.perform(get("/api/listings/" + listingId)).andExpect(status().isNotFound());
  }

  @Test
  void hostListingsIncludesOwnInactiveListings() throws Exception {
    String token = hostToken("host-listings@example.com");
    long listingId = createListing(token, List.of(), List.of());
    mockMvc
        .perform(
            patch("/api/listings/" + listingId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", "INACTIVE"))))
        .andExpect(status().isOk());

    MvcResult result =
        mockMvc
            .perform(get("/api/host/listings").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(listingId))
            .andExpect(jsonPath("$[0].status").value("INACTIVE"))
            .andReturn();

    assertThat(tree(result).size()).isEqualTo(1);
  }
}
