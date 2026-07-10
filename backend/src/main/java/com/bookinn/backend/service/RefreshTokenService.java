package com.bookinn.backend.service;

import com.bookinn.backend.config.JwtProperties;
import com.bookinn.backend.domain.RefreshToken;
import com.bookinn.backend.domain.User;
import com.bookinn.backend.exception.InvalidRefreshTokenException;
import com.bookinn.backend.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates, and revokes opaque refresh tokens. The raw token is a 256-bit random string
 * returned to the client; only its SHA-256 hash is persisted. Rotation revokes the consumed token
 * and issues a fresh one, so a stolen token stops working once the legitimate client refreshes.
 */
@Service
public class RefreshTokenService {

  private static final int TOKEN_BYTES = 32;

  private final RefreshTokenRepository refreshTokenRepository;
  private final Duration refreshTtl;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Creates the service.
   *
   * @param refreshTokenRepository token store
   * @param jwtProperties supplies the refresh-token lifetime
   * @param clock time source
   */
  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties, Clock clock) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTtl = jwtProperties.refreshTtl();
    this.clock = clock;
  }

  /**
   * Issues a new refresh token for the user and persists its hash.
   *
   * @param user the owner
   * @return the raw token to send to the client
   */
  @Transactional
  public String issue(User user) {
    String rawToken = generateRawToken();
    RefreshToken token =
        new RefreshToken(user, hash(rawToken), clock.instant().plus(refreshTtl));
    refreshTokenRepository.save(token);
    return rawToken;
  }

  /**
   * Validates a presented refresh token and rotates it: the old token is revoked and a new one is
   * issued.
   *
   * @param rawToken the raw token presented by the client
   * @return the owner and the replacement raw token
   * @throws InvalidRefreshTokenException if the token is unknown, revoked, or expired
   */
  @Transactional
  public RefreshResult rotate(String rawToken) {
    RefreshToken existing =
        refreshTokenRepository
            .findByTokenHash(hash(rawToken))
            .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));
    if (existing.isRevoked()) {
      throw new InvalidRefreshTokenException("Refresh token has been revoked");
    }
    if (existing.getExpiresAt().isBefore(clock.instant())) {
      throw new InvalidRefreshTokenException("Refresh token has expired");
    }
    existing.setRevoked(true);
    refreshTokenRepository.save(existing);
    User user = existing.getUser();
    return new RefreshResult(user, issue(user));
  }

  /**
   * Revokes a presented refresh token if it exists. Idempotent: unknown tokens are ignored.
   *
   * @param rawToken the raw token to revoke
   */
  @Transactional
  public void revoke(String rawToken) {
    refreshTokenRepository
        .findByTokenHash(hash(rawToken))
        .ifPresent(
            token -> {
              token.setRevoked(true);
              refreshTokenRepository.save(token);
            });
  }

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
