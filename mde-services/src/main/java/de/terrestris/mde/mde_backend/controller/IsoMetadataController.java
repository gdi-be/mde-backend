package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.service.IsoMetadataService;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/metadata/iso")
public class IsoMetadataController extends BaseMetadataController<IsoMetadataService, IsoMetadata> {


}
