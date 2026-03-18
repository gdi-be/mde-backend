package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.jspecify.annotations.Nullable;

@Data
@JsonDeserialize(as = Lineage.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Lineage implements Serializable {

  // mde-client related id
  private UUID id;

  @KeywordField @Nullable private String identifier;

  @FullTextField @Nullable private String title;

  @Nullable private Instant date;

  public Lineage() {
    this.id = UUID.randomUUID();
  }
}
