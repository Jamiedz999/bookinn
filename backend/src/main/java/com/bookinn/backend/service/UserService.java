package com.bookinn.backend.service;

import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.exception.InvalidCredentialsException;
import com.bookinn.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read access to the current user's profile. */
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
}
