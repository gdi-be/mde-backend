package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.service.IsoMetadataService;
import de.terrestris.mde.mde_backend.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Log4j2
@RestController
@RequestMapping("/search")
public class SearchController extends BaseMetadataController<IsoMetadataService, IsoMetadata> {

  @Autowired
  private SearchService service;

  @GetMapping(path = "/index/initialize")
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Ok: The index was successfully initialized"
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error: Something internal went wrong while initializing the index"
    )
  })
  public void initializeIndex() {
    log.info("Initializing index");
    try {
      service.reindexAll();
    } catch (RuntimeException e) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        messageSource.getMessage(
          "BaseController.INTERNAL_SERVER_ERROR",
          null,
          LocaleContextHolder.getLocale()
        ),
        e
      );
    };
  }

}
