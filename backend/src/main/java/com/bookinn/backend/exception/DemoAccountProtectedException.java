package com.bookinn.backend.exception;

/**
 * Raised when a protected demo account (D7, {@code is_demo}) attempts a mutation that is disabled
 * for the shared sandbox personas — currently changing its own password or email. Served as 403 so
 * the demo cannot be locked away from other interviewers.
 */
public class DemoAccountProtectedException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message the human-readable reason
   */
  public DemoAccountProtectedException(String message) {
    super(message);
  }
}
