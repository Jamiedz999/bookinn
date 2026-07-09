package com.bookinn.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Application entry point. */
@SpringBootApplication
public class BackendApplication {

  /**
   * Boots the Spring context.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(BackendApplication.class, args);
  }

}
