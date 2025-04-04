package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataDeletionResponse {

  private String deletedMetadataCollection;

  private List<String> deletedCatalogRecords;

}
