package com.bookinn.backend.exception;

/**
 * Raised when a state-machine transition is attempted from a terminal state (COMPLETED or
 * CANCELLED). This is the "illegal transition" defence from PRD §4; mapped to HTTP 409.
 */
public class IllegalBookingTransitionException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message human-readable reason
   */
  public IllegalBookingTransitionException(String message) {
    super(message);
  }
}
