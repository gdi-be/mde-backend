package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.TechnicalMetadataRepository;
import de.terrestris.mde.mde_backend.model.TechnicalMetadata;
import org.springframework.stereotype.Service;

@Service
public class TechnicalMetadataService extends BaseMetadataService<TechnicalMetadataRepository, TechnicalMetadata> {
}
