package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.service.SseService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Log4j2
@RestController
@RequestMapping("/events")
public class SseController {

  @Autowired protected MessageSource messageSource;

  @Autowired private SseService service;

  @GetMapping("/subscribe")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Subscribe to server sent events",
      description =
          "Calling this endpoint will create a new server sent event emitter and return it. "
              + "The client can then listen to the events sent by the server, e.g. for updates of the validation process.",
      security = {@SecurityRequirement(name = "Bearer Authentication")})
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ok: Successfully subscribed to the events",
            content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized: You need to provide a bearer token",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal Server Error: Something internal went wrong while subscribing to the server sent events",
            content = @Content)
      })
  public SseEmitter subscribe() {
    try {
      return service.createEmitter();
    } catch (Exception e) {
      log.error("Error creating the SSE emitter: {}", e.getMessage());
      log.trace("Full stack trace: ", e);

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          messageSource.getMessage(
              "BASE_CONTROLLER.INTERNAL_SERVER_ERROR", null, LocaleContextHolder.getLocale()),
          e);
    }
  }
}
