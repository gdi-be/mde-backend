package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = ColumnInfo.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ColumnInfo {

  public enum ColumnType {
    BigDecimal,
    Boolean,
    Date,
    Double,
    Float,
    Geometry,
    Integer,
    Link,
    Long,
    Short,
    Text,
    Timestamp
  }

  @Nullable private String name;

  @Nullable private String alias;

  @Nullable private ColumnType type;
}
