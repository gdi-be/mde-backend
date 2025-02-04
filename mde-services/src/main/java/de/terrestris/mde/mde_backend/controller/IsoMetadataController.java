package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.service.IsoGenerator;
import de.terrestris.mde.mde_backend.service.IsoMetadataService;
import de.terrestris.mde.mde_backend.service.PublicationService;
import de.terrestris.mde.mde_backend.service.ValidatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.hibernate.search.engine.search.query.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@RequestMapping("/metadata/iso")
public class IsoMetadataController extends BaseMetadataController<IsoMetadataService, IsoMetadata> {

  @Autowired
  private IsoGenerator isoGenerator;

  @Autowired
  private ValidatorService validator;

  @Autowired
  private PublicationService publicationService;

  @GetMapping(
    path = "/search",
    produces = {
      "application/json"
    }
  )
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Ok: The entity was successfully updated"
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error: Something internal went wrong while updating the entity"
    )
  })
  public List<IsoMetadata> search(@RequestParam String searchTerm, @RequestParam(required = false) Integer offset, @RequestParam(required = false) Integer limit) {

    log.trace("Search request for IsoMetadata with searchTerm: {}, offset: {}, limit: {}", searchTerm, offset, limit);
    try {
      SearchResult<IsoMetadata> result = this.service.search(searchTerm, offset, limit);
      return result.hits();
    } catch (Exception e) {
      log.error("Error while searching for IsoMetadata with searchTerm: {}, offset: {}, limit: {}", searchTerm, offset, limit, e);

      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        messageSource.getMessage(
          "BASE_CONTROLLER.INTERNAL_SERVER_ERROR",
          null,
          LocaleContextHolder.getLocale()
        ),
        e
      );
    }
  }

  @GetMapping(path = "/validate/{id}")
  public ResponseEntity<List<String>> validateIsoMetadata(@PathVariable("id") String id) throws XMLStreamException, IOException {
    var list = validator.validateMetadata(id);
    return new ResponseEntity<>(list, OK);
  }

  @GetMapping(path = "/publish/{id}")
  public ResponseEntity<String> publishMetadata(@PathVariable("id") String id) {
    try {
      publicationService.publishMetadata(id);
    } catch (XMLStreamException | IOException | URISyntaxException | InterruptedException e) {
      log.warn("Unable to publish metadata: {}", e.getMessage());
      log.trace("Stack trace:", e);
    }
    return new ResponseEntity<>(OK);
  }

}
