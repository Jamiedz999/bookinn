package com.bookinn.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base for full-context integration tests. Boots the app against a real MySQL 8 container so Flyway
 * migrations and JPA mappings are exercised end to end. Requires a running Docker daemon.
 *
 * <p>Uses the Testcontainers singleton pattern: one container is started in a static initializer and
 * shared across every test class (and the cached Spring context), then reaped by Ryuk at JVM exit.
 * A per-class {@code @Container} would be stopped after the first class while the cached context
 * still points at it.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

  @ServiceConnection
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

  static {
    MYSQL.start();
  }
}
