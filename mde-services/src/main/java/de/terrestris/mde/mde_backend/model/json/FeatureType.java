package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = FeatureType.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class FeatureType {

  // mde-client related id
  private UUID id;

  @Nullable private List<ColumnInfo> columns;

  @Nullable private String name;

  @Nullable private String title;

  @Nullable private String shortDescription;

  public FeatureType() {
    this.id = UUID.randomUUID();
  }
}
