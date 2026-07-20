package com.bookinn.backend.service;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.exception.InvalidCredentialsException;
import com.bookinn.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read and self-service access to the current user's profile. */
@Service
public class UserService {

  private final UserRepository userRepository;

  /**
   * Creates the service.
   *
   * @param userRepository user store
   */
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Loads the current user's public profile.
   *
   * @param userId id taken from the authenticated principal
   * @return the user's public view
   * @throws InvalidCredentialsException if the authenticated user no longer exists
   */
  @Transactional(readOnly = true)
  public UserResponse getById(Long userId) {
    return userRepository
        .findById(userId)
        .map(UserResponse::from)
        .orElseThrow(() -> new InvalidCredentialsException("Authenticated user no longer exists"));
  }

  /**
   * Grants the current user the HOST role so they can manage listings. Idempotent: a user who is
   * already a host is returned unchanged.
   *
   * <p>Roles are embedded in the access token at login, so the returned view advertising HOST does
   * not by itself let the current token through HOST-only endpoints — the caller must obtain a new
   * access token (re-login, or refresh) before {@code @PreAuthorize("hasRole('HOST')")} accepts it.
   *
   * @param userId id taken from the authenticated principal
   * @return the user's public view including the HOST role
   * @throws InvalidCredentialsException if the authenticated user no longer exists
   */
  @Transactional
  public UserResponse becomeHost(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new InvalidCredentialsException("Authenticated user no longer exists"));
    user.addRole(Role.HOST);
    return UserResponse.from(userRepository.save(user));
  }
}
