package de.terrestris.mde.metadata;

import de.terrestris.mde.AbstractApiIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Codelist API")
class CodelistIT extends AbstractApiIT {

  private String token;

  @BeforeEach
  void setup() {
    token = getTokenForUser("editor-user", "password");
  }

  @Nested
  @DisplayName("INSPIRE Schema Codelist")
  class InspireSchemaCodelist {

    @ParameterizedTest(name = "Theme {0} returns schema {1}")
    @CsvSource({
        "AD, Addresses",
        "AU, AdministrativeUnits",
        "TN, RoadTransport",
        "HY, HydroBase",
        "EL, ElevationBaseTypes",
        "PS, ProtectedSites",
        "GN, GeographicalNames",
        "CP, CadastralParcels",
        "BU, Buildings",
        "SO, Soil"
    })
    @DisplayName("Known INSPIRE theme returns correct schema")
    void knownThemeReturnsSchema(String theme, String expectedSchema) {
      given()
          .header("Authorization", "Bearer " + token)
          .get("/codelists/inspire/schema/" + theme)
          .then()
          .statusCode(200)
          .body("$", hasItem(expectedSchema));
    }

    @Test
    @DisplayName("AD theme returns only Addresses schema")
    void adThemeReturnsAddresses() {
      given()
          .header("Authorization", "Bearer " + token)
          .get("/codelists/inspire/schema/AD")
          .then()
          .statusCode(200)
          .body("$", contains("Addresses"));
    }

    @Test
    @DisplayName("AU theme returns multiple schemas")
    void auThemeReturnsMultipleSchemas() {
      given()
          .header("Authorization", "Bearer " + token)
          .get("/codelists/inspire/schema/AU")
          .then()
          .statusCode(200)
          .body("$", hasItems("AdministrativeUnits", "MaritimeUnits"))
          .body("size()", equalTo(2));
    }

    @Test
    @DisplayName("TN theme returns all transport schemas")
    void tnThemeReturnsAllTransportSchemas() {
      given()
          .header("Authorization", "Bearer " + token)
          .get("/codelists/inspire/schema/TN")
          .then()
          .statusCode(200)
          .body("$", hasItems(
              "RoadTransport",
              "RailTransport",
              "AirTransport",
              "WaterTransport",
              "CommonTransportElements"))
          .body("size()", equalTo(5));
    }

    @Test
    @DisplayName("Invalid theme returns 404")
    void invalidThemeReturns404() {
      given()
          .header("Authorization", "Bearer " + token)
          .get("/codelists/inspire/schema/INVALID")
          .then()
          .statusCode(404);
    }

    @Test
    @DisplayName("Unknown but valid-looking theme returns 404")
    void unknownThemeReturns404() {
      given()
          .header("Authorization", "Bearer " + token)
          .get("/codelists/inspire/schema/XX")
          .then()
          .statusCode(404);
    }

    @Test
    @DisplayName("Lowercase theme returns 404")
    void lowercaseThemeReturns404() {
      given()
          .header("Authorization", "Bearer " + token)
          .get("/codelists/inspire/schema/ad")
          .then()
          .statusCode(404);
    }

    @Test
    @DisplayName("Unauthenticated request is rejected")
    void unauthenticatedRequestIsRejected() {
      given()
          .get("/codelists/inspire/schema/AD")
          .then()
          .statusCode(401);
    }
  }
}
