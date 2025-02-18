package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.BaseMetadata;
import de.terrestris.mde.mde_backend.service.BaseMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.Optional;

@Log4j2
public abstract class BaseMetadataController<T extends BaseMetadataService<?, S>, S extends BaseMetadata> {

    @Autowired
    protected T service;

    @Autowired
    protected MessageSource messageSource;

    @GetMapping(
            produces = { "application/json" }
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Returns all entities",
            security = { @SecurityRequirement(name = "bearer-key") }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The entities where successfully returned"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: You need to provide a bearer token",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error: Something internal went wrong while getting the entities"
        )
    })
    public Page<S> findAll(@PageableDefault(Integer.MAX_VALUE) @ParameterObject Pageable pageable) {
        log.trace("Requested to return all entities of type {}", getGenericClassName());

        try {
            Page<S> persistedEntities = service.findAll(pageable);

            log.trace("Successfully got all entities of type {} (count: {})",
                    getGenericClassName(), persistedEntities.getTotalElements());

            return persistedEntities;
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Error while requesting all entities of type {}: \n {}",
                    getGenericClassName(), e.getMessage());
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

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: The entity was successfully updated"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: You need to provide a bearer token",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "You don't have the permission to update this entity"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error: Something internal went wrong while updating the entity"
        )
    })
    public S update(@RequestBody S entity, @PathVariable("id") BigInteger entityId) {
        log.trace("Requested to update entity of type {} with ID {} ({})",
                getGenericClassName(), entityId, entity);

        try {
            if (!entityId.equals(entity.getId())) {
                log.error("IDs of update candidate (ID: {}) and update data ({}) don't match.",
                        entityId, entity);

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }

            Optional<S> persistedEntity = service.findOne(entityId);

            if (persistedEntity.isPresent()) {
                S updatedEntity = service.update(entityId, entity);

                log.trace("Successfully updated entity of type {} with ID {}",
                        getGenericClassName(), entityId);

                return updatedEntity;
            } else {
                log.error("Could not find entity of type {} with ID {}",
                        getGenericClassName(), entityId);

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
            log.warn("Updating entity of type {} with ID {} is denied",
                    getGenericClassName(), entityId);
            log.trace("FullStack trace:", ade);

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    messageSource.getMessage(
                            "BASE_CONTROLLER.NOT_FOUND",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    ade
            );
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Error while updating entity of type {} with ID {}: \n {}",
                    getGenericClassName(), entityId, e.getMessage());
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

    @DeleteMapping(
        value = "/{id}",
        produces = { "application/json" }
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete entity by its ID",
        description = "TODO"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "No content: The entity was successfully deleted"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: You need to provide a bearer token"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Not found: The provided ID does not exist (or you don't have the permission to delete it)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error: Something internal went wrong while deleting the entity"
        )
    })
    public void delete(@Parameter(description = "id of the entity to delete") @PathVariable("id") BigInteger entityId) {
        log.trace("Requested to delete entity of type {} with ID {}",
                getGenericClassName(), entityId);

        try {
            Optional<S> entity = service.findOne(entityId);

            if (entity.isPresent()) {
                service.delete(entity.get());

                log.trace("Successfully deleted entity of type {} with ID {}",
                        getGenericClassName(), entityId);
            } else {
                log.error("Could not find entity of type {} with ID {}",
                        getGenericClassName(), entityId);

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
            log.warn("Deleting entity of type {} with ID {} is denied",
                    getGenericClassName(), entityId);
            log.trace("Stack trace:", ade);

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    messageSource.getMessage(
                            "BASE_CONTROLLER.NOT_FOUND",
                            null,
                            LocaleContextHolder.getLocale()
                    ),
                    ade
            );
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Error while deleting entity of type {} with ID {}: \n {}",
                    getGenericClassName(), entityId, e.getMessage());
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

    protected String getGenericClassName() {
        Class<?>[] resolvedTypeArguments = GenericTypeResolver.resolveTypeArguments(getClass(),
                BaseMetadataController.class);

        if (resolvedTypeArguments != null && resolvedTypeArguments.length == 2) {
            return resolvedTypeArguments[1].getSimpleName();
        } else {
            return null;
        }
    }
}
