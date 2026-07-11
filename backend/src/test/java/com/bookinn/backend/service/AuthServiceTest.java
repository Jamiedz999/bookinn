package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  private AuthService authService() {
    return new AuthService(
        userRepository, passwordEncoder, authenticationManager, jwtService, refreshTokenService);
  }

  private User guest() {
    User user = new User("alice@example.com", "enc", "Alice", false, EnumSet.of(Role.GUEST));
    user.setId(1L);
    return user;
  }

  @Test
  void registerRejectsDuplicateEmail() {
    when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

    assertThatThrownBy(
            () ->
                authService()
                    .register(new RegisterRequest("alice@example.com", "password123", "Alice")))
        .isInstanceOf(EmailAlreadyExistsException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void registerHashesPasswordAndCreatesGuest() {
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    authService().register(new RegisterRequest("alice@example.com", "password123", "Alice"));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getPasswordHash()).isEqualTo("hashed");
    assertThat(saved.getRoles()).containsExactly(Role.GUEST);
    assertThat(saved.isDemo()).isFalse();
  }

  @Test
  void loginIssuesTokensOnValidCredentials() {
    when(authenticationManager.authenticate(any()))
        .thenReturn(new UsernamePasswordAuthenticationToken("alice@example.com", "password123"));
    when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(guest()));
    when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
    when(refreshTokenService.issue(any(User.class))).thenReturn("refresh-token");

    AuthResult result = authService().login(new LoginRequest("alice@example.com", "password123"));

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.rawRefreshToken()).isEqualTo("refresh-token");
    assertThat(result.user().getEmail()).isEqualTo("alice@example.com");
  }

  @Test
  void loginRejectsBadCredentials() {
    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(
            () -> authService().login(new LoginRequest("alice@example.com", "wrong")))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(refreshTokenService, never()).issue(any());
  }

  @Test
  void refreshRotatesTokenAndMintsAccess() {
    when(refreshTokenService.rotate("old-refresh"))
        .thenReturn(new RefreshResult(guest(), "new-refresh"));
    when(jwtService.generateAccessToken(any(User.class))).thenReturn("new-access");

    AuthResult result = authService().refresh("old-refresh");

    assertThat(result.accessToken()).isEqualTo("new-access");
    assertThat(result.rawRefreshToken()).isEqualTo("new-refresh");
  }

  @Test
  void refreshRejectsMissingToken() {
    assertThatThrownBy(() -> authService().refresh("  "))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(refreshTokenService, never()).rotate(any());
  }

  @Test
  void logoutRevokesPresentedToken() {
    authService().logout("some-token");

    verify(refreshTokenService).revoke("some-token");
  }

  @Test
  void logoutIgnoresMissingToken() {
    authService().logout(null);

    verify(refreshTokenService, never()).revoke(any());
  }

  @Test
  void demoLoginReusesExistingDemoAccount() {
    User demo = new User("demo-host@bookinn.app", "enc", "Demo Host", true, EnumSet.of(Role.HOST));
    demo.setId(9L);
    when(userRepository.findByEmail("demo-host@bookinn.app")).thenReturn(Optional.of(demo));
    when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
    when(refreshTokenService.issue(any(User.class))).thenReturn("refresh");

    AuthResult result = authService().demoLogin(Role.HOST);

    assertThat(result.user().getId()).isEqualTo(9L);
    verify(userRepository, never()).save(any());
  }

  @Test
  void demoLoginCreatesDemoAccountOnFirstUse() {
    when(userRepository.findByEmail("demo-guest@bookinn.app")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("enc");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
    when(refreshTokenService.issue(any(User.class))).thenReturn("refresh");

    AuthResult result = authService().demoLogin(Role.GUEST);

    assertThat(result.user().isDemo()).isTrue();
    assertThat(result.user().getRoles()).containsExactly(Role.GUEST);
    verify(userRepository).save(any(User.class));
  }
}
