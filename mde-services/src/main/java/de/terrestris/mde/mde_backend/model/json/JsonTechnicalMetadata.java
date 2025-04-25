package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = JsonTechnicalMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonTechnicalMetadata {

  private List<LayerInfo> layerInfos;

  private DatabaseInfo databaseInfo;

  private List<Category> categories;

  private String deliveredCrs;

  private List<String> descriptions;
}
