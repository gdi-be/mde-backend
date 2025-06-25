package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = Layer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Layer {

  @Nullable private String name;

  @Nullable private String title;

  @Nullable private String styleName;

  @Nullable private String styleTitle;

  @Nullable private String shortDescription;

  @Nullable private String legendImage;

  @Nullable private String datasource;

  @Nullable private String secondaryDatasource;
}
