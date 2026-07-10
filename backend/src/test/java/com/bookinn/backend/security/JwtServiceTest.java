package com.bookinn.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET =
      "test-secret-key-that-is-definitely-long-enough-0123456789abcdef";
  private static final String OTHER_SECRET =
      "another-secret-key-also-long-enough-to-be-valid-0123456789abcdef";
  private static final Duration TTL = Duration.ofMinutes(15);
  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

  private JwtService serviceAt(Instant now, String secret) {
    return new JwtService(secret, TTL, Clock.fixed(now, ZoneOffset.UTC));
  }

  private User guestNamedAlice() {
    User user = new User("alice@example.com", "hash", "Alice", false, EnumSet.of(Role.GUEST));
    user.setId(42L);
    return user;
  }

  @Test
  void roundTripsUserIdEmailAndRoles() {
    JwtService service = serviceAt(T0, SECRET);
    User user = guestNamedAlice();
    user.addRole(Role.HOST);

    String token = service.generateAccessToken(user);
    AccessTokenClaims claims = service.parse(token);

    assertThat(claims.userId()).isEqualTo(42L);
    assertThat(claims.email()).isEqualTo("alice@example.com");
    assertThat(claims.roles()).containsExactlyInAnyOrder(Role.GUEST, Role.HOST);
  }

  @Test
  void rejectsExpiredToken() {
    String token = serviceAt(T0, SECRET).generateAccessToken(guestNamedAlice());
    JwtService laterService = serviceAt(T0.plus(TTL).plusSeconds(1), SECRET);

    assertThatThrownBy(() -> laterService.parse(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void acceptsTokenJustBeforeExpiry() {
    String token = serviceAt(T0, SECRET).generateAccessToken(guestNamedAlice());
    JwtService laterService = serviceAt(T0.plus(TTL).minusSeconds(1), SECRET);

    assertThat(laterService.parse(token).userId()).isEqualTo(42L);
  }

  @Test
  void rejectsTokenSignedWithDifferentSecret() {
    String token = serviceAt(T0, OTHER_SECRET).generateAccessToken(guestNamedAlice());
    JwtService service = serviceAt(T0, SECRET);

    assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsTamperedToken() {
    JwtService service = serviceAt(T0, SECRET);
    String token = service.generateAccessToken(guestNamedAlice());
    String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

    assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(JwtException.class);
  }
}
