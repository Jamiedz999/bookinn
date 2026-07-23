package com.bookinn.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards the health probe the container healthcheck depends on: {@code /actuator/health} must be
 * public (no token) and report UP with no leaked internals. If it required auth the healthcheck would
 * fail and the backend would never be marked healthy in compose.
 */
class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void healthEndpointIsPublicAndReportsUpWithoutDetails() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        // show-details: never — no component breakdown (db, diskSpace, ...) on the wire.
        .andExpect(jsonPath("$.components").doesNotExist());
  }
}
