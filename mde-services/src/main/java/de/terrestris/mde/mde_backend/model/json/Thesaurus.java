package de.terrestris.mde.mde_backend.model.json;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = Thesaurus.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Thesaurus {

  @Nullable
  private String title;

  @Nullable
  private String namespace;

  @JsonFormat(shape = STRING)
  @Nullable
  private Instant date;

  @Nullable
  private CI_DateTypeCode code;
}
