package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = PreviewMap.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class PreviewMap {

  @Nullable private String crs;

  @Nullable private Double minx;

  @Nullable private Double miny;

  @Nullable private Double maxx;

  @Nullable private Double maxy;

  @Nullable private String url;
}
