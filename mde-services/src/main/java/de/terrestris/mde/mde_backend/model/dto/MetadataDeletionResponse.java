package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataDeletionResponse {

  private String deletedMetadataCollection;

  private List<String> deletedCatalogRecords;

  private List<String> affectedClones;
}
