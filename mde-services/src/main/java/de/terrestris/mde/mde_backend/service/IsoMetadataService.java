package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import de.terrestris.mde.mde_backend.model.IsoMetadata;
import org.springframework.stereotype.Service;

@Service
public class IsoMetadataService extends BaseMetadataService<IsoMetadataRepository, IsoMetadata> {
}
