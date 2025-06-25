package de.terrestris.mde.mde_backend.model.json;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = Service.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Service implements FileIdentifier {

  public enum ServiceType {
    WFS,
    WMS,
    ATOM,
    WMTS
  }

  @Nullable private String workspace;

  @Nullable private String title;

  @Nullable private String shortDescription;

  @Nullable private String contentDescription;

  @Nullable private String technicalDescription;

  @Nullable private String fileIdentifier;

  @Nullable private String serviceIdentification;

  @Nullable private ServiceType serviceType;

  // imported from service connectPoint
  @Nullable private String url;

  // imported from CapabilitiesUrl in service files (non ISO section)
  @Nullable private String capabilitiesUrl;

  @Nullable private List<ServiceDescription> serviceDescriptions;

  @Nullable private LegendImage legendImage;

  @Nullable private List<Source> publications;

  @JsonFormat(shape = STRING)
  @Nullable
  private Instant created;

  @JsonFormat(shape = STRING)
  @Nullable
  private Instant updated;

  @JsonFormat(shape = STRING)
  @Nullable
  private Instant published;

  @Nullable private String preview;

  @Nullable private List<FeatureType> featureTypes;

  @Nullable private List<DownloadInfo> downloads;
}
