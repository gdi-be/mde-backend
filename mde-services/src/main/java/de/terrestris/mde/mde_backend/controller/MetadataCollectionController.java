package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.enumeration.MetadataType;
import de.terrestris.mde.mde_backend.model.BaseMetadata;
import de.terrestris.mde.mde_backend.model.dto.MetadataCollection;
import de.terrestris.mde.mde_backend.model.dto.MetadataCreationData;
import de.terrestris.mde.mde_backend.model.dto.MetadataCreationResponse;
import de.terrestris.mde.mde_backend.model.dto.MetadataJsonPatch;
import de.terrestris.mde.mde_backend.service.MetadataCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.Optional;

@Log4j2
@RestController
@RequestMapping("/metadata/collection")
public class MetadataCollectionController {

    @Autowired
    MetadataCollectionService service;

    @Autowired
    protected MessageSource messageSource;

    @GetMapping("/{metadataId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
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
                    HttpStatus.NOT_FOUND,
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
                HttpStatus.NOT_FOUND,
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
                HttpStatus.NOT_FOUND,
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
              metadataId = service.create(creationData.getTitle(), creationData.getMetadataProfile(), creationData.getCloneMetadataId());
          } else {
              metadataId =  service.create(creationData.getTitle(), creationData.getMetadataProfile());
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
    public BaseMetadata updateJsonValue(@RequestBody MetadataJsonPatch patch, @PathVariable("metadataId")  String metadataId) {
        MetadataType metadataType = patch.getType();

        try {
            return switch (metadataType) {
                case MetadataType.CLIENT ->
                    service.updateClientJsonValue(metadataId, patch.getKey(), patch.getValue());
                case MetadataType.TECHNICAL ->
                    service.updateTechnicalJsonValue(metadataId, patch.getKey(), patch.getValue());
                case MetadataType.ISO ->
                    service.updateIsoJsonValue(metadataId, patch.getKey(), patch.getValue());
                default ->
                    throw new IllegalArgumentException("Invalid metadata type: " + metadataType);
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

}
