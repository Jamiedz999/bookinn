package com.bookinn.backend.controller;

import com.bookinn.backend.dto.CreateListingRequest;
import com.bookinn.backend.dto.ListingResponse;
import com.bookinn.backend.dto.ListingStatusRequest;
import com.bookinn.backend.dto.ListingSummaryResponse;
import com.bookinn.backend.dto.UpdateListingRequest;
import com.bookinn.backend.security.AuthenticatedUser;
import com.bookinn.backend.service.ListingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listing endpoints. Public detail is open; create/edit/status require the HOST role and, via the
 * service, ownership of the target listing.
 */
@RestController
public class ListingController {

  private final ListingService listingService;

  /**
   * Creates the controller.
   *
   * @param listingService listing lifecycle operations
   */
  public ListingController(ListingService listingService) {
    this.listingService = listingService;
  }

  /**
   * Creates a listing owned by the authenticated host.
   *
   * @param principal the authenticated host
   * @param request the create payload
   * @return 201 with the created listing
   */
  @PostMapping("/api/listings")
  @PreAuthorize("hasRole('HOST')")
  public ResponseEntity<ListingResponse> create(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody CreateListingRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(listingService.create(principal.id(), request));
  }

  /**
   * Replaces the editable fields of a listing the host owns.
   *
   * @param principal the authenticated host
   * @param id id of the listing to edit
   * @param request the update payload
   * @return 200 with the updated listing
   */
  @PutMapping("/api/listings/{id}")
  @PreAuthorize("hasRole('HOST')")
  public ListingResponse update(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable Long id,
      @Valid @RequestBody UpdateListingRequest request) {
    return listingService.update(principal.id(), id, request);
  }

  /**
   * Activates or deactivates a listing the host owns.
   *
   * @param principal the authenticated host
   * @param id id of the listing to change
   * @param request the target status
   * @return 200 with the listing after the change
   */
  @PatchMapping("/api/listings/{id}/status")
  @PreAuthorize("hasRole('HOST')")
  public ListingResponse changeStatus(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @PathVariable Long id,
      @Valid @RequestBody ListingStatusRequest request) {
    return listingService.changeStatus(principal.id(), id, request);
  }

  /**
   * Returns a listing for the public detail page. INACTIVE listings are hidden (404).
   *
   * @param id id of the listing
   * @return 200 with the listing detail
   */
  @GetMapping("/api/listings/{id}")
  public ListingResponse detail(@PathVariable Long id) {
    return listingService.getPublicDetail(id);
  }

  /**
   * Lists the authenticated host's own properties, including INACTIVE ones.
   *
   * @param principal the authenticated host
   * @return the host's listings as summaries
   */
  @GetMapping("/api/host/listings")
  @PreAuthorize("hasRole('HOST')")
  public List<ListingSummaryResponse> hostListings(
      @AuthenticationPrincipal AuthenticatedUser principal) {
    return listingService.getHostListings(principal.id());
  }
}
