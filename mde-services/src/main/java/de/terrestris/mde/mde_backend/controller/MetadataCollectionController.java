package de.terrestris.mde.mde_backend.controller;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import de.terrestris.mde.mde_backend.enumeration.MetadataType;
import de.terrestris.mde.mde_backend.model.BaseMetadata;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.dto.*;
import de.terrestris.mde.mde_backend.model.json.Comment;
import de.terrestris.mde.mde_backend.model.json.Layer;
import de.terrestris.mde.mde_backend.service.*;
import de.terrestris.mde.mde_backend.thread.TrackedTask;
import de.terrestris.mde.mde_backend.thread.TrackingExecutorService;
import de.terrestris.mde.mde_backend.utils.PublicationException;
import de.terrestris.utils.io.ZipUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.xml.stream.XMLStreamException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.hibernate.search.engine.search.query.SearchResult;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Log4j2
@RestController
@RequestMapping("/metadata")
public class MetadataCollectionController
    extends BaseMetadataController<MetadataCollectionService, MetadataCollection> {

  @Autowired MetadataCollectionService service;

  @Autowired ValidatorService validatorService;

  @Autowired IsoGenerator isoGenerator;

  @Autowired PublicationService publicationService;

  @Autowired protected MessageSource messageSource;

  @Autowired private TrackingExecutorService executor;

  @GetMapping("/{metadataId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The MetadataCollection was successfully returned"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: You need to provide a bearer token",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description =
                "Not found: The provided ID does not exist (or you don't have the permission to read it)"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while getting the MetadataCollection")
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
                "BASE_CONTROLLER.NOT_FOUND", null, LocaleContextHolder.getLocale()));
      }
    } catch (AccessDeniedException ade) {
      log.warn("Access to MetadataCollection with metadataId {} is denied", metadataId);
      log.trace("Full Stack trace:", ade);

      throw new ResponseStatusException(
          NOT_FOUND,
          messageSource.getMessage(
              "BASE_CONTROLLER.NOT_FOUND", null, LocaleContextHolder.getLocale()),
          ade);
    } catch (NoSuchElementException nsee) {
      log.error(
          "Could not get MetadataCollection with metadataId {}. Reason: {}",
          metadataId,
          nsee.getMessage());
      log.trace("Full Stack trace:", nsee);

      throw new ResponseStatusException(
          NOT_FOUND,
          messageSource.getMessage(
              "BASE_CONTROLLER.NOT_FOUND", null, LocaleContextHolder.getLocale()),
          nsee);
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      log.error(
          "Error while requesting MetadataCollection with metadataId {}: \n {}",
          metadataId,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PostMapping("/")
  public MetadataCreationResponse create(@RequestBody MetadataCreationData creationData) {

    try {
      String metadataId;
      if (creationData.getCloneMetadataId() != null) {
        metadataId = service.clone(creationData.getTitle(), creationData.getCloneMetadataId());
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
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PatchMapping("/{metadataId}")
  public BaseMetadata updateJsonValue(
      @RequestBody MetadataJsonPatch patch, @PathVariable("metadataId") String metadataId) {
    MetadataType metadataType = patch.getType();

    try {
      return switch (metadataType) {
        case MetadataType.CLIENT ->
            service.updateClientJsonValue(metadataId, patch.getKey(), patch.getValue());
        case MetadataType.TECHNICAL ->
            service.updateTechnicalJsonValue(metadataId, patch.getKey(), patch.getValue());
        case MetadataType.ISO ->
            service.updateIsoJsonValue(metadataId, patch.getKey(), patch.getValue());
        default -> throw new IllegalArgumentException("Invalid metadata type: " + metadataType);
      };
      // TODO: Add more specific exception handling
    } catch (Exception e) {
      log.error("Error while updating metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PatchMapping("/{metadataId}/updateLayers/{serviceIdentification}")
  public String updateLayers(
      @PathVariable("metadataId") String metadataId,
      @PathVariable("serviceIdentification") String serviceIdentification,
      @RequestBody List<Layer> layers) {
    try {
      return service.updateLayers(metadataId, serviceIdentification, layers);
    } catch (Exception e) {
      log.error(
          "Error while updating layers of metadata with id {} and serviceIdentification {}: \n {}",
          metadataId,
          serviceIdentification,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @GetMapping("/{metadataId}/autokeywords")
  public ResponseEntity<List<String>> getAutomaticKeywords(
      @PathVariable("metadataId") String metadataId) {
    var metadataOptional = service.findOneByMetadataId(metadataId);
    if (metadataOptional.isEmpty()) {
      return new ResponseEntity<>(NOT_FOUND);
    }
    var isoMetadata = metadataOptional.get().getIsoMetadata();
    return new ResponseEntity<>(DatasetIsoGenerator.getAutomaticKeywords(isoMetadata), OK);
  }

  @GetMapping(value = "/{metadataId}/validate", produces = "application/json")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Validate a metadata collection by its ID",
      description =
          "Starts a validation job for the given metadata collection in the background. To get the results you"
              + " need to be subscribed to the server sent events (SSE) endpoint at /events/subscribe.",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The validation job was successfully started",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = String.class),
                    examples = {
                      @ExampleObject(
                          description =
                              "The response contains the metadata ID of the metadata collection that is going to be validated.",
                          value = "87e4d4b4-412d-46a6-ac82-184941128aab")
                    })),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: You need to provide a bearer token",
            content = @Content),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict: The validation job is already running for this metadata ID",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while starting the validation job",
            content = @Content)
      })
  // TODO Make validation canceable?
  public ResponseEntity<String> validate(@PathVariable("metadataId") String metadataId) {
    Set<Runnable> tasks = executor.getRunningTasks();

    // TODO Check for metadataId existence?
    for (Runnable task : tasks) {
      if (task instanceof TrackedTask trackedTask) {
        if (trackedTask.getTaskId().equals(metadataId)) {
          log.warn("Validation job for metadata with ID {} is already running.", metadataId);
          return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
      } else {
        log.warn("Task is not a TrackedTask: {}", task);
      }
    }

    try {
      TrackedTask task = validatorService.createValidateMetadataTask(metadataId);

      executor.submit(task);

      return new ResponseEntity<>(metadataId, OK);
    } catch (Exception e) {
      log.error("Error while validating metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @GetMapping("/{metadataId}/download")
  public ResponseEntity<byte[]> download(@PathVariable("metadataId") String metadataId) {
    try {
      var files = isoGenerator.generateMetadata(metadataId);
      Path tmp = Files.createTempFile(null, null);
      ZipUtils.zip(tmp.toFile(), files.getFirst().getParent().toFile(), true);
      var bs = IOUtils.toByteArray(Files.newInputStream(tmp));
      Files.delete(tmp);
      return new ResponseEntity<>(bs, OK);
    } catch (XMLStreamException | IOException e) {
      log.error(
          "Error while downloading/generating metadata with id {}: \n {}",
          metadataId,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PostMapping("/{metadataId}/assignUser")
  public ResponseEntity<Void> assignUser(
      @PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.assignUser(metadataId, userId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error(
          "Error while assigning user to metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @DeleteMapping("/{metadataId}/unassignUser")
  public ResponseEntity<Void> unassignUser(
      @PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.unassignUser(metadataId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error(
          "Error while unassigning user from metadata with id {}: \n {}",
          metadataId,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @DeleteMapping("/{metadataId}/addToTeam")
  public ResponseEntity<Void> addToTeam(
      @PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.addToTeam(metadataId, userId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error(
          "Error while adding user to team of metadata with id {}: \n {}",
          metadataId,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PostMapping("/{metadataId}/removeFromTeam")
  public ResponseEntity<Void> removeFromTeam(
      @PathVariable("metadataId") String metadataId, @RequestBody String userId) {
    try {
      service.removeFromTeam(metadataId, userId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error(
          "Error while removing user from team of metadata with id {}: \n {}",
          metadataId,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PostMapping("/{metadataId}/assignRole")
  public ResponseEntity<Void> assignRole(
      @PathVariable("metadataId") String metadataId, @RequestBody AssignRoleData data) {
    try {
      service.assignRole(metadataId, data.getRole());

      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error(
          "Error while assigning role to metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @DeleteMapping("/{metadataId}/unassignRole")
  public ResponseEntity<Void> unassignRole(@PathVariable("metadataId") String metadataId) {
    try {
      service.unassignRole(metadataId);
      return new ResponseEntity<Void>(OK);
    } catch (Exception e) {
      log.error(
          "Error while unassigning role from metadata with id {}: \n {}",
          metadataId,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @GetMapping(
      path = "/search",
      produces = {"application/json"})
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Ok: The entity was successfully updated"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while updating the entity")
      })
  public List<MetadataCollection> search(
      @RequestParam String searchTerm,
      @RequestParam(required = false) Integer offset,
      @RequestParam(required = false) Integer limit) {

    log.trace(
        "Search request for MetadataCollection with searchTerm: {}, offset: {}, limit: {}",
        searchTerm,
        offset,
        limit);
    try {
      SearchResult<MetadataCollection> result = this.service.search(searchTerm, offset, limit);
      return result.hits();
    } catch (Exception e) {
      log.error(
          "Error while searching for MetadataCollection with searchTerm: {}, offset: {}, limit: {}",
          searchTerm,
          offset,
          limit,
          e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PostMapping(
      path = "/query",
      produces = {"application/json"})
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: MetadataCollections were successfully queried"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while querying MetadataCollections")
      })
  public Page<MetadataCollection> query(
      @RequestBody QueryConfig queryConfig,
      @PageableDefault(Integer.MAX_VALUE) @ParameterObject Pageable pageable) {
    log.trace("Query MetadataCollections with queryConfig: {}", queryConfig);
    try {
      return this.service.query(queryConfig, pageable);
    } catch (Exception e) {
      log.error("Error while querying MetadataCollection with queryConfig: {}", queryConfig, e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PostMapping("/{metadataId}/comment")
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Ok: The Comment was successfully added"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while adding the Comment")
      })
  public ResponseEntity<Comment> addComment(
      @RequestBody String commentText, @PathVariable("metadataId") String metadataId) {
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
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @DeleteMapping("/{metadataId}/comment")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(security = {@SecurityRequirement(name = "bearer-key")})
  public ResponseEntity<Void> deleteComment(
      @RequestBody String commentId, @PathVariable("metadataId") String metadataId) {
    try {
      service.deleteComment(metadataId, UUID.fromString(commentId));
      return ResponseEntity.status(OK).build();
    } catch (Exception e) {
      log.error("Error while deleting comment: {}", e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @DeleteMapping("/{metadataId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Delete a metadata collection by its ID",
      description =
          "Removes the given metadata collection from the database and all its referenced records "
              + "in the catalog (Geonetwork).",
      security = {@SecurityRequirement(name = "Bearer Authentication")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The metadata collection was successfully deleted",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MetadataDeletionResponse.class),
                    examples = {
                      @ExampleObject(
                          name = "MetadataDeletionResponse",
                          description =
                              "The response contains the deleted metadata collection ID and the deleted catalog records.",
                          value =
                              "{ "
                                  + "\"deletedMetadataCollection\": \"87e4d4b4-412d-46a6-ac82-184941128aab\", "
                                  + "\"deletedCatalogRecords\": ["
                                  + "\"7a34c154-6cdc-40fe-a338-db546d60155f\", "
                                  + "\"73a1ce13-da31-4395-8b2a-c961794fd112\""
                                  + "]"
                                  + "}")
                    })),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: You need to provide a bearer token",
            content = @Content),
        @ApiResponse(
            responseCode = "404",
            description =
                "Not found: The provided metadata collection does not exist (or you don't have the permission to delete it)",
            content = @Content),
        @ApiResponse(
            responseCode = "409",
            description =
                "Conflict: The metadata collection is assigned to a user or you are not the owner or in the team and cannot be deleted",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while deleting the metadata collection",
            content = @Content)
      })
  public ResponseEntity<?> delete(
      @PathVariable("metadataId") String metadataId, Authentication auth) {
    return new ResponseEntity<>(publicationService.deleteMetadata(metadataId, auth), OK);
  }

  @GetMapping("/{metadataId}/team")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The team members were successfully returned"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while getting the team members")
      })
  public ResponseEntity<List<UserData>> getTeam(@PathVariable("metadataId") String metadataId) {
    try {
      List<UserData> teamWithRoles = service.getTeamWithRoles(metadataId);

      return new ResponseEntity<>(teamWithRoles, OK);
    } catch (Exception e) {
      log.error(
          "Error while getting the team members of the metadata collection with id {}: {}",
          metadataId,
          e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @PostMapping("/{metadataId}/approved")
  public ResponseEntity<Void> approveMetadata(@PathVariable("metadataId") String metadataId) {
    try {
      service.setApprovalState(metadataId, true);
      return new ResponseEntity<>(OK);
    } catch (Exception e) {
      log.error("Error while approving metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @DeleteMapping("/{metadataId}/approved")
  public ResponseEntity<Void> disapproveMetadata(@PathVariable("metadataId") String metadataId) {
    try {
      service.setApprovalState(metadataId, false);
      return new ResponseEntity<>(OK);
    } catch (Exception e) {
      log.error("Error while disapproving metadata with id {}: \n {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @Operation(
      summary = "Publishes a metadata collection in the catalog (Geonetwork)",
      description = "Publishes the metadata collection in the catalog (Geonetwork).",
      security = {@SecurityRequirement(name = "Bearer Authentication")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The metadata was successfully published",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MetadataPublishResponse.class),
                    examples = {
                      @ExampleObject(
                          name = "MetadataPublishResponse",
                          description =
                              "The response contains the published metadata catalog records.",
                          value =
                              "{ "
                                  + "\"publishedCatalogRecords\": ["
                                  + "\"7a34c154-6cdc-40fe-a338-db546d60155f\", "
                                  + "\"73a1ce13-da31-4395-8b2a-c961794fd112\""
                                  + "]"
                                  + "}")
                    })),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict: Prerequisites for publishing the metadata are not met",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while publishing the metadata",
            content = @Content)
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/{metadataId}/publish", produces = "application/json")
  public ResponseEntity<MetadataPublishResponse> publishMetadata(
      @PathVariable("metadataId") String metadataId) {
    try {
      ArrayList<String> uuids = publicationService.publishMetadata(metadataId);

      var response = new MetadataPublishResponse();
      response.setPublishedCatalogRecords(uuids);

      return new ResponseEntity<>(response, OK);
    } catch (PublicationException e) {
      log.error("Prerequisites for publishing the metadata are not met: {}", e.getMessage());
      log.trace("Full stack trace: ", e);

      return new ResponseEntity<>(HttpStatus.CONFLICT);
    } catch (Exception e) {
      log.error("Error while publishing metadata with id {}: {}", metadataId, e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @Operation(
      summary = "Publishes all metadata collections in the catalog (Geonetwork)",
      description = "Publishes all metadata collections in the catalog (Geonetwork).",
      security = {@SecurityRequirement(name = "Bearer Authentication")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The metadata was successfully published"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while publishing the metadata",
            content = @Content)
      })
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/publishall", produces = "application/json")
  public ResponseEntity<String> publishAllMetadata() {
    try {
      publicationService.publishAllMetadata();

      return new ResponseEntity<>(OK);
    } catch (Exception e) {
      log.error("Error while publishing all metadata: {}", e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }

  @Operation(
      summary = "Generates all ISO documents for all metadata collections",
      description =
          "Generates all ISO documents for all metadata collections and returns them as a zip file.",
      security = {@SecurityRequirement(name = "Bearer Authentication")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The metadata was successfully generated"),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while generating the metadata",
            content = @Content)
      })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/downloadall")
  public ResponseEntity<byte[]> downloadAll() {
    try {
      var files = isoGenerator.generateAllMetadata();
      var tmp = Files.createTempFile(null, null);
      var tmpDir = Files.createTempDirectory(null);
      log.debug("Preparing zip in {}", tmpDir.toAbsolutePath());
      for (var file : files) {
        Files.move(file, tmpDir.resolve(file.getFileName()));
        if (file.getParent().toFile().listFiles().length == 0) {
          Files.delete(file.getParent());
        }
      }
      ZipUtils.zip(tmp.toFile(), tmpDir.toFile(), true);
      var bs = IOUtils.toByteArray(Files.newInputStream(tmp));
      Files.delete(tmp);
      deleteDirectory(tmpDir.toFile());
      return new ResponseEntity<>(bs, OK);
    } catch (XMLStreamException | IOException e) {
      log.error("Error while downloading/generating metadata: \n {}", e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }
}
