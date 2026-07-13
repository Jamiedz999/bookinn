package com.bookinn.backend.controller;

import com.bookinn.backend.dto.AmenityResponse;
import com.bookinn.backend.service.AmenityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public amenity dictionary endpoint. */
@RestController
@RequestMapping("/api/amenities")
public class AmenityController {

  private final AmenityService amenityService;

  /**
   * Creates the controller.
   *
   * @param amenityService amenity dictionary access
   */
  public AmenityController(AmenityService amenityService) {
    this.amenityService = amenityService;
  }

  /**
   * Lists the whole amenity dictionary.
   *
   * @return the amenities
   */
  @GetMapping
  public List<AmenityResponse> list() {
    return amenityService.listAll();
  }
}
