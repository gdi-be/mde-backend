package de.terrestris.mde.metadata;

import de.terrestris.mde.AbstractApiIT;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Metadata API Integration Tests")
class MetadataApiIT extends AbstractApiIT {

  private String metadataId;

  @BeforeEach
  void insertTestData() {
    metadataId = "test-metadata-uuid-1234";
    jdbcTemplate.execute("DELETE FROM metadata_collection WHERE metadata_id = 'test-metadata-uuid-1234'");
    jdbcTemplate.execute(
        "INSERT INTO metadata_collection " +
            "(metadata_id, responsible_role, owner_id, assigned_user_id, approved, status, created, modified, iso_metadata, client_metadata, technical_metadata, team_member_ids) "
            +
            "VALUES " +
            "('test-metadata-uuid-1234', 'MdeEditor', 'test-owner-id', null, true, 'IN_EDIT', now(), now(), "
            +
            "'{\"title\": \"Test Metadata\", \"metadataProfile\": \"ISO\"}', '{}', '{}', '{}')");
  }

  @AfterEach
  void cleanup() {
    jdbcTemplate.execute("DELETE FROM metadata_collection WHERE metadata_id = 'test-metadata-uuid-1234'");
  }

  @Test
  @DisplayName("Can not delete metadata without authentication")
  void unauthenticatedRequestIsRejected() {
    given()
        .delete("/metadata/" + metadataId)
        .then()
        .statusCode(401);
  }

  @Test
  @DisplayName("Non-editor cannot publish metadata")
  void nonEditorCannotPublish() {
    String token = getTokenForUser("qa-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(500);
  }

  @Test
  @DisplayName("Editor can publish metadata")
  void editorCanPublish() {
    String token = getTokenForUser("editor-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(200)
        .body("publishedCatalogRecords", hasSize(greaterThanOrEqualTo(1)));

    given()
        .header("Authorization", "Bearer " + token)
        .get("/metadata/" + metadataId)
        .then()
        .body("status", equalTo("PUBLISHED"));
  }

  @Test
  @DisplayName("Admin can publish metadata")
  void adminCanPublish() {
    String token = getTokenForUser("admin-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(200)
        .body("publishedCatalogRecords", hasSize(greaterThanOrEqualTo(1)));

    given()
        .header("Authorization", "Bearer " + token)
        .get("/metadata/" + metadataId)
        .then()
        .body("status", equalTo("PUBLISHED"));
  }

  @Test
  @DisplayName("Unapproved metadata cannot be published")
  void unapprovedMetadataCannotBePublished() {
    jdbcTemplate.execute(
        "UPDATE metadata_collection SET approved = false WHERE metadata_id = 'test-metadata-uuid-1234'");
    String token = getTokenForUser("editor-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(409);
  }

  @Test
  @DisplayName("Metadata without editor role cannot be published")
  void metadataWithoutEditorRoleCannotBePublished() {
    jdbcTemplate.execute(
        "UPDATE metadata_collection SET responsible_role = null WHERE metadata_id = 'test-metadata-uuid-1234'");
    String token = getTokenForUser("editor-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(409);
  }

  @Test
  @DisplayName("Fetching metadata by ID works")
  void canFetchMetadataById() {
    String token = getTokenForUser("editor-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .get("/metadata/" + metadataId)
        .then()
        .statusCode(200)
        .body("metadataId", equalTo(metadataId));
  }

  @Test
  @DisplayName("Fetching unknown metadata ID returns 404")
  void fetchingUnknownIdReturns404() {
    String token = getTokenForUser("editor-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .get("/metadata/does-not-exist")
        .then()
        .statusCode(404);
  }

  @Test
  @DisplayName("QA user can approve metadata")
  void canApproveMetadata() {
    String token = getTokenForUser("qa-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .post("/metadata/" + metadataId + "/approved")
        .then()
        .statusCode(200);
  }

  @Test
  @DisplayName("QA user can disapprove metadata")
  void canDisapproveMetadata() {
    String token = getTokenForUser("qa-user", "password");
    given()
        .header("Authorization", "Bearer " + token)
        .delete("/metadata/" + metadataId + "/approved")
        .then()
        .statusCode(200);
  }
}
