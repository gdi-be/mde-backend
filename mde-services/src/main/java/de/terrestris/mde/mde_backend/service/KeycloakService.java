package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.model.dto.UserDetails;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class KeycloakService  {

  @Autowired
  protected RealmResource keycloakRealm;

  public UserResource getUserResource(String id) {
    UsersResource kcUsers = this.keycloakRealm.users();
    return kcUsers.get(id);
  }

  /**
   * Get the effective user's realm roles.
   *
   * @return List of RoleRepresentation
   */
  public List<RoleRepresentation> getRealmRoles(String userId) {
    UserResource userResource = this.getUserResource(userId);
    List<RoleRepresentation> roles = new ArrayList<>();

    try {
      roles = userResource.roles().realmLevel().listEffective();
    } catch (Exception e) {
      log.warn("Could not get the realm roles for the user with Keycloak ID {}. " +
          "This may happen if the user is not available in Keycloak.",
        userId);
      log.trace("Full stack trace: ", e);
    }

    return roles;
  }

  public UserDetails getUserDetails(String userId) {
    var userResource = this.getUserResource(userId);
    var user = userResource.toRepresentation();
    var attributes = user.getAttributes();
    var details = new UserDetails();
    details.setStreet(attributes.getOrDefault("streetAddress", List.of("")).getFirst());
    details.setCity(attributes.getOrDefault("city", List.of("")).getFirst());
    details.setPhone(attributes.getOrDefault("telephoneNumber", List.of("")).getFirst());
    details.setFirstName(user.getFirstName());
    details.setLastName(user.getLastName());
    details.setEmail(user.getEmail());
    return details;
  }

}
