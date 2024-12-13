package de.terrestris.mde.mde_backend.model.dto;

import de.terrestris.mde.mde_backend.model.json.JsonClientMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonTechnicalMetadata;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetadataCollection {

    JsonTechnicalMetadata technicalMetadata;

    JsonClientMetadata clientMetadata;

    JsonIsoMetadata isoMetadata;
}
