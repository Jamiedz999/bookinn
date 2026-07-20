package com.bookinn.backend.service;

import com.bookinn.backend.dto.AmenityResponse;
import com.bookinn.backend.repository.AmenityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read access to the public amenity dictionary. */
@Service
public class AmenityService {

  private final AmenityRepository amenityRepository;

  /**
   * Creates the service.
   *
   * @param amenityRepository amenity store
   */
  public AmenityService(AmenityRepository amenityRepository) {
    this.amenityRepository = amenityRepository;
  }

  /**
   * Lists the whole amenity dictionary, alphabetically.
   *
   * @return the amenities
   */
  @Transactional(readOnly = true)
  public List<AmenityResponse> listAll() {
    return amenityRepository.findAllByOrderByNameAsc().stream().map(AmenityResponse::from).toList();
  }
}
