package com.bookinn.backend.config;

import com.bookinn.backend.service.DemoSeedService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the public demo sandbox (D7, M6), activated by {@code bookinn.demo.enabled=true} (set
 * in the deployed environment; off by default so local runs and tests are untouched). On startup it
 * seeds the demo dataset if the database is empty; the nightly reset lives in
 * {@link com.bookinn.backend.service.DemoResetJob}, gated by the same flag.
 *
 * <p>This is distinct from {@link DevSeedConfig}, a tiny opt-in seed for local dashboard work.
 */
@Configuration
@ConditionalOnProperty(name = "bookinn.demo.enabled", havingValue = "true")
public class DemoDataConfig {

  /**
   * Seeds the full demo dataset on startup if it is not already present.
   *
   * @param demoSeedService the demo seed
   * @return the runner
   */
  @Bean
  public CommandLineRunner demoDataSeeder(DemoSeedService demoSeedService) {
    return args -> demoSeedService.seedIfEmpty();
  }
}
