package com.bookinn.backend.repository;

import com.bookinn.backend.domain.Listing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Listing} properties. */
public interface ListingRepository extends JpaRepository<Listing, Long> {

  /**
   * Lists a host's own properties, newest first.
   *
   * @param hostId the owning host's id
   * @return the host's listings
   */
  List<Listing> findByHostIdOrderByCreatedAtDesc(Long hostId);
}
