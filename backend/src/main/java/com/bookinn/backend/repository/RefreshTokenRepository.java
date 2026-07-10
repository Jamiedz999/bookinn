package com.bookinn.backend.repository;

import com.bookinn.backend.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for persisted {@link RefreshToken} rows. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Finds a token by the SHA-256 hash of its raw value.
   *
   * @param tokenHash the stored hash
   * @return the token, if present
   */
  Optional<RefreshToken> findByTokenHash(String tokenHash);
}
