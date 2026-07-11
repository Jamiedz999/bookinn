package com.bookinn.backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from its {@code Authorization: Bearer <jwt>} header. A valid token
 * populates the security context; a missing or invalid token leaves the request anonymous so the
 * filter chain can reject protected endpoints with 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer ";

  private final JwtService jwtService;

  /**
   * Creates the filter.
   *
   * @param jwtService verifies bearer access tokens
   */
  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER);
    if (header != null
        && header.startsWith(PREFIX)
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        AccessTokenClaims claims = jwtService.parse(header.substring(PREFIX.length()));
        AuthenticatedUser principal =
            new AuthenticatedUser(claims.userId(), claims.email(), claims.roles());
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtException ex) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
