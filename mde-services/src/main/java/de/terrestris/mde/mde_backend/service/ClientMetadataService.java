package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.ClientMetadataRepository;
import de.terrestris.mde.mde_backend.model.ClientMetadata;
import org.springframework.stereotype.Service;

@Service
public class ClientMetadataService extends BaseMetadataService<ClientMetadataRepository, ClientMetadata> {
}
