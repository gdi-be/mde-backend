package de.terrestris.mde.mde_backend.model.json.termsofuse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = TermsOfUse.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class TermsOfUse {

  @Nullable
  private Integer id;

  @Nullable
  private String shortname;

  private boolean active = false;

  @Nullable
  private String description;

  @Nullable
  private List<String> matchStrings;

  private boolean openData = false;

  @Nullable
  private Json json;

  @Nullable
  private String note;
}
