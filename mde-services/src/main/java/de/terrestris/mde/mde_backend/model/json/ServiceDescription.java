package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import lombok.*;
import org.jspecify.annotations.Nullable;

@Data
@JsonDeserialize(as = ServiceDescription.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class ServiceDescription implements Serializable {

  @Nullable private String type;

  @Nullable private String url;
}
