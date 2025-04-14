package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = ColumnInfo.class)
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

  public enum ColumnType {
    BigDecimal,
    Date,
    Double,
    Float,
    Geometry,
    Integer,
    Link,
    Long,
    Text,
    Short,
    Timestamp
  }

  private String name;

  private String alias;

  private ColumnType type;

  private FilterType filterType;

}
