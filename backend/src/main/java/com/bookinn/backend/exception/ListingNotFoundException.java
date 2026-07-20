package com.bookinn.backend.exception;

/**
 * Thrown when a listing does not exist, or is not visible to the caller (e.g. an INACTIVE listing
 * requested through the public detail endpoint). Maps to 404 so existence is not leaked.
 */
public class ListingNotFoundException extends RuntimeException {

  /**
   * Creates the exception.
   *
   * @param message detail message
   */
  public ListingNotFoundException(String message) {
    super(message);
  }
}
