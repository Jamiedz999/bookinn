package com.bookinn.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A photo URL for a listing with an explicit display order. Phase 1 stores URLs only (no upload).
 * Maps to the {@code listing_photo} table defined in Flyway V1.
 */
@Entity
@Table(name = "listing_photo")
@Getter
@Setter
@NoArgsConstructor
public class ListingPhoto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "listing_id", nullable = false)
  private Listing listing;

  @Column(nullable = false, length = 500)
  private String url;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  /**
   * Creates a photo with the given URL and position.
   *
   * @param url the image URL
   * @param sortOrder zero-based display position
   */
  public ListingPhoto(String url, int sortOrder) {
    this.url = url;
    this.sortOrder = sortOrder;
  }
}
