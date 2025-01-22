package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_RestrictionCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = Constraint.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Constraint {

  public enum ConstraintType {
    accessConstraints,
    useConstraints,
    otherConstraints
  }

  private String namespace;

  private String text;

  private MD_RestrictionCode restrictionCode;

  private ConstraintType type;

}
