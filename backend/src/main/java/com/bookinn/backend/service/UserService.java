package com.bookinn.backend.service;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.ChangeEmailRequest;
import com.bookinn.backend.dto.ChangePasswordRequest;
import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.exception.DemoAccountProtectedException;
import com.bookinn.backend.exception.EmailAlreadyExistsException;
import com.bookinn.backend.exception.InvalidCredentialsException;
import com.bookinn.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read and self-service access to the current user's profile. */
@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Creates the service.
   *
   * @param userRepository user store
   * @param passwordEncoder encoder for verifying and hashing passwords
   */
  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
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

  /**
   * Changes the current user's password after verifying the existing one.
   *
   * @param userId id taken from the authenticated principal
   * @param request the current and replacement passwords
   * @throws DemoAccountProtectedException if the account is a protected demo persona (403)
   * @throws InvalidCredentialsException if the current password is wrong, or the user is gone
   */
  @Transactional
  public void changePassword(Long userId, ChangePasswordRequest request) {
    User user = requireMutableUser(userId);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new InvalidCredentialsException("Current password is incorrect");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
  }

  /**
   * Changes the current user's login email.
   *
   * @param userId id taken from the authenticated principal
   * @param request the replacement email
   * @return the user's public view with the new email
   * @throws DemoAccountProtectedException if the account is a protected demo persona (403)
   * @throws EmailAlreadyExistsException if the new email is already taken
   * @throws InvalidCredentialsException if the user is gone
   */
  @Transactional
  public UserResponse changeEmail(Long userId, ChangeEmailRequest request) {
    User user = requireMutableUser(userId);
    String newEmail = request.newEmail();
    if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
      throw new EmailAlreadyExistsException("Email already registered: " + newEmail);
    }
    user.setEmail(newEmail);
    return UserResponse.from(userRepository.save(user));
  }

  /**
   * Loads the user and refuses the mutation if it is a protected demo account.
   *
   * @param userId id taken from the authenticated principal
   * @return the mutable user
   */
  private User requireMutableUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new InvalidCredentialsException("Authenticated user no longer exists"));
    if (user.isDemo()) {
      throw new DemoAccountProtectedException("Demo accounts cannot change email or password");
    }
    return user;
  }
}
