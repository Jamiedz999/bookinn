package com.bookinn.backend.exception;

/**
 * Raised when search date parameters are inconsistent: only one of {@code checkIn}/{@code checkOut}
 * supplied, or {@code checkIn} is not strictly before {@code checkOut}. Maps to 400.
 */
public class InvalidDateRangeException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message the human-readable reason
   */
  public InvalidDateRangeException(String message) {
    super(message);
  }
}
