package com.bookinn.backend.config;

import com.bookinn.backend.security.JwtAuthenticationFilter;
import com.bookinn.backend.security.JwtService;
import com.bookinn.backend.security.RestAccessDeniedHandler;
import com.bookinn.backend.security.RestAuthenticationEntryPoint;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security. A whitelist permits auth endpoints and API docs; everything else requires
 * a valid access token. Method-level {@code @PreAuthorize} enforces role and ownership rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class SecurityConfig {

  private static final String[] PUBLIC_PATHS = {
    "/api/auth/**",
    "/api/hello",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs",
    "/v3/api-docs/**"
  };

  private static final String[] PUBLIC_GET_PATHS = {"/api/listings/*", "/api/amenities"};

  /**
   * Builds the stateless filter chain.
   *
   * @param http the security builder
   * @param jwtAuthenticationFilter the bearer-token filter
   * @param entryPoint 401 responder
   * @param accessDeniedHandler 403 responder
   * @return the configured filter chain
   * @throws Exception if the chain cannot be built
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RestAuthenticationEntryPoint entryPoint,
      RestAccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * System UTC clock, injected where time must be deterministic under test.
   *
   * @return the clock
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * The access-token signer/verifier.
   *
   * @param properties JWT settings
   * @param clock time source
   * @return the JWT service
   */
  @Bean
  public JwtService jwtService(JwtProperties properties, Clock clock) {
    return new JwtService(properties.secret(), properties.accessTtl(), clock);
  }

  /**
   * BCrypt password encoder used for hashing and verifying passwords.
   *
   * @return the encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Exposes the container-managed authentication manager for password login.
   *
   * @param configuration Spring Security's authentication configuration
   * @return the authentication manager
   * @throws Exception if it cannot be resolved
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }
}
