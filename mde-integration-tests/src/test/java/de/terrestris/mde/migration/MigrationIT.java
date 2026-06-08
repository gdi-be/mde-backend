package de.terrestris.mde.migration;

import de.terrestris.mde.mde_backend.MdeBackendApplication;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
@SpringBootTest(classes = MdeBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("it")
@Import(MigrationIT.TestConfig.class)
public class MigrationIT {

  static {
    System.setProperty("VARIABLE_FILE", Paths.get("..", "mde-services", "src", "test", "resources", "variables.json").toAbsolutePath().toString());
    System.setProperty("CODELISTS_DIR", Paths.get("..", "mde-services", "src", "test", "resources", "codelists").toAbsolutePath().toString());
  }

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @LocalServerPort
  private int port;

  @TestConfiguration
  static class TestConfig {
    @Bean(initMethod = "migrate")
    public org.flywaydb.core.Flyway flyway(DataSource dataSource) {
      return org.flywaydb.core.Flyway.configure()
          .dataSource(dataSource)
          .locations("classpath:db/migration")
          .baselineOnMigrate(true)
          .load();
    }
  }

  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
      .withDatabaseName("mde_test")
      .withUsername("postgres")
      .withPassword("postgres");

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    postgres.start();
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.properties.hibernate.search.backend.directory.root",
        () -> Paths.get(System.getProperty("java.io.tmpdir"), "lucene-test-migration-it").toString());
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/jwks");
    registry.add("KEYCLOAK_HOST", () -> "localhost");
    registry.add("KEYCLOAK_REALM", () -> "test-realm");
    registry.add("KEYCLOAK_CLIENT_SECRET", () -> "test-secret");
    registry.add("KEYCLOAK_CLIENT_ID", () -> "test-client");
    registry.add("CSW_SERVER", () -> "http://localhost/csw");
  }

  @BeforeEach
  public void setup() {
    RestAssured.port = port;
  }

  @Test
  public void testMigrationsExecuteSuccessfully() {
    Integer schemaVersions = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history",
        Integer.class);
    assertNotNull(schemaVersions);
    assertTrue(schemaVersions > 0, "Flyway migrations should have been executed");
  }

  @Test
  public void testLatestMigrationApplied() {
    String latestVersion = jdbcTemplate.queryForObject(
        "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1",
        String.class);
    assertNotNull(latestVersion, "At least one migration should be applied");
    assertTrue(latestVersion.matches("\\d+\\.\\d+\\.\\d+"),
        "Migration version should follow semantic versioning");
  }

  @Test
  public void testMetadataTableExists() throws SQLException {
    DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
    ResultSet tables = metaData.getTables(null, "public", "metadata%", new String[] { "TABLE" });

    assertTrue(tables.next(), "Metadata table should exist after migrations");
  }

  @Test
  public void testMigrationHistoryIsConsistent() {
    Integer failedMigrations = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false",
        Integer.class);
    assertEquals(0, failedMigrations, "All migrations should have completed successfully");
  }

  @Test
  public void testMigrationVersionSequence() {
    String result = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history",
        String.class);
    assertNotNull(result);
    int migrationCount = Integer.parseInt(result);
    assertTrue(migrationCount >= 13, "Expected at least 13 migrations (V1.0.0 to V2.0.9)");
  }
}
