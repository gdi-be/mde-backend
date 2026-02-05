package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_RoleCode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = Contact.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class Contact {

  // mde-client related id
  private UUID id;

  @Nullable private String name;

  @Nullable private String organisation;

  @Nullable private String contact;

  @Nullable private String phone;

  @Nullable private String email;

  @Nullable private CI_OnLineFunctionCode code;

  @Nullable private String url;

  @Nullable private CI_RoleCode roleCode;

  public Contact() {
    this.id = UUID.randomUUID();
  }
}
