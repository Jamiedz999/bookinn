package com.bookinn.backend.security;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users by email for the login {@code AuthenticationManager}. Only used to verify credentials
 * during password login; token-authenticated requests bypass this and are resolved from JWT claims.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Creates the service.
   *
   * @param userRepository user store
   */
  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No user for email " + email));
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getEmail())
        .password(user.getPasswordHash())
        .authorities(user.getRoles().stream().map(this::toAuthority).toList())
        .build();
  }

  private SimpleGrantedAuthority toAuthority(Role role) {
    return new SimpleGrantedAuthority("ROLE_" + role.name());
  }
}
