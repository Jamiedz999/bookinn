package com.bookinn.backend.exception;

/**
 * Raised when a booking cannot be created because it overlaps an existing CONFIRMED booking for the
 * same listing. Mapped to HTTP 409 (PRD §5: business conflicts use 409).
 */
public class BookingConflictException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message human-readable conflict detail
   */
  public BookingConflictException(String message) {
    super(message);
  }
}
