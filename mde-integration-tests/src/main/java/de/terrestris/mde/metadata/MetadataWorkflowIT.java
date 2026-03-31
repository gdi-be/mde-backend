package de.terrestris.mde.metadata;

import de.terrestris.mde.AbstractApiIT;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class MetadataWorkflowIT extends AbstractApiIT {

  private String metadataId;
  private String adminToken;
  private String editorToken;
  private String qaToken;
  private String ownerToken;

  @BeforeEach
  void setup() {
    adminToken = getTokenForUser("admin-user", "password");
    editorToken = getTokenForUser("editor-user", "password");
    qaToken = getTokenForUser("qa-user", "password");
    ownerToken = getTokenForUser("owner-user", "password");
  }

  @AfterEach
  void cleanup() {
    if (metadataId != null) {
      given()
          .header("Authorization", "Bearer " + adminToken)
          .delete("/metadata/" + metadataId)
          .then()
          .log().status();
      metadataId = null;
    }
  }

  @Test
  @DisplayName("Data owner can create metadata")
  void dataOwnerCanCreateMetadata() {
    metadataId = given()
        .header("Authorization", "Bearer " + ownerToken)
        .contentType(ContentType.JSON)
        .body("{\"title\": \"Workflow Test " + System.currentTimeMillis() + "\"}")
        .post("/metadata/")
        .then()
        .statusCode(200)
        .body("metadataId", notNullValue())
        .extract().path("metadataId");
  }

  @Test
  @DisplayName("Full workflow: create -> assign -> approve -> publish")
  void fullWorkflow_createAssignApprovePublish() {
    // Step 1: Create
    String title = "Workflow Test " + System.currentTimeMillis();
    metadataId = given()
        .header("Authorization", "Bearer " + ownerToken)
        .contentType(ContentType.JSON)
        .body("{\"title\": \"" + title + "\"}")
        .post("/metadata/")
        .then()
        .statusCode(200)
        .body("metadataId", notNullValue())
        .body("title", equalTo(title))
        .extract().path("metadataId");

    // Step 2: Assign to editor
    String editorId = extractSubFromToken(editorToken);
    given()
        .header("Authorization", "Bearer " + ownerToken)
        .contentType(ContentType.JSON)
        .body("\"" + editorId + "\"")
        .post("/metadata/" + metadataId + "/assignUser")
        .then()
        .statusCode(200);

    // Step 3: Assign MdeEditor role
    given()
        .header("Authorization", "Bearer " + ownerToken)
        .contentType(ContentType.JSON)
        .body("{\"role\": \"MdeEditor\"}")
        .post("/metadata/" + metadataId + "/assignRole")
        .then()
        .statusCode(200);

    // Step 4: Approve (as QA)
    given()
        .header("Authorization", "Bearer " + qaToken)
        .post("/metadata/" + metadataId + "/approved")
        .then()
        .statusCode(200);

    // Step 5: Publish (as editor)
    given()
        .header("Authorization", "Bearer " + editorToken)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(200)
        .body("publishedCatalogRecords", hasSize(greaterThanOrEqualTo(1)));

    // After publish: assigned user and role should be cleared
    given()
        .header("Authorization", "Bearer " + adminToken)
        .get("/metadata/" + metadataId)
        .then()
        .statusCode(200)
        .body("assignedUserId", nullValue())
        .body("responsibleRole", nullValue());
  }

  @Test
  @DisplayName("Unapproved metadata cannot be published")
  void publishFailsIfNotApproved() {
    metadataId = given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType(ContentType.JSON)
        .body("{\"title\": \"Unapproved Test " + System.currentTimeMillis() + "\"}")
        .post("/metadata/")
        .then()
        .statusCode(200)
        .extract().path("metadataId");

    given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType(ContentType.JSON)
        .body("{\"role\": \"MdeEditor\"}")
        .post("/metadata/" + metadataId + "/assignRole")
        .then()
        .statusCode(200);

    // No approval step — publish should be rejected
    given()
        .header("Authorization", "Bearer " + editorToken)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(409);
  }

  @Test
  @DisplayName("Metadata without assigned role cannot be published")
  void publishFailsIfRoleNotAssigned() {
    metadataId = given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType(ContentType.JSON)
        .body("{\"title\": \"No Role Test " + System.currentTimeMillis() + "\"}")
        .post("/metadata/")
        .then()
        .statusCode(200)
        .extract().path("metadataId");

    given()
        .header("Authorization", "Bearer " + qaToken)
        .post("/metadata/" + metadataId + "/approved")
        .then()
        .statusCode(200);

    given()
        .header("Authorization", "Bearer " + editorToken)
        .post("/metadata/" + metadataId + "/publish")
        .then()
        .statusCode(409);
  }

  @Test
  @DisplayName("Editor cannot delete metadata assigned to other user")
  void editorCannotDeleteMetadataAssignedToOtherUser() {
    metadataId = given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType(ContentType.JSON)
        .body("{\"title\": \"Delete Test " + System.currentTimeMillis() + "\"}")
        .post("/metadata/")
        .then()
        .statusCode(200)
        .extract().path("metadataId");

    String editorId = extractSubFromToken(editorToken);
    given()
        .header("Authorization", "Bearer " + adminToken)
        .contentType(ContentType.JSON)
        .body("\"" + editorId + "\"")
        .post("/metadata/" + metadataId + "/assignUser")
        .then()
        .statusCode(200);

    given()
        .header("Authorization", "Bearer " + qaToken)
        .delete("/metadata/" + metadataId)
        .then()
        .statusCode(403);
  }
}
