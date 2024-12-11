package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Data
@JsonDeserialize(as = Citation.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Citation {

  private String title;

  @JsonFormat(shape = STRING)
  private Instant date;

  private String url;

}
