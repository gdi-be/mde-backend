package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@JsonDeserialize(as = Service.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Service {

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

  private String url;

  private List<ServiceDescription> serviceDescriptions;

  private LegendImage legendImage;

  private List<Source> dataBases;

  private List<Source> publications;

  private Instant created;

  private Instant updated;

  private Instant published;

  private List<Source> previews;

}
