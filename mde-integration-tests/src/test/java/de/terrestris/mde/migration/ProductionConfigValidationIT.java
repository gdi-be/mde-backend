package de.terrestris.mde.migration;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionConfigValidationIT {

  private Properties loadProductionProperties() {
    Properties properties = new Properties();
    try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties")) {
      assertNotNull(stream, "application.properties must be available on test classpath");
      properties.load(stream);
      return properties;
    } catch (IOException e) {
      fail("Could not read application.properties", e);
      return properties;
    }
  }

  private String requireProperty(Properties properties, String key) {
    String value = properties.getProperty(key);
    assertNotNull(value, key + " must be configured");
    assertFalse(value.isBlank(), key + " cannot be blank");
    return value;
  }

  @Test
  public void testDatabaseUrlPointsToPostgres() {
    Properties properties = loadProductionProperties();
    String datasourceUrl = requireProperty(properties, "spring.datasource.url");
    assertTrue(datasourceUrl.contains("postgresql") || datasourceUrl.contains("postgres"),
        "Database should be PostgreSQL - current value: " + datasourceUrl);
  }

  @Test
  public void testDatabaseUsernameIsConfigured() {
    Properties properties = loadProductionProperties();
    String datasourceUsername = requireProperty(properties, "spring.datasource.username");
    assertFalse(datasourceUsername.isEmpty(),
        "spring.datasource.username must be configured");
  }

  @Test
  public void testDatabasePasswordIsConfigured() {
    Properties properties = loadProductionProperties();
    String datasourcePassword = requireProperty(properties, "spring.datasource.password");
    assertFalse(datasourcePassword.isEmpty(),
        "spring.datasource.password must be configured");
  }

  @Test
  public void testFlywayIsEnabled() {
    Properties properties = loadProductionProperties();
    assertEquals("true", properties.getProperty("spring.flyway.enabled"),
        "spring.flyway.enabled must be true");
  }

  @Test
  public void testFlywayLocationsAreConfigured() {
    Properties properties = loadProductionProperties();
    String flywayLocations = properties.getProperty("spring.flyway.locations");
    if (flywayLocations != null) {
      assertFalse(flywayLocations.isBlank(),
        "spring.flyway.locations cannot be blank when provided");
      assertTrue(flywayLocations.contains("db/migration"),
        "Flyway must point to classpath:db/migration when explicitly configured");
    }
  }

  @Test
  public void testJpaOpenInViewIsDisabledInProduction() {
    Properties properties = loadProductionProperties();
    assertEquals("false", properties.getProperty("spring.jpa.open-in-view"),
        "spring.jpa.open-in-view should be false in production");
  }

  @Test
  public void testJwtIssuerUriIsConfigured() {
    Properties properties = loadProductionProperties();
    String jwtIssuerUri = requireProperty(properties, "spring.security.oauth2.resourceserver.jwt.issuer-uri");
    assertFalse(jwtIssuerUri.isEmpty(),
        "spring.security.oauth2.resourceserver.jwt.issuer-uri must be configured");
  }

  @Test
  public void testKeycloakRealmIsConfigured() {
    Properties properties = loadProductionProperties();
    String keycloakRealm = requireProperty(properties, "keycloak.realm");
    assertFalse(keycloakRealm.isEmpty(),
        "keycloak.realm must be configured");
  }

  @Test
  public void testKeycloakClientIdIsConfigured() {
    Properties properties = loadProductionProperties();
    String keycloakClientId = requireProperty(properties, "keycloak.client-id");
    assertFalse(keycloakClientId.isEmpty(),
        "keycloak.client-id must be configured");
  }

  @Test
  public void testApplicationNameIsConfigured() {
    Properties properties = loadProductionProperties();
    String applicationName = requireProperty(properties, "spring.application.name");
    assertFalse(applicationName.isEmpty(),
        "spring.application.name must be configured");
  }

  @Test
  public void testCswServerIsConfigured() {
    Properties properties = loadProductionProperties();
    String cswServer = requireProperty(properties, "csw.server");
    assertFalse(cswServer.isEmpty(),
        "csw.server must be configured");
  }
}
