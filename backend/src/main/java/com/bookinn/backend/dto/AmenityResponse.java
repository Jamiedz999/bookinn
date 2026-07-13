package com.bookinn.backend.dto;

import com.bookinn.backend.domain.Amenity;

/**
 * A single amenity in the public dictionary.
 *
 * @param id amenity id
 * @param name display name
 */
public record AmenityResponse(Long id, String name) {

  /**
   * Projects an {@link Amenity} entity to its response view.
   *
   * @param amenity the entity
   * @return the response DTO
   */
  public static AmenityResponse from(Amenity amenity) {
    return new AmenityResponse(amenity.getId(), amenity.getName());
  }
}
