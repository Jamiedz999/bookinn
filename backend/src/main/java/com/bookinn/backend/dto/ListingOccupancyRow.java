package com.bookinn.backend.dto;

/**
 * Spring Data projection for one row of the occupancy query: a listing and the number of its booked
 * nights that fall inside the current month. Listings with no bookings this month still appear (via
 * the query's {@code LEFT JOIN}) with zero nights. The service divides by the month length to get
 * the rate.
 */
public interface ListingOccupancyRow {

  /**
   * The listing's id.
   *
   * @return the listing id
   */
  Long getListingId();

  /**
   * The listing's title, used as the bar label.
   *
   * @return the listing title
   */
  String getListingTitle();

  /**
   * Booked nights that fall inside the current month, clipped at both month boundaries.
   *
   * @return the in-month booked nights
   */
  long getBookedNights();
}
