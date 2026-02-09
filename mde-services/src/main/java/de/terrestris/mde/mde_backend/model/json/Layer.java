package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = Layer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class Layer {

  // mde-client related id
  private UUID id;

  @Nullable private String name;

  @Nullable private String title;

  @Nullable private String styleName;

  @Nullable private String styleTitle;

  @Nullable private String shortDescription;

  @Nullable private String legendImage;

  @Nullable private String datasource;

  @Nullable private String secondaryDatasource;

  public Layer() {
    this.id = UUID.randomUUID();
  }
}
