package com.bookinn.backend.controller;

import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.security.AuthenticatedUser;
import com.bookinn.backend.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
