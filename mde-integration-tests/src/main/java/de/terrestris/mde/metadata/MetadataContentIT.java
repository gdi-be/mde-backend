package de.terrestris.mde.metadata;

import de.terrestris.mde.AbstractApiIT;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class MetadataContentIT extends AbstractApiIT {

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
      given()
          .header("Authorization", "Bearer " + adminToken)
          .delete("/metadata/" + metadataId)
          .then()
          .statusCode(anyOf(is(200), is(404)));
      metadataId = null;
    }
  }

  @Nested
  @DisplayName("Form Field Updates")
  class FormFieldUpdates {

    @BeforeEach
    void init() {
      metadataId = createMetadata(editorToken);
    }

    @Test
    @DisplayName("Title field can be updated")
    void updateTitle() {

      patchIso("title", "Updated Title", editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .body("isoMetadata.title", equalTo("Updated Title"));
    }

    @Test
    @DisplayName("Form updates are persisted and retrievable")
    void patchUpdatesArePersisted() {

      String newDescription = "Test description for persistence - " + System.currentTimeMillis();

      patchIso("description", newDescription, editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .statusCode(200)
          .body("isoMetadata.description", equalTo(newDescription));
    }

    @Test
    @DisplayName("Multiple form fields can be updated sequentially")
    void multipleFieldUpdatesSequentially() {
      String newTitle = "Updated Title - " + System.currentTimeMillis();
      String newDescription = "Updated Description - " + System.currentTimeMillis();

      patchIso("title", newTitle, editorToken, metadataId);
      patchIso("description", newDescription, editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .statusCode(200)
          .body("isoMetadata.title", equalTo(newTitle))
          .body("isoMetadata.description", equalTo(newDescription));
    }

    @Test
    @DisplayName("Metadata profile field can be updated")
    void canUpdateMetadataProfile() {
      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "ISO",
                "key": "metadataProfile",
                "value": "INSPIRE_IDENTIFIED"
              }
              """)
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(200)
          .body("isoMetadata.metadataProfile", equalTo("INSPIRE_IDENTIFIED"));
    }

    @Test
    @DisplayName("Metadata extent field can be updated")
    void canUpdateExtent() {
      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "ISO",
                "key": "extent",
                "value": {
                  "minx": 13.088,
                  "maxx": 13.761,
                  "miny": 52.338,
                  "maxy": 52.675
                }
              }
              """)
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(200)
          .body("isoMetadata.extent.minx", equalTo(13.088f))
          .body("isoMetadata.extent.maxx", equalTo(13.761f));
    }

    @Test
    @DisplayName("Metadata topic category field can be updated")
    void updateTopicCategory() {

      patchIso("topicCategory", List.of("location"), editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .body("isoMetadata.topicCategory", equalTo(List.of("location")));
    }

    @Test
    @DisplayName("CRS field can be updated")
    void updateCrs() {

      patchIso("crs", "EPSG:4326", editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .body("isoMetadata.crs", equalTo("EPSG:4326"));
    }

    @Test
    @DisplayName("Terms of use can be updated")
    void updateTermsOfUse() {

      patchIso("termsOfUseId", 1, editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .statusCode(200)
          .body("isoMetadata.termsOfUseId", equalTo(1));
    }
  }

  @Nested
  @DisplayName("INSPIRE Fields Updates")
  class InspireTests {

    @BeforeEach
    void init() {
      metadataId = createMetadata(editorToken);
    }

    @Test
    @DisplayName("Inspire theme field can be updated")
    void updateInspireTheme() {

      patchIso("inspireTheme", "ADDRESSES", editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .statusCode(200);
    }
  }

  @Nested
  @DisplayName("Service Fields Updates")
  class ServiceTests {

    @BeforeEach
    void init() {
      metadataId = createMetadata(editorToken);
    }

    @Test
    @DisplayName("Can add service")
    void addService() {
      Map<String, Object> service = Map.of(
          "id", UUID.randomUUID().toString(),
          "workspace", "workspace1",
          "title", "123 Datentest",
          "shortDescription", "dewf",
          "serviceIdentification", "00e19242-8d3c-4617-ad73-d5a6b79ae55f",
          "serviceType", "WMS",
          "preview", "preview.png",
          "legendImage", Map.of(
              "format", "wefwef",
              "url", "weffwe",
              "width", 234,
              "height", 235));

      patchIso("services", List.of(service), editorToken, metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId)
          .then()
          .body("isoMetadata.services.size()", greaterThan(0));
    }

    @Test
    @DisplayName("Cannot add service with invalid serviceType")
    void cannotAddInvalidServiceType() {

      Map<String, Object> service = Map.of(
          "id", UUID.randomUUID().toString(),
          "workspace", "workspace1",
          "title", "Invalid Service",
          "shortDescription", "test",
          "serviceIdentification", "00e19242-8d3c-4617-ad73-d5a6b79ae55f",
          "serviceType", "INVALID_TYPE",
          "preview", "preview.png");

      Map<String, Object> body = Map.of(
          "type", "ISO",
          "key", "services",
          "value", List.of(service));

      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body(body)
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(500);
    }
  }

  @Nested
  @DisplayName("Client Metadata Updates")
  class ClientMetadataTests {

    @BeforeEach
    void init() {
      metadataId = createMetadata(editorToken);
    }

    @Test
    @DisplayName("Client metadata (layer) can be added and retrieved")
    void updateClientMetadata() {

      String serviceId = "00e19242-8d3c-4617-ad73-d5a6b79ae55f";

      Map<String, Object> layer = Map.of(
          "id", UUID.randomUUID().toString(),
          "name", "layer1",
          "title", "Test Title",
          "styleName", "dewf",
          "styleTitle", "Test Style",
          "shortDescription", "This is a test Layer",
          "legendImage", "testImage.png",
          "datasource", "testDatasource",
          "secondaryDatasource", "testSecondaryDatasource");

      Map<String, Object> layers = Map.of(
          serviceId, List.of(layer));

      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body(Map.of(
              "type", "CLIENT",
              "key", "layers",
              "value", layers))
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(200)
          .body("clientMetadata.layers." + serviceId + "[0].name", equalTo("layer1"))
          .body("clientMetadata.layers." + serviceId + "[0].title", equalTo("Test Title"));
    }
  }

  @Nested
  @DisplayName("Form Authorization and Access Control")
  class TecchnicalMetadataTests {

    @Test
    @DisplayName("06 Update technical metadata")
    void updateTechnicalMetadata() {

      metadataId = createMetadata(editorToken);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "TECHNICAL",
                "key": "status",
                "value": "DRAFT"
              }
              """)
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(200);
    }
  }

  @Nested
  @DisplayName("Form Authorization and Access Control")
  class FormAuthorizationTests {

    @Test
    @DisplayName("Unauthenticated request to PATCH is rejected")
    void unauthenticatedPatchIsRejected() {
      metadataId = createMetadata(editorToken);

      given()
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "ISO",
                "key": "title",
                "value": "Unauthorized Update"
              }
              """)
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(401);
    }

    @Test
    @DisplayName("Editor can update their own metadata")
    void editorCanUpdateOwnMetadata() {
      metadataId = createMetadata(editorToken);

      String newTitle = "Editor Updated - " + System.currentTimeMillis();

      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body(String.format("""
              {
                "type": "ISO",
                "key": "title",
                "value": "%s"
              }
              """, newTitle))
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(200);
    }
  }

  @Nested
  @DisplayName("Form Validation and Error Handling")
  class FormValidationTests {

    @Test
    @DisplayName("PATCH with invalid metadata type is rejected")
    void invalidMetadataTypeIsRejected() {
      metadataId = createMetadata(editorToken);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "INVALID_TYPE",
                "key": "title",
                "value": "Test"
              }
              """)
          .patch("/metadata/" + metadataId)
          .then()
          .statusCode(400);
    }

    @Test
    @DisplayName("PATCH to non-existent metadata returns error")
    void patchNonExistentMetadataReturnsError() {
      String nonExistentId = "non-existent-" + System.currentTimeMillis();

      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "ISO",
                "key": "title",
                "value": "Test"
              }
              """)
          .patch("/metadata/" + nonExistentId)
          .then()
          .statusCode(500);
    }
  }

  @Nested
  @DisplayName("Codelist endpoint")
  class CodelistTests {

    @Test
    @DisplayName("Codelist endpoint returns expected values")
    void codelistReturnsInspireSchemas() {
      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/codelists/inspire/schema/AD")
          .then()
          .statusCode(200)
          .body("$", hasItem("Addresses"));
    }

    @Test
    @DisplayName("Codelist endpoint returns 404 for invalid theme")
    void codelistReturnsNotFoundForInvalidTheme() {
      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/codelists/inspire/schema/INVALID")
          .then()
          .statusCode(404);
    }
  }

  @Nested
  @DisplayName("Form Update Conflicts")
  class FormUpdateConflicts {

    @Test
    @DisplayName("Duplicate title update is rejected with CONFLICT")
    void duplicateTitleIsRejected() {
      String title1 = "Unique Title - " + System.currentTimeMillis();
      metadataId = given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("{\"title\": \"" + title1 + "\"}")
          .post("/metadata/")
          .then()
          .statusCode(200)
          .extract().path("metadataId");

      String metadataId2 = given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("{\"title\": \"Second Metadata\"}")
          .post("/metadata/")
          .then()
          .statusCode(200)
          .extract().path("metadataId");

      try {
        given()
            .header("Authorization", "Bearer " + editorToken)
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                  "type": "ISO",
                  "key": "title",
                  "value": "%s"
                }
                """, title1))
            .patch("/metadata/" + metadataId2)
            .then()
            .statusCode(409);
      } finally {
        given()
            .header("Authorization", "Bearer " + adminToken)
            .delete("/metadata/" + metadataId2)
            .then()
            .log().status();
      }
    }
  }

  @Nested
  @DisplayName("Download Operations")
  class DownloadAndCloneTests {

    @Test
    @DisplayName("Can download metadata as ZIP")
    void canDownloadMetadataAsZip() {

      metadataId = createMetadata(editorToken);
     
      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "ISO",
                "key": "metadataProfile",
                "value": "ISO"
              }
              """)
          .patch("/metadata/" + metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .contentType(ContentType.JSON)
          .body("""
              {
                "type": "ISO",
                "key": "description",
                "value": "Test description"
              }
              """)
          .patch("/metadata/" + metadataId);

      given()
          .header("Authorization", "Bearer " + editorToken)
          .get("/metadata/" + metadataId + "/download")
          .then()
          .statusCode(200)
          .contentType("application/octet-stream");
    }
  }
}
