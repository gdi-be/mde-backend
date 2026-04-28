package de.terrestris.mde.metadata;

import de.terrestris.mde.AbstractApiIT;
import de.terrestris.mde.mde_backend.service.GeneratorUtils;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XML Generation Integration Tests")
class XmlGenerationIT extends AbstractApiIT {

  private String token;
  private String metadataId;

  @BeforeEach
  void setup() {
    token = getTokenForUser("editor-user", "password");
    metadataId = given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"title\": \"XML Generation Test " + System.currentTimeMillis() + "\"}")
        .post("/metadata/")
        .then()
        .statusCode(200)
        .extract().path("metadataId");
  }

  @AfterEach
  void cleanup() {
    given()
        .header("Authorization", "Bearer " + token)
        .delete("/metadata/" + metadataId);
  }

  @Test
  @DisplayName("Generated XML contains German language code")
  void xmlContainsGermanLanguageCode() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("LanguageCode")
        .contains("codeListValue=\"ger\"");
  }

  @Test
  @DisplayName("Generated XML contains UTF-8 character set")
  void xmlContainsUtf8CharacterSet() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("MD_CharacterSetCode")
        .contains("codeListValue=\"utf8\"");
  }

  @Test
  @DisplayName("Generated XML contains dataset hierarchy level")
  void xmlContainsDatasetHierarchyLevel() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("MD_ScopeCode")
        .contains("codeListValue=\"dataset\"");
  }

  @Test
  @DisplayName("Generated XML contains default contact email")
  void xmlContainsDefaultContactEmail() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains(GeneratorUtils.DEFAULT_CONTACT.getEmail());
  }

  @Test
  @DisplayName("Generated XML contains default contact organisation")
  void xmlContainsDefaultContactOrganisation() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains(GeneratorUtils.DEFAULT_CONTACT.getOrganisation());
  }

  @Test
  @DisplayName("Generated XML contains CI_ResponsibleParty element")
  void xmlContainsResponsibleParty() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains("CI_ResponsibleParty");
  }

  @Test
  @DisplayName("Generated XML contains profile name from metadata variables")
  void xmlContainsProfileName() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains(GeneratorUtils.METADATA_VARIABLES.getProfileName());
  }

  @Test
  @DisplayName("Generated XML contains profile version from metadata variables")
  void xmlContainsProfileVersion() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains(GeneratorUtils.METADATA_VARIABLES.getProfileVersion());
  }

  @Test
  @DisplayName("Generated XML contains default CRS")
  void xmlContainsDefaultCrs() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("referenceSystemInfo")
        .contains("http://www.opengis.net/def/crs/EPSG/0/25833");
  }

  @Test
  @DisplayName("Generated XML omits referenceSystemInfo when CRS is null")
  void xmlOmitsReferenceSystemInfoWhenCrsIsNull() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"crs\", \"value\": null}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).doesNotContain("referenceSystemInfo");
  }

  @Test
  @DisplayName("Generated XML contains creation date when set")
  void xmlContainsCreationDateWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"created\", \"value\": \"2026-01-15T00:00:00Z\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("2026-01-15")
        .contains("codeListValue=\"creation\"");
  }

  @Test
  @DisplayName("Generated XML contains publication date when set")
  void xmlContainsPublicationDateWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"published\", \"value\": \"2026-06-01T00:00:00Z\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("2026-06-01")
        .contains("codeListValue=\"publication\"");
  }

  @Test
  @DisplayName("Generated XML contains revision date when set")
  void xmlContainsRevisionDateWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"modified\", \"value\": \"2026-09-01T00:00:00Z\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("2026-09-01")
        .contains("codeListValue=\"revision\"");
  }

  @Test
  @DisplayName("Generated XML omits creation date when not set")
  void xmlOmitsCreationDateWhenNotSet() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).doesNotContain("codeListValue=\"creation\"");
  }

  @Test
  @DisplayName("Generated XML always contains regional keyword")
  void xmlAlwaysContainsRegionalKeyword() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("SpatialScope/regional")
        .contains("Regional");
  }

  @Test
  @DisplayName("Generated XML contains spatial scope thesaurus")
  void xmlContainsSpatialScopeThesaurus() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("inspire.ec.europa.eu/metadata-codelist/SpatialScope")
        .contains("Räumlicher Anwendungsbereich");
  }

  @Test
  @DisplayName("Generated XML contains maintenance info when frequency is set")
  void xmlContainsMaintenanceInfoWhenFrequencyIsSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"maintenanceFrequency\", \"value\": \"asNeeded\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("MD_MaintenanceInformation")
        .contains("codeListValue=\"asNeeded\"");
  }

  @Test
  @DisplayName("Generated XML contains keyword type code")
  void xmlContainsKeywordTypeCode() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("MD_KeywordTypeCode")
        .contains("codeListValue=\"theme\"");
  }

  @Test
  @DisplayName("Generated XML contains HVD keyword when highValueDataset is true")
  void xmlContainsHvdKeywordWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"highValueDataset\", \"value\": true}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"highValueDataCategory\", \"value\": [\"Georaum\"]}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("HVD-Kategorien")
        .contains("Georaum")
        .contains("data.europa.eu/bna/c_ac64a52d");
  }

  @Test
  @DisplayName("Generated XML omits HVD keyword when highValueDataset is false")
  void xmlOmitsHvdKeywordWhenNotSet() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).doesNotContain("HVD-Kategorien");
  }

  @Test
  @DisplayName("Generated XML contains INSPIRE theme keywords when profile is not ISO")
  void xmlContainsInspireThemeKeywordsForNonIsoProfile() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"metadataProfile\", \"value\": \"INSPIRE_IDENTIFIED\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"inspireTheme\", \"value\": [\"AD\"]}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("GEMET - INSPIRE themes, version 1.0")
        .contains("Adressen");
  }

  @Test
  @DisplayName("Generated XML omits INSPIRE theme keywords for ISO profile")
  void xmlOmitsInspireThemeKeywordsForIsoProfile() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).doesNotContain("GEMET - INSPIRE themes, version 1.0");
  }

  @Test
  @DisplayName("Generated XML contains spatial representation type when set")
  void xmlContainsSpatialRepresentationTypeWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"spatialRepresentationTypes\", \"value\": [\"vector\"]}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("spatialRepresentationType")
        .contains("codeListValue=\"vector\"");
  }

  @Test
  @DisplayName("Generated XML contains scale when set")
  void xmlContainsScaleWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"scale\", \"value\": 25000}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("equivalentScale")
        .contains("25000");
  }

  @Test
  @DisplayName("Generated XML contains resolution in metres when set")
  void xmlContainsResolutionInMetresWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"resolutions\", \"value\": [30.0]}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("Distance")
        .contains("uom=\"metres\"")
        .contains("30.0");
  }

  @Test
  @DisplayName("Generated XML contains topic category when set")
  void xmlContainsTopicCategoryWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"topicCategory\", \"value\": [\"location\"]}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("topicCategory")
        .contains("MD_TopicCategoryCode")
        .contains("location");
  }

  @Test
  @DisplayName("Generated XML contains bounding box when extent is set")
  void xmlContainsBoundingBoxWhenExtentIsSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("""
            {"type": "ISO", "key": "extent", "value": {
                "minx": 13.088, "maxx": 13.761,
                "miny": 52.338, "maxy": 52.675
            }}
            """)
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("EX_GeographicBoundingBox")
        .contains("13.088")
        .contains("13.761")
        .contains("52.338")
        .contains("52.675");
  }

  @Test
  @DisplayName("Generated XML contains regional key from metadata variables")
  void xmlContainsRegionalKey() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("""
            {"type": "ISO", "key": "extent", "value": {
                "minx": 13.088, "maxx": 13.761,
                "miny": 52.338, "maxy": 52.675
            }}
            """)
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("EX_GeographicDescription")
        .contains(GeneratorUtils.METADATA_VARIABLES.getRegionalKey());
  }

  @Test
  @DisplayName("Generated XML omits fileIdentifier when not yet published")
  void xmlOmitsFileIdentifierBeforePublication() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).doesNotContain("fileIdentifier");
  }

  @Test
  @DisplayName("Generated XML contains lineage when set")
  void xmlContainsLineageWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("""
            {"type": "ISO", "key": "lineage", "value": [
                {"title": "Test Lineage Source", "identifier": "test-id-123"}
            ]}
            """)
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("LI_Lineage")
        .contains("Test Lineage Source")
        .contains("test-id-123");
  }

  @Test
  @DisplayName("Generated XML contains INSPIRE conformance report for non-ISO profile")
  void xmlContainsInspireConformanceReportForNonIsoProfile() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"metadataProfile\", \"value\": \"INSPIRE_IDENTIFIED\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("DQ_DomainConsistency")
        .contains("data.europa.eu/eli/reg/2010/1089")
        .contains("1089/2010");
  }

  @Test
  @DisplayName("Generated XML omits conformance report for ISO profile")
  void xmlOmitsConformanceReportForIsoProfile() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).doesNotContain("DQ_DomainConsistency");
  }

  @Test
  @DisplayName("Generated XML marks INSPIRE_HARMONISED as pass=true in conformance report")
  void xmlMarksHarmonisedAsPass() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"metadataProfile\", \"value\": \"INSPIRE_HARMONISED\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains("<gco:Boolean>true</gco:Boolean>");
  }

  @Test
  @DisplayName("Generated XML marks INSPIRE_IDENTIFIED as pass=false in conformance report")
  void xmlMarksIdentifiedAsFail() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"metadataProfile\", \"value\": \"INSPIRE_IDENTIFIED\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains("<gco:Boolean>false</gco:Boolean>");
  }

  @Test
  @DisplayName("Generated XML contains description when set")
  void xmlContainsDescriptionWhenSet() throws Exception {
    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body("{\"type\": \"ISO\", \"key\": \"description\", \"value\": \"Test abstract text\"}")
        .patch("/metadata/" + metadataId)
        .then().statusCode(200);

    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml).contains("Test abstract text");
  }

  @Test
  @DisplayName("Generated XML contains standard format from metadata variables")
  void xmlContainsStandardFormat() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains("MD_Distribution")
        .contains(GeneratorUtils.METADATA_VARIABLES.getStandardFormat());
  }

  @Test
  @DisplayName("Generated XML contains standard version from metadata variables")
  void xmlContainsStandardVersion() throws Exception {
    String xml = downloadDatasetXml(token, metadataId);
    assertThat(xml)
        .contains(GeneratorUtils.METADATA_VARIABLES.getStandardVersion());
  }

  @Test
  @DisplayName("Download produces valid zip")
  void downloadProducesValidZip() {
    byte[] zip = given()
        .header("Authorization", "Bearer " + token)
        .get("/metadata/" + metadataId + "/download")
        .then()
        .statusCode(200)
        .extract().asByteArray();

    assertThat(zip).isNotEmpty();
    assertThat(zip[0]).isEqualTo((byte) 0x50);
    assertThat(zip[1]).isEqualTo((byte) 0x4B);
  }

  private String downloadDatasetXml(String token, String metadataId) throws Exception {
    byte[] zip = given()
        .header("Authorization", "Bearer " + token)
        .get("/metadata/" + metadataId + "/download")
        .then()
        .statusCode(200)
        .extract().asByteArray();

    var zipStream = new ZipInputStream(new ByteArrayInputStream(zip));
    var entry = zipStream.getNextEntry();
    while (entry != null) {
      if (entry.getName().startsWith("dataset_")) {
        return new String(zipStream.readAllBytes(), StandardCharsets.UTF_8);
      }
      entry = zipStream.getNextEntry();
    }
    throw new IllegalStateException("No dataset XML found in zip");
  }
}
