package de.terrestris.mde;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import de.terrestris.mde.mde_backend.MdeBackendApplication;

import io.restassured.RestAssured;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.springframework.jdbc.core.JdbcTemplate;

import org.testcontainers.postgresql.PostgreSQLContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.wiremock.integrations.testcontainers.WireMockContainer;

import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.Base64;

@SpringBootTest(classes = MdeBackendApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Testcontainers

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractApiIT {

  static {

    try {
      ClassLoader cl = AbstractApiIT.class.getClassLoader();

      var variablesStream = cl.getResourceAsStream("variables.json");

      var variablesFile = java.nio.file.Files.createTempFile("variables", ".json");

      java.nio.file.Files.copy(
          variablesStream,
          variablesFile,
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      System.setProperty("VARIABLE_FILE", variablesFile.toString());

      var codelistsDir = java.nio.file.Files.createTempDirectory("codelists");

      String[] files = {
          "codelists/contact.yaml",
          "codelists/terms_of_use.yaml",
          "codelists/metadatavariables.yaml"
      };

      for (String file : files) {

        var stream = cl.getResourceAsStream(file);

        var target = codelistsDir.resolve(
            file.replace("codelists/", ""));

        java.nio.file.Files.copy(
            stream,
            target,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }

      System.setProperty("CODELISTS_DIR", codelistsDir.toString());

    } catch (Exception e) {
      throw new RuntimeException("Failed to load test resources", e);
    }
  }

  @SuppressWarnings("resource")
  @Container
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16")
      .withDatabaseName("mde")
      .withUsername("mde")
      .withPassword("mde");

  @SuppressWarnings("resource")
  @Container
  public static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0")
      .withRealmImportFile("keycloak/test-realm.json");

  @SuppressWarnings("resource")
  @Container
  static WireMockContainer wiremock = new WireMockContainer("wiremock/wiremock:3.3.1");

  @LocalServerPort
  int port;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void setupContainers() {
    RestAssured.baseURI = "http://localhost";

    WireMock wm = new WireMock(wiremock.getHost(), wiremock.getMappedPort(8080));

    wm.register(WireMock.post(WireMock.urlEqualTo("/srv/eng/csw-publication"))
        .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody("""
                <?xml version="1.0"?>
                <csw:TransactionResponse xmlns:csw="http://www.opengis.net/cat/csw/2.0.2">
                  <csw:TransactionSummary>
                    <csw:totalInserted>1</csw:totalInserted>
                    <csw:totalUpdated>0</csw:totalUpdated>
                    <csw:totalDeleted>0</csw:totalDeleted>
                  </csw:TransactionSummary>
                  <csw:InsertResult>
                    <dc:identifier xmlns:dc="http://purl.org/dc/elements/1.1/">stub-uuid</dc:identifier>
                  </csw:InsertResult>
                </csw:TransactionResponse>
                """)));

    wm.register(WireMock.post(WireMock.urlEqualTo("/srv/api/me"))
        .willReturn(WireMock.aResponse()
            .withStatus(204)
            .withHeader("Set-Cookie", "XSRF-TOKEN=test-csrf-token; Path=/")));

    wm.register(WireMock.put(WireMock.urlMatching("/srv/api/records/.*/publish.*"))
        .willReturn(WireMock.aResponse()
            .withStatus(204)));

    wm.register(WireMock.get(WireMock.urlMatching("/srv/api/records/index.*"))
        .willReturn(WireMock.aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody("{}")));
  }

  @BeforeEach
  void setupPort() {
    System.out.println("Spring Boot port: " + port);
    RestAssured.port = port;
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // Database
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // Keycloak JWT validation
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> keycloak.getAuthServerUrl() + "/realms/metadata-editor");

    // Keycloak admin client
    registry.add("keycloak.server-url", keycloak::getAuthServerUrl);
    registry.add("keycloak.realm", () -> "metadata-editor");
    registry.add("keycloak.client-id", () -> "mde");
    registry.add("keycloak.client-secret", () -> "test-secret");

    // CSW server
    registry.add("csw.server", wiremock::getBaseUrl);
  }

  protected String getTokenForUser(String username, String password) {
    String tokenUrl = keycloak.getAuthServerUrl() + "/realms/metadata-editor/protocol/openid-connect/token";

    String token = RestAssured.given()
        .contentType("application/x-www-form-urlencoded")
        .formParam("client_id", "mde")
        .formParam("client_secret", "test-secret")
        .formParam("grant_type", "password")
        .formParam("username", username)
        .formParam("password", password)
        .post(tokenUrl)
        .then()
        .statusCode(200)
        .extract()
        .path("access_token");

    return token;
  }

  protected String extractSubFromToken(String token) {
    String payload = token.split("\\.")[1];
    byte[] decoded = Base64.getUrlDecoder().decode(payload);
    String json = new String(decoded);
    return json.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
  }
}
