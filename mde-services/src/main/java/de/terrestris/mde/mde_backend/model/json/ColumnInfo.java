package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = JsonClientMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ColumnInfo {

  public enum FilterType {
    SelectBox,
    CatalogBox,
    DoubleEditOrderField,
    EditField,
    EditOrderField
  }

  private String name;

  private String title;

  private String description;

  private String impressum;

  private boolean listView;

  private boolean listViewEnabled;

  private boolean elementView;

  private boolean elementViewEnabled;

  private boolean statisticsView;

  private FilterType filterType;

  private String minFilterValue;

  private String maxFilterValue;

  private String minOrderValue;

  private String maxOrderValue;

}
