package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@JsonDeserialize(as = ContentDescription.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode
public class ContentDescription {

  @Nullable private String url;

  @Nullable private String description;

  @Nullable private CI_OnLineFunctionCode code;
}
