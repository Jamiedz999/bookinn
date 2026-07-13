package com.bookinn.backend.dto;

import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.ListingPhoto;
import com.bookinn.backend.domain.ListingStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Full detail view of a listing, including its amenities and ordered photo URLs.
 *
 * @param id listing id
 * @param hostId owning host's id
 * @param title listing headline
 * @param description free-text details
 * @param city city the property is in
 * @param address street address
 * @param pricePerNight nightly price
 * @param maxGuests maximum occupancy
 * @param status ACTIVE or INACTIVE
 * @param amenities the listing's amenities
 * @param photoUrls photo URLs in display order
 */
public record ListingResponse(
    Long id,
    Long hostId,
    String title,
    String description,
    String city,
    String address,
    BigDecimal pricePerNight,
    int maxGuests,
    ListingStatus status,
    List<AmenityResponse> amenities,
    List<String> photoUrls) {

  /**
   * Projects a {@link Listing} entity to its detail view. Must be called inside an open transaction
   * so the lazy amenity and photo collections can be read.
   *
   * @param listing the entity
   * @return the response DTO
   */
  public static ListingResponse from(Listing listing) {
    return new ListingResponse(
        listing.getId(),
        listing.getHost().getId(),
        listing.getTitle(),
        listing.getDescription(),
        listing.getCity(),
        listing.getAddress(),
        listing.getPricePerNight(),
        listing.getMaxGuests(),
        listing.getStatus(),
        listing.getAmenities().stream().map(AmenityResponse::from).toList(),
        listing.getPhotos().stream().map(ListingPhoto::getUrl).toList());
  }
}
