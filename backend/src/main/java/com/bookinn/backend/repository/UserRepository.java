package com.bookinn.backend.repository;

import com.bookinn.backend.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link User} accounts. */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Finds a user by login email.
   *
   * @param email the email to match
   * @return the user, if present
   */
  Optional<User> findByEmail(String email);

  /**
   * Reports whether an account already uses the given email.
   *
   * @param email the email to check
   * @return {@code true} if taken
   */
  boolean existsByEmail(String email);
}
