package com.bookinn.backend.exception;

/**
 * Raised when a booking request is semantically invalid beyond what bean validation catches — e.g.
 * the guest count exceeds the listing's capacity. Mapped to HTTP 400.
 */
public class InvalidBookingRequestException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message human-readable reason
   */
  public InvalidBookingRequestException(String message) {
    super(message);
  }
}
