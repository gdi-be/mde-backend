package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataCreationResponse {

  private String title;

  private String metadataId;
}
