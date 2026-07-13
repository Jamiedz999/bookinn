package com.bookinn.backend.dto;

import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.ListingPhoto;
import com.bookinn.backend.domain.ListingStatus;
import java.math.BigDecimal;

/**
 * Compact listing view for the host's "my listings" list.
 *
 * @param id listing id
 * @param title listing headline
 * @param city city the property is in
 * @param pricePerNight nightly price
 * @param status ACTIVE or INACTIVE
 * @param coverPhotoUrl first photo URL, or {@code null} when the listing has no photos
 */
public record ListingSummaryResponse(
    Long id,
    String title,
    String city,
    BigDecimal pricePerNight,
    ListingStatus status,
    String coverPhotoUrl) {

  /**
   * Projects a {@link Listing} entity to its summary view. Must be called inside an open transaction
   * so the lazy photo collection can be read.
   *
   * @param listing the entity
   * @return the summary DTO
   */
  public static ListingSummaryResponse from(Listing listing) {
    String cover =
        listing.getPhotos().stream().findFirst().map(ListingPhoto::getUrl).orElse(null);
    return new ListingSummaryResponse(
        listing.getId(),
        listing.getTitle(),
        listing.getCity(),
        listing.getPricePerNight(),
        listing.getStatus(),
        cover);
  }
}
