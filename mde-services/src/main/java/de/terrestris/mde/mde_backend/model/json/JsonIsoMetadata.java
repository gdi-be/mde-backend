package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@JsonDeserialize(as = JsonIsoMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonIsoMetadata {

  public enum MetadataProfile {
    ISO,
    INSPIRE
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

  private DataIdentificator dataIdentificator;

  private List<String> serviceIds;

  private List<Service> services;

  private List<String> keywords;

  private Instant created;

  private Instant published;

  private Instant validFrom;

  private List<Source> preview;

  private String url; // rufUrl in import data

  private String capabilities;

  private List<Contact> contacts;

  private int scale;

  private PreviewMap previewMap;

  private PreviewMap previewMapInternal;

  private Extent extent;

}
