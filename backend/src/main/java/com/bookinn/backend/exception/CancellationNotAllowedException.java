package com.bookinn.backend.exception;

/**
 * Raised when a cancellation is refused by policy — the free-cancellation window has closed, i.e.
 * the request arrived later than 48h before check-in. Mapped to HTTP 409 (PRD §5).
 */
public class CancellationNotAllowedException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message human-readable reason
   */
  public CancellationNotAllowedException(String message) {
    super(message);
  }
}
