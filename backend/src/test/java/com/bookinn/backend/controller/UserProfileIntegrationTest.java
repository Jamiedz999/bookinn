package com.bookinn.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end tests for the self-service profile endpoints: changing password and email for a normal
 * account, and the D7 demo-account protection returning 403.
 */
class UserProfileIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void changePasswordSucceedsAndNewPasswordWorks() throws Exception {
    String email = "change-pw@example.com";
    register(email);
    String token = accessToken(login(email, "password123"));

    mockMvc
        .perform(
            patch("/api/users/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(Map.of("currentPassword", "password123", "newPassword", "brand-new-99"))))
        .andExpect(status().isNoContent());

    // Old password rejected, new one accepted.
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123"))))
        .andExpect(status().isUnauthorized());
    login(email, "brand-new-99");
  }

  @Test
  void changePasswordWithWrongCurrentReturns401() throws Exception {
    String email = "wrong-current@example.com";
    register(email);
    String token = accessToken(login(email, "password123"));

    mockMvc
        .perform(
            patch("/api/users/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", "nope-wrong", "newPassword", "brand-new-99"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changeEmailSucceeds() throws Exception {
    String email = "change-email@example.com";
    register(email);
    String token = accessToken(login(email, "password123"));

    mockMvc
        .perform(
            patch("/api/users/me/email")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("newEmail", "moved@example.com"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("moved@example.com"));
  }

  @Test
  void demoAccountPasswordChangeReturns403() throws Exception {
    String token = accessToken(demoLogin());

    mockMvc
        .perform(
            patch("/api/users/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", "demo1234", "newPassword", "brand-new-99"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.path").value("/api/users/me/password"));
  }

  @Test
  void demoAccountEmailChangeReturns403() throws Exception {
    String token = accessToken(demoLogin());

    mockMvc
        .perform(
            patch("/api/users/me/email")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("newEmail", "hijack@example.com"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  // --- helpers ------------------------------------------------------------

  private void register(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "password123", "name", "Alice"))))
        .andExpect(status().isCreated());
  }

  private MvcResult login(String email, String password) throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))))
        .andExpect(status().isOk())
        .andReturn();
  }

  private MvcResult demoLogin() throws Exception {
    return mockMvc
        .perform(
            post("/api/auth/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "GUEST"))))
        .andExpect(status().isOk())
        .andReturn();
  }

  private String accessToken(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
  }

  private String json(Map<String, ?> body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }
}
