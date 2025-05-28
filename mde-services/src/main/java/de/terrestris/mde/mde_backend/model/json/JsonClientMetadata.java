package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = JsonClientMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonClientMetadata {

  public enum Privacy {
    NONE,
    CRITICAL_INFRASTRUCTURE,
    PERSONAL_DATA,
    INTERNAL_USE_ONLY
  }

  private List<Comment> comments;

  private Privacy privacy = Privacy.NONE;

  private boolean highValueDataset = false;

  private Extent initialExtent;

  private String relatedTopics;

  // Map of service.serviceIdentification and List of layers
  private Map<String, List<Layer>> layers;
}
