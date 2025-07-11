package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = JsonTechnicalMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JsonTechnicalMetadata {

  @Nullable private List<LayerInfo> layerInfos;

  @Nullable private DatabaseInfo databaseInfo;

  @Nullable private List<Category> categories;

  @Nullable private String deliveredCrs;

  @Nullable private List<String> descriptions;
}
