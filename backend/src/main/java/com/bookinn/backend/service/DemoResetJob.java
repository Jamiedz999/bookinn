package com.bookinn.backend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly job that resets the demo sandbox (M6). Only registered when
 * {@code bookinn.demo.enabled=true} so it can never wipe a non-demo database (or a test context).
 * The schedule is a cron expression, overridable via {@code bookinn.demo.reset-cron}; it defaults
 * to 04:00, after the 03:00 booking completion sweep. The real work lives in
 * {@link DemoResetService} so it stays testable independently of the scheduler.
 */
@Component
@ConditionalOnProperty(name = "bookinn.demo.enabled", havingValue = "true")
public class DemoResetJob {

  private final DemoResetService demoResetService;

  /**
   * Creates the job.
   *
   * @param demoResetService the service carrying the reset logic
   */
  public DemoResetJob(DemoResetService demoResetService) {
    this.demoResetService = demoResetService;
  }

  /** Runs the nightly truncate-and-reseed. */
  @Scheduled(cron = "${bookinn.demo.reset-cron:0 0 4 * * *}")
  public void run() {
    demoResetService.reset();
  }
}
