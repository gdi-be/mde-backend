package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonDeserialize(as = JsonIsoMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonIsoMetadata {

  public enum MetadataProfile {
    ISO,
    INSPIRE_HARMONISED,
    INSPIRE_IDENTIFIED
  }

  public enum InspireTheme {
    AD,
    AU,
    BU,
    CP,
    EL,
    GE,
    GN,
    LC,
    LU,
    OI,
    PF,
    PS,
    SD,
    SO,
    SU,
    US
  }

  private MetadataProfile metadataProfile;

  private InspireTheme inspireTheme;

  private String fileIdentifier;

  private DataIdentificator dataIdentificator;

  private String title;

  private String description;

  private List<Service> services;

  private List<Keyword> keywords;

  private String highValueDataCategory;

  @JsonFormat(shape = STRING)
  private Instant dateTime;

  @JsonFormat(shape = STRING)
  private Instant created;

  @JsonFormat(shape = STRING)
  private Instant published;

  @JsonFormat(shape = STRING)
  private Instant modified;

  @JsonFormat(shape = STRING)
  private Instant validFrom;

  @JsonFormat(shape = STRING)
  private Instant validTo;

  private List<Source> previews;

  private String url; // rufUrl in import data

  private String capabilities;

  private List<Contact> contacts;

  private Integer scale;

  private Double resolution;

  private PreviewMap previewMap;

  private PreviewMap previewMapInternal;

  private Extent extent;

  private MD_MaintenanceFrequencyCode maintenanceFrequency;

  private List<ContentDescription> contentDescriptions;

}
