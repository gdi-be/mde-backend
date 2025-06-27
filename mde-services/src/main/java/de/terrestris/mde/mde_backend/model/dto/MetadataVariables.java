package de.terrestris.mde.mde_backend.model.dto;

import lombok.Data;

@Data
public class MetadataVariables {

  private String profileName;

  private String profileVersion;

  private String registry;

  private String regionalKey;

  private String serviceUrl;

  private String lineage;

  private String standardFormat;

  private String standardVersion;
}
