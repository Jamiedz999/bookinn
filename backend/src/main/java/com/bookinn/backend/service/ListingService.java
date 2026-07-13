package com.bookinn.backend.service;

import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.dto.CreateListingRequest;
import com.bookinn.backend.dto.ListingResponse;
import com.bookinn.backend.dto.ListingStatusRequest;
import com.bookinn.backend.dto.ListingSummaryResponse;
import com.bookinn.backend.dto.UpdateListingRequest;
import com.bookinn.backend.repository.AmenityRepository;
import com.bookinn.backend.repository.ListingRepository;
import com.bookinn.backend.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listing lifecycle for hosts: create, edit, activate/deactivate, plus public detail and the host's
 * own list.
 *
 * <p><strong>Scaffold status (M2):</strong> the class, its dependencies, and every method seam are
 * wired, but the business bodies are intentionally left as {@code TODO}s per the agreed split — the
 * ownership rule, amenity/photo round-trip mapping, and status/visibility rules are yours to
 * implement (with their unit tests). Each method's Javadoc states the exact rule from PRD §4/§6.
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
   * amenityIds} against the dictionary and materialises {@code photoUrls} into ordered photos (index
   * becomes {@code sortOrder}).
   *
   * @param hostId id of the authenticated host, taken from the principal
   * @param request the create payload
   * @return the created listing's detail view
   */
  @Transactional
  public ListingResponse create(Long hostId, CreateListingRequest request) {
    // TODO(M2): load host via userRepository; map fields; resolve amenities; add ordered photos;
    // save; return ListingResponse.from(saved).
    throw new UnsupportedOperationException("TODO(M2): ListingService.create");
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
    // TODO(M2): loadOwned(hostId, listingId); overwrite scalar fields; replace amenity set;
    // clearPhotos() then re-add ordered photos; return ListingResponse.from(listing).
    throw new UnsupportedOperationException("TODO(M2): ListingService.update");
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
    // TODO(M2): loadOwned(hostId, listingId); set status; return ListingResponse.from(listing).
    throw new UnsupportedOperationException("TODO(M2): ListingService.changeStatus");
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
    // TODO(M2): find by id; if missing or status != ACTIVE throw ListingNotFoundException;
    // else return ListingResponse.from(listing).
    throw new UnsupportedOperationException("TODO(M2): ListingService.getPublicDetail");
  }

  /**
   * Lists the host's own properties, including INACTIVE ones, newest first.
   *
   * @param hostId id of the authenticated host
   * @return the host's listings as summaries
   */
  @Transactional(readOnly = true)
  public List<ListingSummaryResponse> getHostListings(Long hostId) {
    // TODO(M2): listingRepository.findByHostIdOrderByCreatedAtDesc(hostId)
    // .stream().map(ListingSummaryResponse::from).toList().
    throw new UnsupportedOperationException("TODO(M2): ListingService.getHostListings");
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
    // TODO(M2): find by id; if absent or host id mismatch throw AccessDeniedException; else return.
    throw new UnsupportedOperationException("TODO(M2): ListingService.loadOwned");
  }
}
