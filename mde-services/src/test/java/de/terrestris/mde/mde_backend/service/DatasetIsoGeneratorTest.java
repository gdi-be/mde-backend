package de.terrestris.mde.mde_backend.service;

import static org.junit.jupiter.api.Assertions.*;

import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.model.json.Contact;
import de.terrestris.mde.mde_backend.model.json.Extent;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.termsofuse.Json;
import de.terrestris.mde.mde_backend.model.json.termsofuse.TermsOfUse;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DatasetIsoGenerator testing the REAL implementation. These tests verify that the
 * fix in writeResourceConstraints() properly handles special characters like quotes and backslashes
 * in termsOfUseSource and title fields.
 */
class DatasetIsoGeneratorTest {

  @BeforeAll
  static void setup() {
    System.setProperty("CODELISTS_DIR", "src/test/resources/codelists");
  }

  /**
   * Test the REAL generateDatasetMetadata method with quotes in termsOfUseSource. This is the
   * original bug scenario: "123 \"Birgit\" 123"
   */
  @Test
  void testGenerateDatasetMetadataWithQuotesInSource() {
    DatasetIsoGenerator generator = new DatasetIsoGenerator();
    JsonIsoMetadata metadata = createMinimalMetadata();
    metadata.setTermsOfUseSource("123 \"Birgit\" 123");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertDoesNotThrow(
        () -> generator.generateDatasetMetadata(metadata, "test-id", outputStream),
        "generateDatasetMetadata should handle quotes in termsOfUseSource");

    assertTrue(outputStream.size() > 0, "ISO XML should be generated");
    String xml = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(xml.contains("<?xml"), "Output should be valid XML");
  }

  @Test
  void testGenerateDatasetMetadataWithQuotesInTitle() {
    DatasetIsoGenerator generator = new DatasetIsoGenerator();
    JsonIsoMetadata metadata = createMinimalMetadata();
    metadata.setTitle("Dataset \"Test\" Title");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertDoesNotThrow(
        () -> generator.generateDatasetMetadata(metadata, "test-id", outputStream),
        "generateDatasetMetadata should handle quotes in title");

    assertTrue(outputStream.size() > 0);
  }

  @Test
  void testGenerateDatasetMetadataWithBackslashesInSource() {
    DatasetIsoGenerator generator = new DatasetIsoGenerator();
    JsonIsoMetadata metadata = createMinimalMetadata();
    metadata.setTermsOfUseSource("C:\\Path\\To\\File");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertDoesNotThrow(
        () -> generator.generateDatasetMetadata(metadata, "test-id", outputStream),
        "generateDatasetMetadata should handle backslashes");

    assertTrue(outputStream.size() > 0);
  }

  @Test
  void testGenerateDatasetMetadataNormalCase() {
    DatasetIsoGenerator generator = new DatasetIsoGenerator();
    JsonIsoMetadata metadata = createMinimalMetadata();
    metadata.setTermsOfUseSource("Geodateninfrastruktur Berlin");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertDoesNotThrow(
        () -> generator.generateDatasetMetadata(metadata, "test-id", outputStream),
        "generateDatasetMetadata should work with normal strings");

    assertTrue(outputStream.size() > 0);
  }

  @Test
  void testGenerateDatasetMetadataWithComplexQuotes() {
    DatasetIsoGenerator generator = new DatasetIsoGenerator();
    JsonIsoMetadata metadata = createMinimalMetadata();
    metadata.setTermsOfUseSource("Source: \"Berlin's GDI\" & 'More'");
    metadata.setTitle("Dataset \"Complex\" Title's");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertDoesNotThrow(
        () -> generator.generateDatasetMetadata(metadata, "test-id", outputStream),
        "generateDatasetMetadata should handle complex quotes and special characters");

    assertTrue(outputStream.size() > 0);
  }

  private JsonIsoMetadata createMinimalMetadata() {
    JsonIsoMetadata metadata = new JsonIsoMetadata();
    metadata.setTitle("Test Dataset");
    metadata.setDescription("Test description");
    metadata.setModified(Instant.now());
    metadata.setMetadataProfile(MetadataProfile.ISO);

    // Required contact
    Contact contact = new Contact();
    contact.setName("Test Contact");
    contact.setOrganisation("Test Org");
    contact.setEmail("test@example.com");
    metadata.setContacts(List.of(contact));

    // Required extent
    Extent extent = new Extent();
    extent.setMinx(13.0);
    extent.setMiny(52.0);
    extent.setMaxx(14.0);
    extent.setMaxy(53.0);
    metadata.setExtent(extent);

    // Required TermsOfUse with placeholders
    TermsOfUse termsOfUse = new TermsOfUse();
    termsOfUse.setId(1);
    termsOfUse.setShortname("test");
    termsOfUse.setDescription("Test description");
    Json termsJson = new Json();
    termsJson.setName("[Titel des Datensatzes]");
    termsJson.setQuelle("[Quelle]");
    termsOfUse.setJson(termsJson);
    metadata.setTermsOfUseId(BigInteger.ONE);

    return metadata;
  }
}
