package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

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

  @Nullable private List<Comment> comments;

  @Nullable private Privacy privacy = Privacy.NONE;

  @Nullable private Extent initialExtent;

  @Nullable private String relatedTopics;

  // Map of service.serviceIdentification and List of layers
  @Nullable private Map<String, List<Layer>> layers;
}
