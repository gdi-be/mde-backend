package de.terrestris.mde.migration;

import de.terrestris.mde.mde_backend.MdeBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
@SpringBootTest(classes = MdeBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("") 
public class ProductionMigrationExecutionIT {

  static {
    System.setProperty("VARIABLE_FILE", Paths.get("..", "mde-services", "src", "test", "resources", "variables.json").toAbsolutePath().toString());
    System.setProperty("CODELISTS_DIR", Paths.get("..", "mde-services", "src", "test", "resources", "codelists").toAbsolutePath().toString());
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
      .withDatabaseName("mde_production_test")
      .withUsername("postgres")
      .withPassword("postgres");

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    postgres.start();
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.properties.hibernate.search.backend.directory.root",
        () -> Paths.get(System.getProperty("java.io.tmpdir"), "lucene-test-production-migration").toString());
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/jwks");
    registry.add("KEYCLOAK_HOST", () -> "localhost");
    registry.add("KEYCLOAK_REALM", () -> "test-realm");
    registry.add("KEYCLOAK_CLIENT_SECRET", () -> "test-secret");
    registry.add("KEYCLOAK_CLIENT_ID", () -> "test-client");
    registry.add("CSW_SERVER", () -> "http://localhost/csw");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    registry.add("spring.flyway.locations", () -> "classpath:db/migration");
  }

  @Test
  public void testProductionMigrationsExecuteSuccessfully() {
    assertNotNull(jdbcTemplate, "Database connection should be available");
    assertDoesNotThrow(() ->
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history",
            Integer.class
        ),
        "Flyway migration table should exist after Spring Boot startup"
    );
  }

  @Test
  public void testAllMigrationsAppliedWithProductionConfig() {
    Integer migrationCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
        Integer.class
    );
    
    assertNotNull(migrationCount, "Migration history should exist");
    assertTrue(migrationCount > 0, 
        "At least one migration should have executed successfully with production config");
    
    assertTrue(migrationCount >= 13,
        "All migrations should have executed - expected 13, got: " + migrationCount);
  }

  @Test
  public void testNoFailedMigrationsWithProductionConfig() {
    Integer failedMigrations = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false",
        Integer.class
    );
    
    assertEquals(0, failedMigrations,
        "No migrations should fail with production configuration - " +
        "if this fails, check migration SQL syntax and database compatibility");
  }

  @Test
  public void testLatestMigrationVersionApplied() {
    String latestVersion = jdbcTemplate.queryForObject(
        "SELECT version FROM flyway_schema_history " +
        "WHERE success = true " +
        "ORDER BY installed_rank DESC LIMIT 1",
        String.class
    );
    
    assertNotNull(latestVersion, "A migration should have been applied");
    assertEquals("2.0.9", latestVersion,
        "Latest migration should be V2.0.9 - " +
        "if different, check if migrations were added or removed");
  }

  @Test
  public void testMigrationSequenceIsConsistent() {
    var versions = jdbcTemplate.queryForList(
        "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
        String.class
    );
    
    assertFalse(versions.isEmpty(), "At least one migration should be applied");
    
    assertTrue(versions.contains("1.0.0"), "V1.0.0__initialisation.sql should execute");
    assertTrue(versions.contains("1.0.1"), "V1.0.1__remove_title_from_metadata.sql should execute");
    assertTrue(versions.contains("2.0.0"), "V2.0.0__refactor_base_models.sql should execute");
    assertTrue(versions.contains("2.0.9"), "V2.0.9__add_uuid_fields_to_json_objects.sql should execute");
  }

  @Test
  public void testDatabaseSchemaIsInitializedWithProductionConfig() {
    Integer tableCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM information_schema.tables " +
        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
        Integer.class
    );
    
    assertTrue(tableCount > 0,
        "Database schema should have tables created by migrations");
  }
}
