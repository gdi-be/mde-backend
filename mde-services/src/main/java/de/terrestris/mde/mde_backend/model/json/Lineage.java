package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = Lineage.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Lineage {

  // mde-client related id
  private UUID id;

  @KeywordField @Nullable private String identifier;

  @FullTextField @Nullable private String title;

  @Nullable private Instant date;

  public Lineage() {
    this.id = UUID.randomUUID();
  }
}
