package de.terrestris.mde.mde_backend.model.dto;

import de.terrestris.mde.mde_backend.enumeration.MetadataType;
import lombok.Data;
import tools.jackson.databind.JsonNode;

@Data
public class MetadataJsonPatch {

  private MetadataType type;

  private String key;

  private JsonNode value;
}
