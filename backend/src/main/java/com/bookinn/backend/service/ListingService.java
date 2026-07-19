package com.bookinn.backend.service;

import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.ListingPhoto;
import com.bookinn.backend.domain.ListingStatus;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.CreateListingRequest;
import com.bookinn.backend.dto.ListingResponse;
import com.bookinn.backend.dto.ListingStatusRequest;
import com.bookinn.backend.dto.ListingSummaryResponse;
import com.bookinn.backend.dto.UpdateListingRequest;
import com.bookinn.backend.exception.InvalidCredentialsException;
import com.bookinn.backend.exception.ListingNotFoundException;
import com.bookinn.backend.repository.AmenityRepository;
import com.bookinn.backend.repository.ListingRepository;
import com.bookinn.backend.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listing lifecycle for hosts: create, edit, activate/deactivate, plus public detail and the host's
 * own list. Ownership of a listing is enforced here via {@link #loadOwned} as the service-layer
 * half of the {@code @PreAuthorize} + service "double check" (PRD §6, M2).
 */
@Service
public class ListingService {

  private final ListingRepository listingRepository;
  private final AmenityRepository amenityRepository;
  private final UserRepository userRepository;

  /**
   * Creates the service.
   *
   * @param listingRepository listing store
   * @param amenityRepository amenity dictionary store, for resolving {@code amenityIds}
   * @param userRepository user store, for attaching the owning host
   */
  public ListingService(
      ListingRepository listingRepository,
      AmenityRepository amenityRepository,
      UserRepository userRepository) {
    this.listingRepository = listingRepository;
    this.amenityRepository = amenityRepository;
    this.userRepository = userRepository;
  }

  /**
   * Creates a listing owned by the given host. New listings start {@code ACTIVE}. Resolves {@code
   * amenityIds} against the dictionary and materialises {@code photoUrls} into ordered photos
   * (index becomes {@code sortOrder}).
   *
   * @param hostId id of the authenticated host, taken from the principal
   * @param request the create payload
   * @return the created listing's detail view
   */
  @Transactional
  public ListingResponse create(Long hostId, CreateListingRequest request) {
    User host =
        userRepository
            .findById(hostId)
            .orElseThrow(
                () -> new InvalidCredentialsException("Authenticated user no longer exists"));

    Listing listing = new Listing();
    listing.setHost(host);
    listing.setTitle(request.title());
    listing.setDescription(request.description());
    listing.setCity(request.city());
    listing.setAddress(request.address());
    listing.setPricePerNight(request.pricePerNight());
    listing.setMaxGuests(request.maxGuests());
    // status is left at the entity default of ACTIVE.

    applyAmenities(listing, request.amenityIds());
    applyPhotos(listing, request.photoUrls());

    return ListingResponse.from(listingRepository.save(listing));
  }

  /**
   * Replaces a listing's amenities with the dictionary entries for the given ids. A {@code null} or
   * empty set clears them. Reusable by {@link #update}.
   *
   * @param listing the listing to mutate
   * @param amenityIds amenity dictionary ids, may be {@code null}
   */
  private void applyAmenities(Listing listing, Set<Long> amenityIds) {
    if (amenityIds == null || amenityIds.isEmpty()) {
      listing.getAmenities().clear();
      return;
    }
    listing.setAmenities(new LinkedHashSet<>(amenityRepository.findAllById(amenityIds)));
  }

  /**
   * Rebuilds a listing's photos from the given URLs, using list position as {@code sortOrder}. A
   * {@code null} list clears them. Reusable by {@link #update}.
   *
   * @param listing the listing to mutate
   * @param photoUrls image URLs in display order, may be {@code null}
   */
  private void applyPhotos(Listing listing, List<String> photoUrls) {
    listing.clearPhotos();
    if (photoUrls == null) {
      return;
    }
    for (int i = 0; i < photoUrls.size(); i++) {
      listing.addPhoto(new ListingPhoto(photoUrls.get(i), i));
    }
  }

  /**
   * Replaces the editable fields of a listing the host owns. Amenities and photos are full
   * replacements. Enforces ownership via {@link #loadOwned}.
   *
   * @param hostId id of the authenticated host
   * @param listingId id of the listing to edit
   * @param request the update payload
   * @return the updated listing's detail view
   */
  @Transactional
  public ListingResponse update(Long hostId, Long listingId, UpdateListingRequest request) {
    Listing listing = loadOwned(hostId, listingId);
    listing.setTitle(request.title());
    listing.setDescription(request.description());
    listing.setCity(request.city());
    listing.setAddress(request.address());
    listing.setPricePerNight(request.pricePerNight());
    listing.setMaxGuests(request.maxGuests());
    applyAmenities(listing, request.amenityIds());
    applyPhotos(listing, request.photoUrls());
    return ListingResponse.from(listing);
  }

  /**
   * Activates or deactivates a listing the host owns. Enforces ownership via {@link #loadOwned}.
   *
   * @param hostId id of the authenticated host
   * @param listingId id of the listing to change
   * @param request the target status
   * @return the listing's detail view after the change
   */
  @Transactional
  public ListingResponse changeStatus(
      Long hostId, Long listingId, ListingStatusRequest request) {
    Listing listing = loadOwned(hostId, listingId);
    listing.setStatus(request.status());
    return ListingResponse.from(listing);
  }

  /**
   * Returns a listing for the public detail page. INACTIVE listings must be hidden: throw {@link
   * com.bookinn.backend.exception.ListingNotFoundException} rather than reveal them.
   *
   * @param listingId id of the listing
   * @return the listing's detail view
   */
  @Transactional(readOnly = true)
  public ListingResponse getPublicDetail(Long listingId) {
    Listing listing =
        listingRepository
            .findById(listingId)
            .filter(candidate -> candidate.getStatus() == ListingStatus.ACTIVE)
            .orElseThrow(() -> new ListingNotFoundException("Listing not found: " + listingId));
    return ListingResponse.from(listing);
  }

  /**
   * Returns one of the host's own listings in full detail, regardless of status, for the edit form.
   * Enforces ownership via {@link #loadOwned}, so this is not a way to read another host's listing
   * (nor a public bypass of the INACTIVE-hiding rule on {@link #getPublicDetail}).
   *
   * @param hostId id of the authenticated host
   * @param listingId id of the listing to load
   * @return the owned listing's detail view
   */
  @Transactional(readOnly = true)
  public ListingResponse getOwnedDetail(Long hostId, Long listingId) {
    return ListingResponse.from(loadOwned(hostId, listingId));
  }

  /**
   * Lists the host's own properties, including INACTIVE ones, newest first.
   *
   * @param hostId id of the authenticated host
   * @return the host's listings as summaries
   */
  @Transactional(readOnly = true)
  public List<ListingSummaryResponse> getHostListings(Long hostId) {
    return listingRepository.findByHostIdOrderByCreatedAtDesc(hostId).stream()
        .map(ListingSummaryResponse::from)
        .toList();
  }

  /**
   * Loads a listing and asserts the given host owns it — the service-layer half of the ownership
   * "double check" that backs the 403 acceptance criterion (the other half is {@code @PreAuthorize}
   * on the controller). A missing listing must be indistinguishable from one owned by someone else:
   * throw {@link org.springframework.security.access.AccessDeniedException} in both cases so a host
   * cannot probe which listing ids exist.
   *
   * @param hostId id of the authenticated host
   * @param listingId id of the listing to load
   * @return the owned listing
   */
  private Listing loadOwned(Long hostId, Long listingId) {
    Listing listing =
        listingRepository
            .findById(listingId)
            .orElseThrow(() -> new AccessDeniedException("Not the owner of the requested listing"));
    if (!listing.getHost().getId().equals(hostId)) {
      throw new AccessDeniedException("Not the owner of the requested listing");
    }
    return listing;
  }
}
