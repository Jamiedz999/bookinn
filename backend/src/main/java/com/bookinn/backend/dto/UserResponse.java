package com.bookinn.backend.dto;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import java.util.Set;

/**
 * Public view of a user account.
 *
 * @param id user id
 * @param email login email
 * @param name display name
 * @param demo whether this is a protected demo account
 * @param roles granted roles
 */
public record UserResponse(Long id, String email, String name, boolean demo, Set<Role> roles) {

  /**
   * Projects a {@link User} entity to its public view.
   *
   * @param user the entity
   * @return the response DTO
   */
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(), user.getEmail(), user.getName(), user.isDemo(), Set.copyOf(user.getRoles()));
  }
}
