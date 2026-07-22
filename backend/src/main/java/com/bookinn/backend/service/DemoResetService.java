package com.bookinn.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wipes the demo's business data and rebuilds it from {@link DemoSeedService} (D7, M6). Running
 * this nightly means any mess an interviewer makes — bad listings, junk accounts, cancelled
 * bookings — lives at most one day, which is why the sandbox needs no admin tooling.
 *
 * <p>Reference data survives: the {@code amenity} dictionary (Flyway V2) and Flyway's own history
 * table are left untouched; only user/listing/booking rows and their join/child tables are cleared.
 * {@code TRUNCATE} is used so auto-increment ids reset and the reseed is identical each night.
 * Foreign-key checks are toggled off around the truncation so table order does not matter.
 */
@Service
public class DemoResetService {

  private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

  /** Business tables cleared on reset (child → parent). Excludes {@code amenity} reference data. */
  private static final List<String> BUSINESS_TABLES =
      List.of(
          "booking",
          "listing_amenity",
          "listing_photo",
          "listing",
          "refresh_token",
          "user_role",
          "users");

  @PersistenceContext private EntityManager entityManager;

  private final DemoSeedService demoSeedService;

  /**
   * Creates the service.
   *
   * @param demoSeedService the seed used to rebuild the dataset after truncation
   */
  public DemoResetService(DemoSeedService demoSeedService) {
    this.demoSeedService = demoSeedService;
  }

  /**
   * Truncates every business table then reseeds the demo dataset from scratch. Note that
   * {@code TRUNCATE} implicitly commits in MySQL, so the truncation is not rolled back if the
   * reseed fails — acceptable here, as the job is deterministic and reruns the next night.
   */
  @Transactional
  public void reset() {
    truncateBusinessTables();
    demoSeedService.seed();
    log.info("Demo data reset complete");
  }

  private void truncateBusinessTables() {
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
    for (String table : BUSINESS_TABLES) {
      entityManager.createNativeQuery("TRUNCATE TABLE " + table).executeUpdate();
    }
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
  }
}
