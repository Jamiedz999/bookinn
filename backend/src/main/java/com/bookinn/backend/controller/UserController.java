package com.bookinn.backend.controller;

import com.bookinn.backend.dto.ChangeEmailRequest;
import com.bookinn.backend.dto.ChangePasswordRequest;
import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.security.AuthenticatedUser;
import com.bookinn.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Current-user endpoints. */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  /**
   * Creates the controller.
   *
   * @param userService current-user access
   */
  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Returns the authenticated user's profile.
   *
   * @param principal the authenticated principal
   * @return the current user's public view
   */
  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
    return userService.getById(principal.id());
  }

  /**
   * Grants the authenticated user the HOST role so they can manage listings.
   *
   * @param principal the authenticated principal
   * @return the current user's public view including the HOST role
   */
  @PostMapping("/me/become-host")
  public UserResponse becomeHost(@AuthenticationPrincipal AuthenticatedUser principal) {
    return userService.becomeHost(principal.id());
  }

  /**
   * Changes the authenticated user's password. Protected demo accounts are refused with 403.
   *
   * @param principal the authenticated principal
   * @param request the current and replacement passwords
   * @return an empty 204 response on success
   */
  @PatchMapping("/me/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(principal.id(), request);
    return ResponseEntity.noContent().build();
  }

  /**
   * Changes the authenticated user's login email. Protected demo accounts are refused with 403.
   *
   * @param principal the authenticated principal
   * @param request the replacement email
   * @return the updated profile
   */
  @PatchMapping("/me/email")
  public UserResponse changeEmail(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody ChangeEmailRequest request) {
    return userService.changeEmail(principal.id(), request);
  }
}
