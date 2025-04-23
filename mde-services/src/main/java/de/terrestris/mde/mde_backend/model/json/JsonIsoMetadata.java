package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_MaintenanceFrequencyCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonDeserialize(as = JsonIsoMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonIsoMetadata implements FileIdentifier {

  public enum InspireTheme {
    AC,
    AD,
    AF,
    AM,
    AU,
    BR,
    BU,
    CP,
    EF,
    EL,
    ER,
    GE,
    GG,
    GN,
    HB,
    HH,
    HY,
    LC,
    LU,
    MF,
    MR,
    NZ,
    OF,
    OI,
    PD,
    PF,
    PS,
    RS,
    SD,
    SO,
    SR,
    SU,
    TN,
    US
  }

  private MetadataProfile metadataProfile;

  private List<InspireTheme> inspireTheme;

  private String inspireAnnexVersion;

  private List<DistributionVersion> distributionVersions;

  private String fileIdentifier;

  private String identifier;

  @FullTextField()
  @GenericField(name = "title_sort", sortable = Sortable.YES)
  private String title;

  private String description;

  private List<Service> services;

  private Map<String, List<Keyword>> keywords = new HashMap<>();

  private Map<String, Thesaurus> thesauri = new HashMap<>();

  private List<String> highValueDataCategory;

  @JsonFormat(shape = STRING)
  private Instant dateTime;

  @JsonFormat(shape = STRING)
  private Instant created;

  @JsonFormat(shape = STRING)
  private Instant published;

  @GenericField(name = "modified_sort", sortable = Sortable.YES)
  @JsonFormat(shape = STRING)
  private Instant modified;

  @JsonFormat(shape = STRING)
  private Instant validFrom;

  @JsonFormat(shape = STRING)
  private Instant validTo;

  private String url; // rufUrl in import data

  private String capabilities;

  private List<Contact> contacts;

  private List<Contact> pointsOfContact;

  private Integer scale;

  private List<Double> resolutions;

  private String preview;

  private String crs;

  private Extent extent;

  private MD_MaintenanceFrequencyCode maintenanceFrequency = MD_MaintenanceFrequencyCode.unknown;

  private List<ContentDescription> contentDescriptions;

  private String technicalDescription;

  private String contentDescription;

  @IndexedEmbedded
  private List<Lineage> lineage;

  @GenericField
  private boolean valid;

  private List<String> topicCategory;

  private BigInteger termsOfUseId;

  private String termsOfUseSource;

}
