package com.bookinn.backend.exception;

/** Thrown when registration is attempted with an email that is already taken. Maps to 409. */
public class EmailAlreadyExistsException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message detail message
   */
  public EmailAlreadyExistsException(String message) {
    super(message);
  }
}
