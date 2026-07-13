package com.bookinn.backend.repository;

import com.bookinn.backend.domain.Amenity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the {@link Amenity} dictionary. */
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

  /**
   * Loads amenities in a stable alphabetical order for the public dictionary.
   *
   * @return all amenities ordered by name
   */
  List<Amenity> findAllByOrderByNameAsc();
}
