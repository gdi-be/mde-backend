package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

@Data
@JsonDeserialize(as = JsonClientMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonClientMetadata implements Serializable {

  @Nullable private List<Comment> comments;

  @Nullable private Extent initialExtent;

  @Nullable private String relatedTopics;

  // Map of service.serviceIdentification and List of layers
  @Nullable private Map<String, List<Layer>> layers;
}
