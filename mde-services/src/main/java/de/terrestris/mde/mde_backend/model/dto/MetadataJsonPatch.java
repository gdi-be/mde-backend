package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import de.terrestris.mde.mde_backend.enumeration.MetadataType;
import lombok.Data;

@Data
public class MetadataJsonPatch {

    private MetadataType type;

    private String key;

    private JsonNode value;

}
