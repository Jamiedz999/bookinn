package com.bookinn.backend.exception;

/** Thrown when a refresh token is missing, unknown, revoked, or expired. Maps to 401. */
public class InvalidRefreshTokenException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message detail message
   */
  public InvalidRefreshTokenException(String message) {
    super(message);
  }
}
