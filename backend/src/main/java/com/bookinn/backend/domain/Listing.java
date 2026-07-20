package com.bookinn.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A bookable property owned by a host. Carries its amenities (N-M) and ordered photos (1-N). Maps
 * to the {@code listing} table defined in Flyway V1.
 */
@Entity
@Table(name = "listing")
@Getter
@Setter
@NoArgsConstructor
public class Listing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Owning host. Ownership rules compare {@code host.getId()} to the caller's id. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "host_id", nullable = false)
  private User host;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, length = 100)
  private String city;

  @Column(nullable = false, length = 255)
  private String address;

  @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
  private BigDecimal pricePerNight;

  @Column(name = "max_guests", nullable = false)
  private int maxGuests;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ListingStatus status = ListingStatus.ACTIVE;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "listing_amenity",
      joinColumns = @JoinColumn(name = "listing_id"),
      inverseJoinColumns = @JoinColumn(name = "amenity_id"))
  private Set<Amenity> amenities = new LinkedHashSet<>();

  @OneToMany(
      mappedBy = "listing",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("sortOrder ASC")
  private List<ListingPhoto> photos = new ArrayList<>();

  /**
   * Adds a photo and back-links it to this listing so the cascade persists it.
   *
   * @param photo the photo to attach
   */
  public void addPhoto(ListingPhoto photo) {
    photo.setListing(this);
    this.photos.add(photo);
  }

  /** Detaches every photo, letting {@code orphanRemoval} delete the rows. */
  public void clearPhotos() {
    this.photos.forEach(photo -> photo.setListing(null));
    this.photos.clear();
  }
}
