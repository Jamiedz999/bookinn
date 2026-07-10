package com.bookinn.backend.security;

import com.bookinn.backend.domain.Role;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The authenticated principal placed in the security context by {@link JwtAuthenticationFilter}.
 * Controllers receive it via {@code @AuthenticationPrincipal}.
 *
 * @param id id of the authenticated user
 * @param email login email of the user
 * @param roles roles granted to the user
 */
public record AuthenticatedUser(Long id, String email, Set<Role> roles) {

  /**
   * Maps this user's roles to Spring Security authorities, prefixed with {@code ROLE_} so that
   * {@code hasRole(...)} checks resolve.
   *
   * @return the granted authorities
   */
  public List<GrantedAuthority> authorities() {
    return roles.stream()
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
        .toList();
  }
}
