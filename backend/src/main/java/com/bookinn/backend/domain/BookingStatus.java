package com.bookinn.backend.domain;

/**
 * Lifecycle state of a booking. {@code CONFIRMED} is the only non-terminal state (PRD decision D3,
 * "Instant-Book" — no host approval / PENDING). {@code COMPLETED} and {@code CANCELLED} are
 * terminal: any further transition is rejected by {@link Booking}. Persisted by name in
 * {@code status}.
 */
public enum BookingStatus {
  CONFIRMED,
  COMPLETED,
  CANCELLED
}
