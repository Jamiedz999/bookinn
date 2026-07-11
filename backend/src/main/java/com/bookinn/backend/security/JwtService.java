package com.bookinn.backend.security;

import com.bookinn.backend.domain.Role;
import com.bookinn.backend.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

/**
 * Signs and verifies stateless JWT access tokens (HMAC-SHA). The token carries the user id as
 * subject plus email and role claims. A {@link Clock} is injected so token expiry is deterministic
 * under test.
 */
public final class JwtService {

  private static final String CLAIM_EMAIL = "email";
  private static final String CLAIM_ROLES = "roles";

  private final SecretKey key;
  private final Duration accessTtl;
  private final Clock clock;

  /**
   * Creates a JWT service.
   *
   * @param secret HMAC secret; must be at least 32 bytes
   * @param accessTtl lifetime of issued access tokens
   * @param clock time source used for issue and expiry checks
   */
  public JwtService(String secret, Duration accessTtl, Clock clock) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTtl = accessTtl;
    this.clock = clock;
  }

  /**
   * Issues a signed access token for the given user.
   *
   * @param user the authenticated user
   * @return a compact JWT string
   */
  public String generateAccessToken(User user) {
    Instant now = clock.instant();
    List<String> roleNames = user.getRoles().stream().map(Role::name).toList();
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim(CLAIM_EMAIL, user.getEmail())
        .claim(CLAIM_ROLES, roleNames)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessTtl)))
        .signWith(key)
        .compact();
  }

  /**
   * Verifies a token's signature and expiry and extracts its claims.
   *
   * @param token the compact JWT string
   * @return the verified claims
   * @throws io.jsonwebtoken.JwtException if the token is malformed, tampered, or expired
   */
  public AccessTokenClaims parse(String token) {
    Claims claims =
        Jwts.parser()
            .verifyWith(key)
            .clock(() -> Date.from(clock.instant()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    Long userId = Long.valueOf(claims.getSubject());
    String email = claims.get(CLAIM_EMAIL, String.class);
    List<?> rawRoles = claims.get(CLAIM_ROLES, List.class);
    Set<Role> roles =
        rawRoles.stream()
            .map(Object::toString)
            .map(Role::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
    return new AccessTokenClaims(userId, email, roles);
  }
}
