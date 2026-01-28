package de.terrestris.mde.mde_backend.model.json;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_MaintenanceFrequencyCode;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_SpatialRepresentationTypeCode;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = JsonIsoMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonIsoMetadata implements CommonFields {

  public enum Privacy {
    NONE,
    CRITICAL_INFRASTRUCTURE,
    PERSONAL_DATA,
    INTERNAL_USE_ONLY
  }

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

  @Nullable private MetadataProfile metadataProfile;

  @Nullable private List<InspireTheme> inspireTheme;

  @Nullable private String inspireAnnexVersion;

  @Nullable private String inspireFormatName;

  @Nullable private List<DistributionVersion> distributionVersions;

  @Nullable private String fileIdentifier;

  @Nullable private String identifier;

  @Nullable
  @FullTextField()
  @GenericField(name = "title_sort", sortable = Sortable.YES)
  private String title;

  @Nullable private String description;

  @Nullable private List<Service> services;

  @Nullable private Map<String, List<Keyword>> keywords = new HashMap<>();

  @Nullable private Map<String, Thesaurus> thesauri = new HashMap<>();

  private boolean highValueDataset = false;

  @Nullable private List<String> highValueDataCategory;

  @Nullable
  @JsonFormat(shape = STRING)
  private Instant dateTime;

  @Nullable
  @JsonFormat(shape = STRING)
  private Instant created;

  @Nullable
  @JsonFormat(shape = STRING)
  private Instant published;

  @Nullable
  @GenericField(name = "modified_sort", sortable = Sortable.YES)
  @JsonFormat(shape = STRING)
  private Instant modified;

  @Nullable
  @JsonFormat(shape = STRING)
  private Instant validFrom;

  @Nullable
  @JsonFormat(shape = STRING)
  private Instant validTo;

  @Nullable private String url;

  @Nullable private String capabilities;

  @Nullable private List<Contact> contacts;

  @Nullable private List<Contact> pointsOfContact;

  @Nullable private Integer scale;

  @Nullable private List<Double> resolutions;

  @Nullable private String preview;

  @Nullable private String crs;

  @Nullable private Extent extent;

  @Nullable
  private MD_MaintenanceFrequencyCode maintenanceFrequency = MD_MaintenanceFrequencyCode.asNeeded;

  @Nullable private List<ContentDescription> contentDescriptions;

  @Nullable private String technicalDescription;

  @Nullable private String contentDescription;

  @Nullable @IndexedEmbedded private List<Lineage> lineage;

  private boolean valid = false;

  @Nullable private List<String> topicCategory;

  @Nullable private Privacy privacy = Privacy.NONE;

  @Nullable private BigInteger termsOfUseId;

  @Nullable private String termsOfUseSource;

  @Nullable private List<Source> dataBases;

  @Nullable private List<MD_SpatialRepresentationTypeCode> spatialRepresentationTypes;
}
