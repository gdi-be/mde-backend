package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import jakarta.validation.constraints.Null;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataCreationData {

  private MetadataProfile metadataProfile;

  private String title;

  @Null private String cloneMetadataId;
}
