package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_RoleCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = Contact.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Contact {

  private String name;

  private String organisation;

  private String contact;

  private String phone;

  private String email;

  private CI_OnLineFunctionCode code;

  private String url;

  private CI_RoleCode roleCode;

}
