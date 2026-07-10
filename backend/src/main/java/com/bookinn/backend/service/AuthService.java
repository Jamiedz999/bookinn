package com.bookinn.backend.service;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.dto.LoginRequest;
import com.bookinn.backend.dto.RegisterRequest;
import com.bookinn.backend.exception.EmailAlreadyExistsException;
import com.bookinn.backend.exception.InvalidCredentialsException;
import com.bookinn.backend.exception.InvalidRefreshTokenException;
import com.bookinn.backend.repository.UserRepository;
import com.bookinn.backend.security.JwtService;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Orchestrates registration, login, token refresh, logout, and one-click demo login. */
@Service
public class AuthService {

  private static final String DEMO_HOST_EMAIL = "demo-host@bookinn.app";
  private static final String DEMO_GUEST_EMAIL = "demo-guest@bookinn.app";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  /**
   * Creates the service.
   *
   * @param userRepository user store
   * @param passwordEncoder BCrypt encoder
   * @param authenticationManager verifies login credentials
   * @param jwtService issues access tokens
   * @param refreshTokenService issues and rotates refresh tokens
   */
  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  /**
   * Registers a new GUEST account.
   *
   * @param request the registration payload
   * @return the created user
   * @throws EmailAlreadyExistsException if the email is already taken
   */
  @Transactional
  public User register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException("Email already registered: " + request.email());
    }
    User user =
        new User(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.name(),
            false,
            EnumSet.of(Role.GUEST));
    return userRepository.save(user);
  }

  /**
   * Authenticates an email/password login and issues tokens.
   *
   * @param request the login payload
   * @return access token, refresh token, and user
   * @throws InvalidCredentialsException if the email or password is wrong
   */
  @Transactional
  public AuthResult login(LoginRequest request) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    } catch (AuthenticationException ex) {
      throw new InvalidCredentialsException("Invalid email or password");
    }
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
    return issueTokens(user);
  }

  /**
   * Rotates a refresh token and issues a fresh access token.
   *
   * @param rawRefreshToken the refresh token from the cookie
   * @return new access token, new refresh token, and user
   * @throws InvalidRefreshTokenException if the token is missing, unknown, revoked, or expired
   */
  @Transactional
  public AuthResult refresh(String rawRefreshToken) {
    if (!StringUtils.hasText(rawRefreshToken)) {
      throw new InvalidRefreshTokenException("Missing refresh token");
    }
    RefreshResult rotated = refreshTokenService.rotate(rawRefreshToken);
    String accessToken = jwtService.generateAccessToken(rotated.user());
    return new AuthResult(accessToken, rotated.rawToken(), rotated.user());
  }

  /**
   * Revokes a refresh token on logout. Missing tokens are ignored.
   *
   * @param rawRefreshToken the refresh token from the cookie, if any
   */
  @Transactional
  public void logout(String rawRefreshToken) {
    if (StringUtils.hasText(rawRefreshToken)) {
      refreshTokenService.revoke(rawRefreshToken);
    }
  }

  /**
   * Signs in as a demo persona, creating the demo account on first use.
   *
   * @param role the demo role to assume
   * @return access token, refresh token, and the demo user
   */
  @Transactional
  public AuthResult demoLogin(Role role) {
    String email = role == Role.HOST ? DEMO_HOST_EMAIL : DEMO_GUEST_EMAIL;
    User user = userRepository.findByEmail(email).orElseGet(() -> createDemoUser(role));
    return issueTokens(user);
  }

  private User createDemoUser(Role role) {
    boolean host = role == Role.HOST;
    String email = host ? DEMO_HOST_EMAIL : DEMO_GUEST_EMAIL;
    String name = host ? "Demo Host" : "Demo Guest";
    Set<Role> roles = host ? EnumSet.of(Role.GUEST, Role.HOST) : EnumSet.of(Role.GUEST);
    User user =
        new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), name, true, roles);
    return userRepository.save(user);
  }

  private AuthResult issueTokens(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String rawRefreshToken = refreshTokenService.issue(user);
    return new AuthResult(accessToken, rawRefreshToken, user);
  }
}
