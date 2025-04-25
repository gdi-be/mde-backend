package de.terrestris.mde.mde_backend.model.json.termsofuse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = TermsOfUse.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class TermsOfUse {

  private int id;

  private String shortname;

  private boolean active;

  private String description;

  private List<String> matchStrings;

  private boolean openData;

  private Json json;

  private String note;
}
