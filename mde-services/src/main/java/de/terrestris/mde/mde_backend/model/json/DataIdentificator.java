package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonDeserialize(as = DataIdentificator.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class DataIdentificator {

  private String id;

  private String namespace;

}
