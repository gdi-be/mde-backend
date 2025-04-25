package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.BaseMetadata;
import de.terrestris.mde.mde_backend.service.BaseMetadataService;

public abstract class BaseMetadataController<
    T extends BaseMetadataService<?, S>, S extends BaseMetadata> {}
