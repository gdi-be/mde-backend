package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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

  private Privacy privacy;

  private boolean highValueDataset;

  private Extent initialExtent;

  private Map<String, List<Layer>> layers;

}
