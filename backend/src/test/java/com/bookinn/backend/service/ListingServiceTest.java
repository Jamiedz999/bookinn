package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bookinn.backend.domain.Amenity;
import com.bookinn.backend.domain.Listing;
import com.bookinn.backend.domain.ListingStatus;
import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.AmenityResponse;
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
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/** Unit tests for {@link ListingService}: field mapping, amenity/photo round-trip, ownership. */
@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

  private static final long HOST_ID = 7L;
  private static final long OTHER_HOST_ID = 999L;

  @Mock private ListingRepository listingRepository;
  @Mock private AmenityRepository amenityRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private ListingService service;

  // --- fixtures -----------------------------------------------------------

  private User user(long id) {
    User user = new User("host@example.com", "enc", "Holly Host", false, EnumSet.of(Role.HOST));
    user.setId(id);
    return user;
  }

  private Amenity amenity(long id, String name) {
    Amenity amenity = new Amenity();
    amenity.setId(id);
    amenity.setName(name);
    return amenity;
  }

  private CreateListingRequest createRequest(Set<Long> amenityIds, List<String> photoUrls) {
    return new CreateListingRequest(
        "Sea view loft",
        "Bright and airy",
        "Lisbon",
        "12 Rua Azul",
        new BigDecimal("120.00"),
        4,
        amenityIds,
        photoUrls);
  }

  private UpdateListingRequest updateRequest() {
    return new UpdateListingRequest(
        "Sea view loft",
        "Bright and airy",
        "Lisbon",
        "12 Rua Azul",
        new BigDecimal("120.00"),
        4,
        Set.of(),
        List.of());
  }

  /** An existing, persisted listing owned by {@code ownerId}. */
  private Listing existingListing(long id, long ownerId, ListingStatus status) {
    Listing listing = new Listing();
    listing.setId(id);
    listing.setHost(user(ownerId));
    listing.setTitle("Old title");
    listing.setDescription("old");
    listing.setCity("Porto");
    listing.setAddress("1 Old St");
    listing.setPricePerNight(new BigDecimal("80.00"));
    listing.setMaxGuests(2);
    listing.setStatus(status);
    return listing;
  }

  /** Stubs the host lookup and makes save() return its argument so the mapping flows through. */
  private void stubHostAndSave() {
    when(userRepository.findById(HOST_ID)).thenReturn(Optional.of(user(HOST_ID)));
    when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  // --- create -------------------------------------------------------------

  @Test
  void createMapsScalarFieldsAndDefaultsToActive() {
    stubHostAndSave();

    ListingResponse response = service.create(HOST_ID, createRequest(Set.of(), List.of()));

    assertThat(response.hostId()).isEqualTo(HOST_ID);
    assertThat(response.title()).isEqualTo("Sea view loft");
    assertThat(response.city()).isEqualTo("Lisbon");
    assertThat(response.address()).isEqualTo("12 Rua Azul");
    assertThat(response.pricePerNight()).isEqualByComparingTo("120.00");
    assertThat(response.maxGuests()).isEqualTo(4);
    assertThat(response.status()).isEqualTo(ListingStatus.ACTIVE);
  }

  @Test
  void createRoundTripsAmenities() {
    stubHostAndSave();
    when(amenityRepository.findAllById(Set.of(1L, 2L)))
        .thenReturn(List.of(amenity(1L, "Wifi"), amenity(2L, "Kitchen")));

    ListingResponse response = service.create(HOST_ID, createRequest(Set.of(1L, 2L), List.of()));

    assertThat(response.amenities())
        .extracting(AmenityResponse::name)
        .containsExactlyInAnyOrder("Wifi", "Kitchen");
  }

  @Test
  void createMaterialisesPhotosInGivenOrder() {
    stubHostAndSave();

    ListingResponse response =
        service.create(HOST_ID, createRequest(Set.of(), List.of("a.jpg", "b.jpg", "c.jpg")));

    assertThat(response.photoUrls()).containsExactly("a.jpg", "b.jpg", "c.jpg");
  }

  @Test
  void createThrowsWhenHostMissing() {
    when(userRepository.findById(HOST_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(HOST_ID, createRequest(Set.of(), List.of())))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  // --- update / ownership -------------------------------------------------

  @Test
  void updateOverwritesFieldsWhenOwner() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, HOST_ID, ListingStatus.ACTIVE)));

    ListingResponse response = service.update(HOST_ID, 5L, updateRequest());

    assertThat(response.title()).isEqualTo("Sea view loft");
    assertThat(response.city()).isEqualTo("Lisbon");
    assertThat(response.pricePerNight()).isEqualByComparingTo("120.00");
  }

  @Test
  void updateIsForbiddenWhenCallerIsNotTheOwner() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, OTHER_HOST_ID, ListingStatus.ACTIVE)));

    assertThatThrownBy(() -> service.update(HOST_ID, 5L, updateRequest()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void updateIsForbiddenWhenListingMissing() {
    when(listingRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(HOST_ID, 5L, updateRequest()))
        .isInstanceOf(AccessDeniedException.class);
  }

  // --- changeStatus -------------------------------------------------------

  @Test
  void changeStatusDeactivatesWhenOwner() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, HOST_ID, ListingStatus.ACTIVE)));

    ListingResponse response =
        service.changeStatus(HOST_ID, 5L, new ListingStatusRequest(ListingStatus.INACTIVE));

    assertThat(response.status()).isEqualTo(ListingStatus.INACTIVE);
  }

  @Test
  void changeStatusIsForbiddenWhenCallerIsNotTheOwner() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, OTHER_HOST_ID, ListingStatus.ACTIVE)));

    assertThatThrownBy(
            () -> service.changeStatus(HOST_ID, 5L, new ListingStatusRequest(ListingStatus.INACTIVE)))
        .isInstanceOf(AccessDeniedException.class);
  }

  // --- public detail ------------------------------------------------------

  @Test
  void getPublicDetailReturnsActiveListing() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, HOST_ID, ListingStatus.ACTIVE)));

    ListingResponse response = service.getPublicDetail(5L);

    assertThat(response.id()).isEqualTo(5L);
  }

  @Test
  void getPublicDetailHidesInactiveListing() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, HOST_ID, ListingStatus.INACTIVE)));

    assertThatThrownBy(() -> service.getPublicDetail(5L))
        .isInstanceOf(ListingNotFoundException.class);
  }

  @Test
  void getPublicDetailThrowsWhenMissing() {
    when(listingRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPublicDetail(5L))
        .isInstanceOf(ListingNotFoundException.class);
  }

  // --- host listings ------------------------------------------------------

  @Test
  void getHostListingsReturnsSummaries() {
    when(listingRepository.findByHostIdOrderByCreatedAtDesc(HOST_ID))
        .thenReturn(
            List.of(
                existingListing(1L, HOST_ID, ListingStatus.ACTIVE),
                existingListing(2L, HOST_ID, ListingStatus.INACTIVE)));

    List<ListingSummaryResponse> summaries = service.getHostListings(HOST_ID);

    assertThat(summaries).extracting(ListingSummaryResponse::id).containsExactly(1L, 2L);
    assertThat(summaries)
        .extracting(ListingSummaryResponse::status)
        .containsExactly(ListingStatus.ACTIVE, ListingStatus.INACTIVE);
  }

  // --- owned detail (edit prefill) ----------------------------------------

  @Test
  void getOwnedDetailReturnsOwnedListingRegardlessOfStatus() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, HOST_ID, ListingStatus.INACTIVE)));

    ListingResponse response = service.getOwnedDetail(HOST_ID, 5L);

    assertThat(response.id()).isEqualTo(5L);
    assertThat(response.status()).isEqualTo(ListingStatus.INACTIVE);
  }

  @Test
  void getOwnedDetailIsForbiddenWhenCallerIsNotTheOwner() {
    when(listingRepository.findById(5L))
        .thenReturn(Optional.of(existingListing(5L, OTHER_HOST_ID, ListingStatus.ACTIVE)));

    assertThatThrownBy(() -> service.getOwnedDetail(HOST_ID, 5L))
        .isInstanceOf(AccessDeniedException.class);
  }
}
