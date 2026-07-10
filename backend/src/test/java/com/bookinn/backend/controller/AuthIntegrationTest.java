package com.bookinn.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.support.AbstractIntegrationTest;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends AbstractIntegrationTest {

  private static final String REFRESH_COOKIE = "refresh_token";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private void register(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123", "name", "Alice"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.roles[0]").value("GUEST"));
  }

  private MvcResult login(String email, String password) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))))
        .andReturn();
  }

  private String accessTokenOf(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private String json(Map<String, ?> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }

  @Test
  void registerThenLoginIssuesAccessTokenAndRefreshCookie() throws Exception {
    register("login-flow@example.com");

    MvcResult result = login("login-flow@example.com", "password123");

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    Cookie refresh = result.getResponse().getCookie(REFRESH_COOKIE);
    assertThat(refresh).isNotNull();
    assertThat(refresh.isHttpOnly()).isTrue();
    assertThat(refresh.getValue()).isNotBlank();
    assertThat(accessTokenOf(result)).isNotBlank();
  }

  @Test
  void loginWithWrongPasswordReturns401WithUniformBody() throws Exception {
    register("wrong-pw@example.com");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "wrong-pw@example.com", "password", "nope"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/auth/login"));
  }

  @Test
  void registerRejectsDuplicateEmailWith409() throws Exception {
    register("dupe@example.com");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        Map.of("email", "dupe@example.com", "password", "password123", "name", "A"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void meRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void meReturnsProfileWhenAuthenticated() throws Exception {
    register("me@example.com");
    String token = accessTokenOf(login("me@example.com", "password123"));

    mockMvc
        .perform(get("/api/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("me@example.com"));
  }

  @Test
  void refreshRotatesTokenAndRevokesTheOldOne() throws Exception {
    register("rotate@example.com");
    Cookie oldRefresh = login("rotate@example.com", "password123").getResponse().getCookie(REFRESH_COOKIE);

    MvcResult refreshed =
        mockMvc.perform(post("/api/auth/refresh").cookie(oldRefresh)).andExpect(status().isOk()).andReturn();
    Cookie newRefresh = refreshed.getResponse().getCookie(REFRESH_COOKIE);
    assertThat(newRefresh).isNotNull();
    assertThat(newRefresh.getValue()).isNotEqualTo(oldRefresh.getValue());

    mockMvc.perform(post("/api/auth/refresh").cookie(oldRefresh)).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
    register("logout@example.com");
    Cookie refresh = login("logout@example.com", "password123").getResponse().getCookie(REFRESH_COOKIE);

    MvcResult logout =
        mockMvc.perform(post("/api/auth/logout").cookie(refresh)).andExpect(status().isNoContent()).andReturn();
    assertThat(logout.getResponse().getCookie(REFRESH_COOKIE).getMaxAge()).isZero();

    mockMvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
  }

  @Test
  void hostOnlyEndpointIsForbiddenForGuestAndAllowedForHost() throws Exception {
    String guestToken = accessTokenOf(demoLogin("GUEST"));
    String hostToken = accessTokenOf(demoLogin("HOST"));

    mockMvc
        .perform(get("/api/test/host-only").header("Authorization", "Bearer " + guestToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));

    mockMvc
        .perform(get("/api/test/host-only").header("Authorization", "Bearer " + hostToken))
        .andExpect(status().isOk());
  }

  @Test
  void demoLoginLandsInLoggedInSession() throws Exception {
    MvcResult result = demoLogin("GUEST");
    assertThat(result.getResponse().getCookie(REFRESH_COOKIE)).isNotNull();
    String token = accessTokenOf(result);

    mockMvc
        .perform(get("/api/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.demo").value(true));
  }

  private MvcResult demoLogin(String role) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", role))))
        .andExpect(status().isOk())
        .andReturn();
  }
}
