package com.bookinn.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookinn.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards that the OpenAPI docs are generated and public. Regression test for the springdoc/Spring
 * Framework 7 incompatibility where introspecting {@code @RestControllerAdvice} threw and the error
 * dispatch surfaced as a 401 on {@code /v3/api-docs}.
 */
class OpenApiDocsIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void openApiDocsAreGeneratedAndPublic() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
  }
}
