package com.bookinn.backend.controller;

import com.bookinn.backend.config.AuthProperties;
import com.bookinn.backend.config.JwtProperties;
import com.bookinn.backend.dto.AuthResponse;
import com.bookinn.backend.dto.DemoLoginRequest;
import com.bookinn.backend.dto.LoginRequest;
import com.bookinn.backend.dto.RegisterRequest;
import com.bookinn.backend.dto.UserResponse;
import com.bookinn.backend.service.AuthResult;
import com.bookinn.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints. The refresh token is carried only in an httpOnly cookie. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final AuthProperties authProperties;
  private final JwtProperties jwtProperties;

  /**
   * Creates the controller.
   *
   * @param authService auth orchestration
   * @param authProperties cookie settings
   * @param jwtProperties token lifetimes
   */
  public AuthController(
      AuthService authService, AuthProperties authProperties, JwtProperties jwtProperties) {
    this.authService = authService;
    this.authProperties = authProperties;
    this.jwtProperties = jwtProperties;
  }

  /**
   * Registers a new GUEST account.
   *
   * @param request the registration payload
   * @return 201 with the created user
   */
  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(UserResponse.from(authService.register(request)));
  }

  /**
   * Logs in with email and password.
   *
   * @param request the login payload
   * @return 200 with an access token and the refresh cookie set
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return authResponse(authService.login(request));
  }

  /**
   * Exchanges the refresh cookie for a new access token, rotating the refresh token.
   *
   * @param request the HTTP request carrying the refresh cookie
   * @return 200 with a new access token and rotated refresh cookie
   */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
    return authResponse(authService.refresh(readRefreshCookie(request).orElse(null)));
  }

  /**
   * Revokes the refresh token and clears the cookie.
   *
   * @param request the HTTP request carrying the refresh cookie
   * @return 204 with the refresh cookie cleared
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    authService.logout(readRefreshCookie(request).orElse(null));
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
        .build();
  }

  /**
   * Signs in as a demo persona.
   *
   * @param request the demo-login payload
   * @return 200 with an access token and the refresh cookie set
   */
  @PostMapping("/demo-login")
  public ResponseEntity<AuthResponse> demoLogin(@Valid @RequestBody DemoLoginRequest request) {
    return authResponse(authService.demoLogin(request.role()));
  }

  private ResponseEntity<AuthResponse> authResponse(AuthResult result) {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.rawRefreshToken()).toString())
        .body(new AuthResponse(result.accessToken(), UserResponse.from(result.user())));
  }

  private Optional<String> readRefreshCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(cookie -> authProperties.refreshCookieName().equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

  private ResponseCookie buildRefreshCookie(String rawToken) {
    return baseCookie(rawToken).maxAge(jwtProperties.refreshTtl()).build();
  }

  private ResponseCookie clearRefreshCookie() {
    return baseCookie("").maxAge(0).build();
  }

  private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
    return ResponseCookie.from(authProperties.refreshCookieName(), value)
        .httpOnly(true)
        .secure(authProperties.cookieSecure())
        .sameSite("Strict")
        .path("/");
  }
}
