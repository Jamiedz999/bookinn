package com.bookinn.backend.domain;

/** Whether a listing is publicly visible and bookable. Persisted by name in {@code listing.status}. */
public enum ListingStatus {
  ACTIVE,
  INACTIVE
}
