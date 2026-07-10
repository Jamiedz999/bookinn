package com.bookinn.backend.exception;

/** Thrown when a login is attempted with an unknown email or wrong password. Maps to 401. */
public class InvalidCredentialsException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message detail message
   */
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
