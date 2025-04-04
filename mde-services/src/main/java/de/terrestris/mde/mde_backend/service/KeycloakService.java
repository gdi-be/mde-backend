package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.properties.KeycloakProperties;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Log4j2
public class KeycloakService  {

  @Autowired
  private KeycloakProperties keycloakProperties;

  @Autowired
  protected RealmResource keycloakRealm;

  public UserResource getUserResource(String id) {
    UsersResource kcUsers = this.keycloakRealm.users();
    return kcUsers.get(id);
  }

  /**
   * Get the Keycloak RoleRepresentations (for the specified client) from a user instance.
   *
   * @return List of RoleRepresentations
   */
  public List<RoleRepresentation> getKeycloakUserRoles(String userId) {
    UserResource userResource = this.getUserResource(userId);
    List<RoleRepresentation> roles = new ArrayList<>();

    try {
      roles = userResource.roles().realmLevel().listEffective();
    } catch (Exception e) {
      log.warn("Could not get the RoleMappingResource for the user with Keycloak ID {}. " +
          "This may happen if the user is not available in Keycloak.",
        userId);
      log.trace("Full stack trace: ", e);
    }

    return roles;
  }

}
