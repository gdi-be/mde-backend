package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.dto.UserDetails;
import de.terrestris.mde.mde_backend.service.KeycloakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@Log4j2
@RestController
@RequestMapping("/user")
public class UserController {

  @Autowired
  private KeycloakService keycloakService;

  @GetMapping(path = "/details")
  @ResponseStatus(OK)
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  @ApiResponses(value = {
    @ApiResponse(
      responseCode = "200",
      description = "Ok: The user details could be retrieved"
    ),
    @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error: Something internal went wrong while getting the user details from keycloak"
    )
  })
  public ResponseEntity<UserDetails> getUserDetails(Authentication auth) {
    try {
      var user = auth.getName();
      return new ResponseEntity<>(keycloakService.getUserDetails(user), OK);
    } catch (Exception e) {
      log.error("Error when getting user details: {}", e.getMessage());
      log.trace("Stack trace:", e);
      return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
    }
  }

}
