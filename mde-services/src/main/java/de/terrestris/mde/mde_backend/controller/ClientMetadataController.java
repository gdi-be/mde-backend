package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.model.json.Comment;
import de.terrestris.mde.mde_backend.service.ClientMetadataService;
import de.terrestris.mde.mde_backend.service.IsoMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;

import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@RequestMapping("/metadata/client")
public class ClientMetadataController extends BaseMetadataController<IsoMetadataService, IsoMetadata> {

  @Autowired
  private ClientMetadataService service;

  @PostMapping("/comment/{metadataId}")
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

  @PatchMapping("/comment/{commentId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Ok: The Comment was successfully updated"
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error: Something internal went wrong while updating the Comment"
    )
  })
  public ResponseEntity<Comment> updateComment(@RequestBody String commentText, @PathVariable("commentId") BigInteger commentId) {
    try {
      Comment comment = service.updateComment(commentId, commentText);
      return ResponseEntity.status(OK).body(comment);
    } catch (Exception e) {
      log.error("Error while updating comment with id {}: {}", commentId, e.getMessage());
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

  @DeleteMapping("/comment/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  public ResponseEntity<Void> deleteComment(@PathVariable("commentId") BigInteger commentId) {
    try {
      service.deleteComment(commentId);
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
