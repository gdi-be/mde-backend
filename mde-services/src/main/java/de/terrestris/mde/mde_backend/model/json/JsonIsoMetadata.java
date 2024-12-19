package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonDeserialize(as = JsonIsoMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonIsoMetadata {

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

  private List<DistributionVersion> distributionVersions;

  private String fileIdentifier;

  private String identifier;

  private String title;

  private String description;

  private List<Service> services;

  private Map<String, List<Keyword>> keywords = new HashMap<>();

  private Map<String, Thesaurus> thesauri = new HashMap<>();

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

  private Contact pointOfContact;

  private Integer scale;

  private List<Double> resolutions;

  private PreviewMap previewMap;

  private PreviewMap previewMapInternal;

  private String crs;

  private Extent extent;

  private MD_MaintenanceFrequencyCode maintenanceFrequency = MD_MaintenanceFrequencyCode.unknown;

  private List<ContentDescription> contentDescriptions;

  private List<List<Constraint>> resourceConstraints = new ArrayList<>();

  private String lineage;

  private boolean valid;

  private String topicCategory;

}
