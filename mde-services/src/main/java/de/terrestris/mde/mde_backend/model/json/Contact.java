package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_RoleCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = Contact.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class Contact {

  @Nullable private String name;

  @Nullable private String organisation;

  @Nullable private String contact;

  @Nullable private String phone;

  @Nullable private String email;

  @Nullable private CI_OnLineFunctionCode code;

  @Nullable private String url;

  @Nullable private CI_RoleCode roleCode;
}
