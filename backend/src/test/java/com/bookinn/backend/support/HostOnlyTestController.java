package com.bookinn.backend.support;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint used to assert method-level authorization. M1 ships no HOST-restricted business
 * endpoints yet, so this stands in to prove {@code @PreAuthorize} + the 403 handler are wired.
 */
@RestController
@RequestMapping("/api/test")
public class HostOnlyTestController {

  @GetMapping("/host-only")
  @PreAuthorize("hasRole('HOST')")
  public String hostOnly() {
    return "host-only ok";
  }
}
