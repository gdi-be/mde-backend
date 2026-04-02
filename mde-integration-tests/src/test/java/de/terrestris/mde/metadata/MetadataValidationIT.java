package de.terrestris.mde.metadata;

import de.terrestris.mde.AbstractApiIT;
import de.terrestris.mde.mde_backend.service.ValidatorService;
import de.terrestris.mde.mde_backend.thread.TrackedTask;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

@DisplayName("Metadata Validation Integration Tests")
class MetadataValidationIT extends AbstractApiIT {

  @Autowired
  private ValidatorService validatorService;

  private String metadataId;
  private String editorToken;
  private String adminToken;

  @BeforeEach
  void setup() {
    adminToken = getTokenForUser("admin-user", "password");
    editorToken = getTokenForUser("editor-user", "password");
  }

  @AfterEach
  void cleanup() {
    if (metadataId != null) {
      if (metadataId != null) {
        waitForValidationToFinish(metadataId);
        given()
            .header("Authorization", "Bearer " + adminToken)
            .delete("/metadata/" + metadataId)
            .then()
            .log().status();
        metadataId = null;
      }
    }
  }

  @Nested
  @DisplayName("Validation endpoint")
  class ValidationEndpointMechanics {

    @Test
    @DisplayName("Authenticated user can start validation — returns metadataId immediately")
    void validationCanBeStarted() {
      metadataId = createMetadata(editorToken);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId + "/validate")
          .then()
          .statusCode(200)
          .body(equalTo(metadataId));

      waitForValidationToFinish(metadataId);
    }

    @Test
    @DisplayName("Unauthenticated validation request is rejected")
    void unauthenticatedValidationIsRejected() {
      metadataId = createMetadata(editorToken);

      given()
          .get("/metadata/" + metadataId + "/validate")
          .then()
          .statusCode(401);
    }

    @Test
    @DisplayName("Starting validation twice for the same running task returns 409")
    void concurrentValidationReturns409() throws InterruptedException {
      metadataId = createMetadata(editorToken);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId + "/validate")
          .then()
          .statusCode(200);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId + "/validate")
          .then()
          .statusCode(409);

      waitForValidationToFinish(metadataId);
    }

    @Test
    @DisplayName("QA user can start validation")
    void qaUserCanStartValidation() {
      String qaToken = getTokenForUser("qa-user", "password");
      metadataId = createMetadata(editorToken);

      given()
          .header("Authorization", "Bearer " + qaToken)
          .get("/metadata/" + metadataId + "/validate")
          .then()
          .statusCode(200)
          .body(equalTo(metadataId));

      waitForValidationToFinish(metadataId);
    }

    @Test
    @DisplayName("Admin user can start validation")
    void adminUserCanStartValidation() {
      metadataId = createMetadata(editorToken);

      given()
          .header("Authorization", "Bearer " + adminToken)
          .get("/metadata/" + metadataId + "/validate")
          .then()
          .statusCode(200)
          .body(equalTo(metadataId));

      waitForValidationToFinish(metadataId);
    }
  }

  @Nested
  @DisplayName("Validation results — ISO")
  class IsoValidationResults {

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("Freshly created metadata has expected validation errors")
    void freshMetadataHasKnownErrors() throws Exception {
      metadataId = createMetadata(editorToken);

      List<String> errors = validatorService.validateMetadata(metadataId);

      assertThat(errors).isNotNull();
      assertThat(errors).anyMatch(e -> e.contains("fileIdentifier"));
      assertThat(errors).anyMatch(e -> e.contains("contact"));
      assertThat(errors).anyMatch(e -> e.contains("dateStamp"));
      assertThat(errors).anyMatch(e -> e.contains("useConstraints"));
      assertThat(errors).anyMatch(e -> e.contains("otherConstraints"));
      assertThat(errors).anyMatch(e -> e.contains("otherRestrictions"));
    }

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("ISO profile produces ISO section header, not INSPIRE header")
    void isoProfileUsesIsoTestSuite() throws Exception {
      metadataId = createMetadata(editorToken);
      patchIso("metadataProfile", "\"ISO\"", editorToken, metadataId);

      List<String> errors = validatorService.validateMetadata(metadataId);

      assertThat(errors).isNotNull();
      assertThat(errors).anyMatch(e -> e.equals("Ergebnisse GDI-DE:"));
      assertThat(errors).noneMatch(e -> e.equals("Ergebnisse GDI-DE für INSPIRE:"));
    }

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("Minimal valid ISO metadata has no unexpected errors")
    void minimalValidIsoMetadataPasses() throws Exception {
      metadataId = createMinimalValidIsoMetadata(editorToken);

      List<String> errors = validatorService.validateMetadata(metadataId);
      System.out.println("TEST METADATA: " + metadataCollectionRepository.findByMetadataId(metadataId).get());
      System.out.println("TEST ERRORS: " + errors);

      assertThat(errors).isNotNull();

      List<String> unexpectedErrors = errors.stream()
          .filter(e -> !e.isBlank())
          .filter(e -> e.startsWith("FEHLER") || e.startsWith("Fehler"))
          .filter(e -> !isKnownBug(e))
          .toList();

      assertThat(unexpectedErrors)
          .as("Unexpected validation errors: %s", unexpectedErrors)
          .isEmpty();
    }

    private static boolean isKnownBug(String error) {
      return error.contains("metadataStandardName") && error.contains("Reihenfolge")
          || error.contains("\"http://www.isotc211.org/2005/gmd\":identifier") && error.contains("Reihenfolge")
          || error.contains("Ressourcenidentifikator")
          || error.contains("apiso-Schema")
          || error.contains("pointOfContact")
          || error.contains("<i>contact</i>");
    }

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("Metadata with missing required fields fails validation")
    void invalidIsoMetadataFailsValidation() throws Exception {
      metadataId = createMinimalValidIsoMetadata(editorToken);

      jdbcTemplate.execute(
          "UPDATE metadata_collection SET iso_metadata = iso_metadata || '{\"contacts\": null}' " +
              "WHERE metadata_id = '" + metadataId + "'");

      List<String> errors = validatorService.validateMetadata(metadataId);

      assertThat(errors).isNotNull();
      assertThat(errors)
          .filteredOn(e -> e.startsWith("FEHLER") || e.startsWith("Fehler"))
          .isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("ISO metadata with bounding box passes validation check for that field")
    void isoMetadataWithBoundingBoxPassesThatCheck() throws Exception {
      metadataId = createMinimalValidIsoMetadata(editorToken);
      patchIso("boundingBox",
          "{\"minLongitude\": 13.0, \"maxLongitude\": 13.8, \"minLatitude\": 52.3, \"maxLatitude\": 52.7}",
          editorToken, metadataId);

      List<String> errors = validatorService.validateMetadata(metadataId);

      assertThat(errors).isNotNull();
      assertThat(errors)
          .noneMatch(e -> e.toLowerCase().contains("bounding") || e.toLowerCase().contains("boundingbox"));
    }
  }

  @Nested
  @DisplayName("Validation results — INSPIRE")
  class InspireValidationResults {

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("INSPIRE profile uses INSPIRE test suite")
    void inspireProfileUsesInspireTestSuite() throws Exception {
      metadataId = createMetadata(editorToken);

      patchIso("metadataProfile", "INSPIRE_HARMONISED", editorToken, metadataId);

      List<String> errors = validatorService.validateMetadata(metadataId);

      assertThat(errors).isNotNull();
      assertThat(errors).anyMatch(e -> e.contains("INSPIRE"));
    }
  }

  @Nested
  @DisplayName("Validation results — special content cases")
  class SpecialContentValidation {

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("Metadata with special characters in title can be validated")
    void metadataWithSpecialCharactersCanBeValidated() throws Exception {
      metadataId = given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("{\"title\": \"Test Ümlä€ts & Spëc!al Çh@r$\"}")
          .post("/metadata/")
          .then()
          .statusCode(200)
          .extract().path("metadataId");

      List<String> errors = validatorService.validateMetadata(metadataId);
      assertThat(errors).isNotNull();
    }

    @Test
    @WithMockUser(roles = "MDEEDITOR")
    @DisplayName("Metadata with temporal extent does not produce temporal-related error")
    void metadataWithTemporalExtentCanBeValidated() throws Exception {
      metadataId = createMinimalValidIsoMetadata(editorToken);
      patchIso("temporalExtent",
          "{\"startDate\": \"2020-01-01\", \"endDate\": \"2024-12-31\"}",
          editorToken, metadataId);

      List<String> errors = validatorService.validateMetadata(metadataId);

      assertThat(errors).isNotNull();
      assertThat(errors)
          .filteredOn(e -> e.startsWith("FEHLER") || e.startsWith("Fehler"))
          .noneMatch(e -> e.toLowerCase().contains("temporal")
              || e.toLowerCase().contains("zeitlich"));
    }
  }

  private String createMinimalValidIsoMetadata(String token) {
    String id = createMetadata(token);
    String ownerId = extractSubFromToken(token);

    jdbcTemplate.execute(
        "DELETE FROM metadata_collection WHERE metadata_id = '" + id + "'");

    jdbcTemplate.execute(
        "INSERT INTO metadata_collection " +
            "(metadata_id, responsible_role, owner_id, assigned_user_id, approved, status, " +
            "created, modified, iso_metadata, client_metadata, technical_metadata, team_member_ids) " +
            "VALUES (" +
            "'" + id + "', " +
            "'MdeEditor', " +
            "'" + ownerId + "', " +
            "null, " +
            "true, " +
            "'IN_EDIT', " +
            "now(), " +
            "now(), " +
            "'{" +
            "\"metadataProfile\": \"ISO\"," +
            "\"title\": \"Test Datensatz\"," +
            "\"description\": \"Testbeschreibung\"," +
            "\"fileIdentifier\": \"" + id + "\"," +
            "\"identifier\": \"" + id + "\"," +
            "\"dateTime\": \"2024-01-01T00:00:00Z\"," +
            "\"pointsOfContact\": [{" +
            "  \"id\": \"00000000-0000-0000-0000-000000000002\"," +
            "  \"organisation\": \"Testorganisation Berlin\"," +
            "  \"name\": \"Test\"," +
            "  \"phone\": \"123456789\"," +
            "  \"email\": \"test@berlin.de\"," +
            "  \"roleCode\": \"pointOfContact\"" +
            "}]," +
            "\"keywords\": {\"default\": [{\"keyword\": \"opendata\"}]}," +
            "\"termsOfUseId\": 1," +
            "\"highValueDataset\": false," +
            "\"crs\": \"http://www.opengis.net/def/crs/EPSG/0/25833\"," +
            "\"maintenanceFrequency\": \"asNeeded\"," +
            "\"valid\": false," +
            "\"privacy\": \"NONE\"" +
            "}'::jsonb, " +
            "'{}', " +
            "'{}', " +
            "'{\"" + ownerId + "\"}'" +
            ")");

    return id;
  }

  private void waitForValidationToFinish(String metadataId) {
    await()
        .atMost(120, TimeUnit.SECONDS)
        .pollInterval(1, TimeUnit.SECONDS)
        .until(() -> executor.getRunningTasks()
            .stream()
            .noneMatch(task -> task instanceof TrackedTask t && t.getTaskId().equals(metadataId)));
  }
}
