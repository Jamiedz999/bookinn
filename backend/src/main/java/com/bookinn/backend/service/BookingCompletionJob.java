package com.bookinn.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily job that completes bookings whose stay has ended. The schedule is a cron expression,
 * overridable via {@code bookinn.booking.completion-cron}; it defaults to 03:00 so it runs before
 * the nightly seed reset (M6). The real transition logic lives in {@link BookingService} so it
 * stays unit-testable independently of the scheduler.
 */
@Component
public class BookingCompletionJob {

  private static final Logger log = LoggerFactory.getLogger(BookingCompletionJob.class);

  private final BookingService bookingService;

  /**
   * Creates the job.
   *
   * @param bookingService the service carrying the completion logic
   */
  public BookingCompletionJob(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  /** Runs the completion sweep and logs how many bookings were completed. */
  @Scheduled(cron = "${bookinn.booking.completion-cron:0 0 3 * * *}")
  public void run() {
    int completed = bookingService.completePastBookings();
    if (completed > 0) {
      log.info("Completed {} past booking(s)", completed);
    }
  }
}
