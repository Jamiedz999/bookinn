package com.bookinn.backend.exception;

import com.bookinn.backend.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates exceptions escaping controllers into the uniform {@link ApiError} body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles a duplicate-email registration attempt.
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 409 response
   */
  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleEmailExists(
      EmailAlreadyExistsException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  /**
   * Handles a request for a listing that does not exist or is not visible to the caller.
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 404 response
   */
  @ExceptionHandler(ListingNotFoundException.class)
  public ResponseEntity<ApiError> handleListingNotFound(
      ListingNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  /**
   * Handles bad login credentials or an invalid refresh token.
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 401 response
   */
  @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
  public ResponseEntity<ApiError> handleUnauthorized(
      RuntimeException ex, HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
  }

  /**
   * Handles a method-security or filter authorization denial that reached the controller layer.
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 403 response
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleForbidden(
      AccessDeniedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "Access denied", request);
  }

  /**
   * Handles a mutation attempt against a protected demo account (D7).
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 403 response
   */
  @ExceptionHandler(DemoAccountProtectedException.class)
  public ResponseEntity<ApiError> handleDemoProtected(
      DemoAccountProtectedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }


  /**
   * Handles inconsistent search date parameters.
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 400 response
   */
  @ExceptionHandler(InvalidDateRangeException.class)
  public ResponseEntity<ApiError> handleInvalidDateRange(
          InvalidDateRangeException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  /**
   * Handles a semantically invalid booking request (e.g. guest count over capacity).
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 400 response
   */
  @ExceptionHandler(InvalidBookingRequestException.class)
  public ResponseEntity<ApiError> handleInvalidBookingRequest(
      InvalidBookingRequestException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  /**
   * Handles a booking conflict — an overlapping booking, a cancellation outside the 48h window, or
   * an illegal state-machine transition. All are business conflicts served as 409 (PRD §5).
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 409 response
   */
  @ExceptionHandler({
    BookingConflictException.class,
    CancellationNotAllowedException.class,
    IllegalBookingTransitionException.class
  })
  public ResponseEntity<ApiError> handleBookingConflict(
      RuntimeException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request);
  }


  /**
   * Handles bean-validation failures on request bodies.
   *
   * @param ex the raised exception
   * @param request the failing request
   * @return a 400 response listing the invalid fields
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, message, request);
  }

  private ResponseEntity<ApiError> build(
      HttpStatus status, String message, HttpServletRequest request) {
    ApiError body =
        new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }
}
