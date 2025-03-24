package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.enumeration.MetadataType;
import de.terrestris.mde.mde_backend.model.BaseMetadata;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.dto.*;
import de.terrestris.mde.mde_backend.model.json.Comment;
import de.terrestris.mde.mde_backend.service.DatasetIsoGenerator;
import de.terrestris.mde.mde_backend.service.MetadataCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.hibernate.search.engine.search.query.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@RequestMapping("/metadata")
public class MetadataCollectionController extends BaseMetadataController<MetadataCollectionService, MetadataCollection> {

  @Autowired
  MetadataCollectionService service;

  @Autowired
  protected MessageSource messageSource;

  @GetMapping("/{metadataId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Ok: The MetadataCollection was successfully returned"
    ),
    @ApiResponse(
      responseCode = "401",
      description = "Unauthorized: You need to provide a bearer token",
      content = @Content
    ),
    @ApiResponse(
      responseCode = "404",
      description = "Not found: The provided ID does not exist (or you don't have the permission to read it)"
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error: Something internal went wrong while getting the MetadataCollection"
    )
  })
  public MetadataCollection findOneByMetadataId(@PathVariable("metadataId") String metadataId) {
    log.trace("Requested to return MetadataCollection with id {}.", metadataId);

    try {
      Optional<MetadataCollection> optMetadataCollection = service.findOneByMetadataId(metadataId);

      if (optMetadataCollection.isPresent()) {
        MetadataCollection metadataCollection = optMetadataCollection.get();

        log.trace("Successfully got MetadataCollection with metadataId {}", metadataId);

        return metadataCollection;
      } else {
        log.error("Could not find MetadataCollection with metadataId {}", metadataId);

        throw new ResponseStatusException(
          NOT_FOUND,
          messageSource.getMessage(
            "BASE_CONTROLLER.NOT_FOUND",
            null,
            LocaleContextHolder.getLocale()
          )
        );
      }
    } catch (AccessDeniedException ade) {
      log.warn("Access to MetadataCollection with metadataId {} is denied", metadataId);
      log.trace("Full Stack trace:", ade);

      throw new ResponseStatusException(
        NOT_FOUND,
        messageSource.getMessage(
          "BASE_CONTROLLER.NOT_FOUND",
          null,
          LocaleContextHolder.getLocale()
        ),
        ade
      );
    } catch (
      NoSuchElementException nsee) {
      log.error("Could not get MetadataCollection with metadataId {}. Reason: {}", metadataId, nsee.getMessage());
      log.trace("Full Stack trace:", nsee);

      throw new ResponseStatusException(
        NOT_FOUND,
        messageSource.getMessage(
          "BASE_CONTROLLER.NOT_FOUND",
          null,
          LocaleContextHolder.getLocale()
        ),
        nsee
      );
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      log.error("Error while requesting MetadataCollection with metadataId {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @PostMapping("/")
  public MetadataCreationResponse create(@RequestBody MetadataCreationData creationData) {

    try {
      String metadataId;
      if (creationData.getCloneMetadataId() != null) {
        metadataId = service.create(creationData.getTitle(), creationData.getCloneMetadataId());
      } else {
        metadataId = service.create(creationData.getTitle());
      }
      MetadataCreationResponse response = new MetadataCreationResponse();
      response.setMetadataId(metadataId);
      response.setTitle(creationData.getTitle());

      return response;
      // TODO: Add more specific exception handling
    } catch (Exception e) {
      log.error("Error while creating metadata: {}", e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @PatchMapping("/{metadataId}")
  public BaseMetadata updateJsonValue(@RequestBody MetadataJsonPatch patch, @PathVariable("metadataId") String metadataId) {
    MetadataType metadataType = patch.getType();

    try {
      return switch (metadataType) {
        case MetadataType.CLIENT -> service.updateClientJsonValue(metadataId, patch.getKey(), patch.getValue());
        case MetadataType.TECHNICAL -> service.updateTechnicalJsonValue(metadataId, patch.getKey(), patch.getValue());
        case MetadataType.ISO -> service.updateIsoJsonValue(metadataId, patch.getKey(), patch.getValue());
        default -> throw new IllegalArgumentException("Invalid metadata type: " + metadataType);
      };
      // TODO: Add more specific exception handling
    } catch (Exception e) {
      log.error("Error while updating metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @GetMapping("/{metadataId}/autokeywords")
  public ResponseEntity<List<String>> getAutomaticKeywords(@PathVariable("metadataId") String metadataId) {
    var metadataOptional = service.findOneByMetadataId(metadataId);
    if (metadataOptional.isEmpty()) {
      return new ResponseEntity<>(NOT_FOUND);
    }
    var isoMetadata = metadataOptional.get().getIsoMetadata();
    return new ResponseEntity<>(DatasetIsoGenerator.getAutomaticKeywords(isoMetadata), OK);
  }

  @PostMapping("/{metadataId}/assignUser")
  public ResponseEntity<Void> assignUser(@PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.assignUser(metadataId, userId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error("Error while assigning user to metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @DeleteMapping("/{metadataId}/unassignUser")
  public ResponseEntity<Void> unassignUser(@PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.unassignUser(metadataId, userId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error("Error while unassigning user from metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @DeleteMapping("/{metadataId}/addToTeam")
  public ResponseEntity<Void> addToTeam(@PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.addToTeam(metadataId, userId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error("Error while adding user to team of metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @PostMapping("/{metadataId}/removeFromTeam")
  public ResponseEntity<Void> removeFromTeam(@PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.removeFromTeam(metadataId, userId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error("Error while removing user from team of metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @PostMapping("/{metadataId}/assignRole")
  public ResponseEntity<Void> assignRole(@PathVariable("metadataId") String metadataId, @RequestBody String role) {
    try {
      service.assignRole(metadataId, role);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error("Error while assigning role to metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @DeleteMapping("/{metadataId}/unassignRole")
  public ResponseEntity<Void> unassignRole(@PathVariable("metadataId") String metadataId) {
    try {
      service.unassignRole(metadataId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error("Error while unassigning role from metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @PostMapping(
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
  public SearchResponse<MetadataCollection> search(@RequestBody SearchConfig searchConfig) {

    log.trace("Search request for MetadataCollection with searchConfig: {}", searchConfig);
    try {
      SearchResult<MetadataCollection> result = this.service.search(searchConfig);
      return new SearchResponse<MetadataCollection>(result.hits(), result.total().hitCount());
    } catch (Exception e) {
      log.error("Error while searching for MetadataCollection with searchConfig: {}", searchConfig, e);

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

  @PostMapping("/{metadataId}/comment")
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Ok: The Comment was successfully added"
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error: Something internal went wrong while adding the Comment"
    )
  })
  public ResponseEntity<Comment> addComment(
    @RequestBody String commentText,
    @PathVariable("metadataId") String metadataId
  ) {
    try {
      Comment comment = service.addComment(metadataId, commentText);
      return ResponseEntity.status(OK).body(comment);
      // TODO: Add more specific exception handling
    } catch (Exception e) {
      log.error("Error while add comment: {}", e.getMessage());
      log.trace("Full stack trace: ", e);

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

  @DeleteMapping("/{metadataId}/comment")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  public ResponseEntity<Void> deleteComment(
    @RequestBody String commentId,
    @PathVariable("metadataId") String metadataId
  ) {
    try {
      service.deleteComment(metadataId, UUID.fromString(commentId));
      return ResponseEntity.status(OK).build();
    } catch (Exception e) {
      log.error("Error while deleting comment: {}", e.getMessage());
      log.trace("Full stack trace: ", e);

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

}
