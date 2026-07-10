package com.bookinn.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookinn.backend.config.JwtProperties;
import com.bookinn.backend.domain.RefreshToken;
import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.exception.InvalidRefreshTokenException;
import com.bookinn.backend.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  private static final Duration REFRESH_TTL = Duration.ofDays(7);
  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

  @Mock private RefreshTokenRepository refreshTokenRepository;

  private RefreshTokenService service;

  @BeforeEach
  void setUp() {
    JwtProperties properties =
        new JwtProperties("secret-key-long-enough-for-hs256-0123456789", Duration.ofMinutes(15), REFRESH_TTL);
    service = new RefreshTokenService(refreshTokenRepository, properties, Clock.fixed(T0, ZoneOffset.UTC));
  }

  private User user() {
    User user = new User("bob@example.com", "hash", "Bob", false, EnumSet.of(Role.GUEST));
    user.setId(7L);
    return user;
  }

  @Test
  void issuePersistsHashedTokenWithExpiryAndReturnsRawValue() {
    String raw = service.issue(user());

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken saved = captor.getValue();
    assertThat(raw).isNotBlank();
    assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(raw);
    assertThat(saved.getExpiresAt()).isEqualTo(T0.plus(REFRESH_TTL));
    assertThat(saved.isRevoked()).isFalse();
  }

  @Test
  void rotateRevokesOldTokenAndIssuesNew() {
    RefreshToken existing = new RefreshToken(user(), "storedhash", T0.plusSeconds(1000));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

    RefreshResult result = service.rotate("raw-token");

    assertThat(existing.isRevoked()).isTrue();
    assertThat(result.user().getId()).isEqualTo(7L);
    assertThat(result.rawToken()).isNotBlank();
    verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
  }

  @Test
  void rotateRejectsUnknownToken() {
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.rotate("raw-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void rotateRejectsRevokedToken() {
    RefreshToken existing = new RefreshToken(user(), "storedhash", T0.plusSeconds(1000));
    existing.setRevoked(true);
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.rotate("raw-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void rotateRejectsExpiredToken() {
    RefreshToken existing = new RefreshToken(user(), "storedhash", T0.minusSeconds(1));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.rotate("raw-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void revokeMarksExistingTokenRevoked() {
    RefreshToken existing = new RefreshToken(user(), "storedhash", T0.plusSeconds(1000));
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

    service.revoke("raw-token");

    assertThat(existing.isRevoked()).isTrue();
    verify(refreshTokenRepository).save(existing);
  }

  @Test
  void revokeIgnoresUnknownToken() {
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    service.revoke("raw-token");

    verify(refreshTokenRepository, never()).save(any());
  }
}
