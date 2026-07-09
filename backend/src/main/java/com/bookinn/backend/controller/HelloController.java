package com.bookinn.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Scaffold smoke-test endpoint. */
@RestController
public class HelloController {

  /**
   * Confirms the backend is reachable.
   *
   * @return a static greeting
   */
  @GetMapping("/api/hello")
  public String hello() {
    return "BookInn backend is alive";
  }
}
