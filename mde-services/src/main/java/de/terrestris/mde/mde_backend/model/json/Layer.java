package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = Layer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Layer {

  private String name;

  private String title;

  private String styleName;

  private String styleTitle;

  private String shortDescription;

  private String legendImage;

  private String datasource;

  private String secondaryDatasource;

}
