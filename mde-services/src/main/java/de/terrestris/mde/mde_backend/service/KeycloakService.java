package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.model.dto.UserDetails;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class KeycloakService {

  /** Number of users fetched per page when listing all Keycloak users. */
  private static final int KEYCLOAK_USER_PAGE_SIZE = 100;

  @Autowired protected RealmResource keycloakRealm;

  public UserResource getUserResource(String id) {
    UsersResource kcUsers = this.keycloakRealm.users();
    return kcUsers.get(id);
  }

  public String getUserIdByEmail(String email) {
    var users = keycloakRealm.users().searchByEmail(email, true);
    if (users.isEmpty()) {
      return null;
    }
    return users.getFirst().getId();
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
      log.warn(
          "Could not get the realm roles for the user with Keycloak ID {}. "
              + "This may happen if the user is not available in Keycloak.",
          userId);
      log.trace("Full stack trace: ", e);
    }

    return roles;
  }

  @PreAuthorize("isAuthenticated()")
  public UserDetails getUserDetails(String userId) {
    var userResource = this.getUserResource(userId);
    var user = userResource.toRepresentation();
    return mapToUserDetails(user);
  }

  /**
   * Returns <em>all</em> users registered in the Keycloak realm.
   *
   * <p>The Keycloak Admin REST API caps a single {@code users().list()} call at 100 results by
   * default. This method works around that limitation by iterating through pages of {@value
   * #KEYCLOAK_USER_PAGE_SIZE} users until an empty page is returned, collecting every user across
   * all pages before mapping them to {@link UserDetails}.
   *
   * @return list of all users in the realm; never {@code null}
   */
  @PreAuthorize("hasRole('ADMIN')")
  public List<UserDetails> getAllUsers() {
    List<UserDetails> allUsers = new ArrayList<>();
    int first = 0;

    while (true) {
      List<UserRepresentation> page = keycloakRealm.users().list(first, KEYCLOAK_USER_PAGE_SIZE);
      if (page.isEmpty()) {
        break;
      }
      page.stream().map(this::mapToUserDetails).forEach(allUsers::add);
      first += page.size();
      if (page.size() < KEYCLOAK_USER_PAGE_SIZE) {
        break;
      }
    }

    log.debug("Retrieved {} users from Keycloak", allUsers.size());

    return allUsers;
  }

  private UserDetails mapToUserDetails(UserRepresentation user) {
    var attributes = user.getAttributes();
    var details = new UserDetails();

    if (attributes == null) {
      attributes = Collections.emptyMap();
    }

    details.setId(user.getId());
    details.setStreet(attributes.getOrDefault("streetAddress", List.of("")).getFirst());
    details.setCity(attributes.getOrDefault("city", List.of("")).getFirst());
    details.setPhone(attributes.getOrDefault("telephoneNumber", List.of("")).getFirst());
    details.setFirstName(user.getFirstName() == null ? "" : user.getFirstName());
    details.setLastName(user.getLastName() == null ? "" : user.getLastName());
    details.setEmail(user.getEmail() == null ? "" : user.getEmail());
    details.setOrganisation(attributes.getOrDefault("organisation", List.of("")).getFirst());
    return details;
  }
}
