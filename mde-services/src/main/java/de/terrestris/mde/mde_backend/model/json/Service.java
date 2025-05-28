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

  private String title;

  private String shortDescription;

  private String contentDescription;

  private String technicalDescription;

  private String fileIdentifier;

  private String serviceIdentification;

  private ServiceType serviceType;

  // imported from service connectPoint
  private String url;

  // imported from CapabilitiesUrl in service files (non ISO section)
  private String capabilitiesUrl;

  private List<ServiceDescription> serviceDescriptions;

  private LegendImage legendImage;

  private List<Source> publications;

  @JsonFormat(shape = STRING)
  private Instant created;

  @JsonFormat(shape = STRING)
  private Instant updated;

  @JsonFormat(shape = STRING)
  private Instant published;

  private String preview;

  private List<FeatureType> featureTypes;

  private List<DownloadInfo> downloads;
}
