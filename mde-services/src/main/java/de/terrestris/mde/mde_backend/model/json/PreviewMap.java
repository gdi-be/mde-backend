package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@JsonDeserialize(as = PreviewMap.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class PreviewMap implements Serializable {

  @Nullable private String crs;

  @Nullable private Double minx;

  @Nullable private Double miny;

  @Nullable private Double maxx;

  @Nullable private Double maxy;

  @Nullable private String url;
}
