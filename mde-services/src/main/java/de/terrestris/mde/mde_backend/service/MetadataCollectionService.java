package de.terrestris.mde.mde_backend.service;

import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchException;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.enumeration.Role;
import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.jpa.ServiceDeletionRepository;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.ServiceDeletion;
import de.terrestris.mde.mde_backend.model.Status;
import de.terrestris.mde.mde_backend.model.dto.QueryConfig;
import de.terrestris.mde.mde_backend.model.dto.UserData;
import de.terrestris.mde.mde_backend.model.json.*;
import de.terrestris.mde.mde_backend.specification.MetadataCollectionSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.imageio.ImageIO;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class MetadataCollectionService
    extends BaseMetadataService<MetadataCollectionRepository, MetadataCollection> {

  @PersistenceContext private EntityManager entityManager;

  @Autowired @Lazy ObjectMapper objectMapper;

  @Autowired KeycloakService keycloakService;

  @Autowired private ServiceDeletionRepository serviceDeletionRepository;

  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  public Page<MetadataCollection> query(QueryConfig config, Pageable pageable) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String myKeycloakId =
        ((JwtAuthenticationToken) authentication).getTokenAttributes().get("sub").toString();

    Specification<MetadataCollection> specification =
        MetadataCollectionSpecification.searchMetadata(config, myKeycloakId);

    return findAllBy(specification, pageable);
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  public Optional<MetadataCollection> findOneByMetadataId(String metadataId) {
    return repository.findByMetadataId(metadataId);
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public String create(String title) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String myKeycloakId =
        ((JwtAuthenticationToken) authentication).getTokenAttributes().get("sub").toString();
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

    String metadataId = UUID.randomUUID().toString();

    MetadataCollection metadataCollection = new MetadataCollection(metadataId);

    metadataCollection.getIsoMetadata().setTitle(title);
    metadataCollection.getIsoMetadata().setIdentifier(metadataId);

    // default values
    metadataCollection.getIsoMetadata().setMetadataProfile(MetadataProfile.ISO);
    metadataCollection.getIsoMetadata().setCrs("http://www.opengis.net/def/crs/EPSG/0/25833");
    metadataCollection.getTechnicalMetadata().setDeliveredCrs("25833");

    // User and role assignment. Set responsibleRole, ownerId, assignedUserId, teamMemberIds.
    Role roleToSet = null;
    List<String> roleNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    if (roleNames.contains("ROLE_MDEEDITOR")) {
      roleToSet = Role.MdeEditor;
    } else if (roleNames.contains("ROLE_MDEDATAOWNER")) {
      roleToSet = Role.MdeDataOwner;
    }
    if (roleToSet != null) {
      metadataCollection.setResponsibleRole(roleToSet);
    }
    metadataCollection.setAssignedUserId(myKeycloakId);
    metadataCollection.setOwnerId(myKeycloakId);
    metadataCollection.setStatus(Status.NEW);

    repository.save(metadataCollection);

    addToTeam(metadataId, myKeycloakId);

    return metadataId;
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public String clone(String title, String cloneMetadataId) throws IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String myKeycloakId =
        ((JwtAuthenticationToken) authentication).getTokenAttributes().get("sub").toString();
    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

    String metadataId = UUID.randomUUID().toString();

    MetadataCollection metadataCollection = new MetadataCollection(metadataId);

    MetadataCollection originalMetadataCollection =
        repository
            .findByMetadataId(cloneMetadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + cloneMetadataId));

    metadataCollection.setClonedFromId(cloneMetadataId);

    JsonIsoMetadata clonedIsoData =
        objectMapper.readValue(
            objectMapper.writeValueAsString(originalMetadataCollection.getIsoMetadata()),
            new TypeReference<JsonIsoMetadata>() {});
    clonedIsoData.setTitle(title);

    JsonClientMetadata clonedClientData =
        objectMapper.readValue(
            objectMapper.writeValueAsString(originalMetadataCollection.getClientMetadata()),
            new TypeReference<JsonClientMetadata>() {});

    JsonTechnicalMetadata clonedTechnicalData =
        objectMapper.readValue(
            objectMapper.writeValueAsString(originalMetadataCollection.getTechnicalMetadata()),
            new TypeReference<JsonTechnicalMetadata>() {});
    clonedIsoData.setIdentifier(metadataId);

    // remove old values according to metadatenprofil-berlin.xlsx column "neuer Jahresstand".
    // all checked properties are set to null / removed
    clonedIsoData.setFileIdentifier(null);
    clonedIsoData.setDescription(null);
    clonedIsoData.setInspireTheme(null);
    clonedIsoData.setInspireAnnexVersion(null);
    clonedIsoData.setCreated(null);
    clonedIsoData.setPublished(null);
    clonedIsoData.setModified(null);
    clonedIsoData.setValidFrom(null);
    clonedIsoData.setValidTo(null);
    clonedIsoData.setScale(null);
    clonedIsoData.setResolutions(null);
    clonedIsoData.setPreview(null);
    clonedIsoData.setTechnicalDescription(null);
    clonedIsoData.setContentDescription(null);
    clonedIsoData.setContentDescriptions(null);
    clonedIsoData.setLineage(null);
    clonedIsoData.setValid(false);
    clonedIsoData.setServices(null);
    clonedClientData.setRelatedTopics(null);
    clonedClientData.setComments(null);
    clonedTechnicalData.setDeliveredCrs(null);

    // default values
    metadataCollection.getIsoMetadata().setMetadataProfile(MetadataProfile.ISO);

    metadataCollection.setIsoMetadata(clonedIsoData);
    metadataCollection.setClientMetadata(clonedClientData);
    metadataCollection.setTechnicalMetadata(clonedTechnicalData);

    // User and role assignment. Set responsibleRole, ownerId, assignedUserId, teamMemberIds.
    Role roleToSet = null;
    List<String> roleNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    if (roleNames.contains("MdeEditor")) {
      roleToSet = Role.MdeEditor;
    } else if (roleNames.contains("MdeDataOwner")) {
      roleToSet = Role.MdeDataOwner;
    }
    if (roleToSet != null) {
      metadataCollection.setResponsibleRole(roleToSet);
    }
    metadataCollection.setAssignedUserId(myKeycloakId);
    metadataCollection.setOwnerId(myKeycloakId);
    metadataCollection.setStatus(Status.NEW);

    repository.save(metadataCollection);

    addToTeam(metadataId, myKeycloakId);

    return metadataId;
  }

  /**
   * Automatically updates the legend image data (width, height, format) of all services.
   *
   * @param data the JsonIsoMetadata object containing the services
   */
  private void updateLegendData(JsonIsoMetadata data) {
    if (data.getServices() == null) {
      return;
    }
    for (var service : data.getServices()) {
      try {
        if (service.getLegendImage() != null && service.getLegendImage().getUrl() != null) {
          URI uri = new URI(replaceValues(service.getLegendImage().getUrl()));

          try (var iis = ImageIO.createImageInputStream(uri.toURL().openStream())) {
            var readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
              var reader = readers.next();
              String formatName = reader.getFormatName();

              reader.setInput(iis);
              var img = reader.read(0);

              service.getLegendImage().setWidth(img.getWidth());
              service.getLegendImage().setHeight(img.getHeight());
              service.getLegendImage().setFormat(formatName.toLowerCase());
            }
          }
        }
      } catch (IOException | URISyntaxException | IllegalArgumentException e) {
        log.warn("Unable to determine size of legend: {}", e.getMessage());
        log.trace("Stack trace:", e);
      }
    }
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public MetadataCollection updateIsoJsonValue(String metadataId, String key, JsonNode value)
      throws IOException, IllegalArgumentException {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    JsonIsoMetadata data = metadataCollection.getIsoMetadata();
    String jsonString = objectMapper.writeValueAsString(data);

    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
    jsonNode.replace(key, value);

    JsonIsoMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonIsoMetadata.class);
    updateLegendData(updatedData);
    metadataCollection.setIsoMetadata(updatedData);
    if (metadataCollection.getStatus().equals(Status.PUBLISHED)) {
      metadataCollection.setStatus(Status.IN_EDIT);
    }
    metadataCollection.setApproved(false);

    return repository.save(metadataCollection);
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public MetadataCollection updateClientJsonValue(String metadataId, String key, JsonNode value)
      throws IOException, JsonPatchException {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    JsonClientMetadata data = metadataCollection.getClientMetadata();
    String jsonString = objectMapper.writeValueAsString(data);

    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
    jsonNode.replace(key, value);

    JsonClientMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonClientMetadata.class);
    metadataCollection.setClientMetadata(updatedData);
    if (metadataCollection.getStatus().equals(Status.PUBLISHED)) {
      metadataCollection.setStatus(Status.IN_EDIT);
    }
    metadataCollection.setApproved(false);

    return repository.save(metadataCollection);
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public MetadataCollection updateTechnicalJsonValue(String metadataId, String key, JsonNode value)
      throws IOException, JsonPatchException {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    JsonTechnicalMetadata data = metadataCollection.getTechnicalMetadata();
    String jsonString = objectMapper.writeValueAsString(data);

    ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
    jsonNode.replace(key, value);

    JsonTechnicalMetadata updatedData =
        objectMapper.treeToValue(jsonNode, JsonTechnicalMetadata.class);
    metadataCollection.setTechnicalMetadata(updatedData);
    if (metadataCollection.getStatus().equals(Status.PUBLISHED)) {
      metadataCollection.setStatus(Status.IN_EDIT);
    }
    metadataCollection.setApproved(false);

    return repository.save(metadataCollection);
  }

  // TODO: we should add permission checks that reflect the frontend behavior
  //  (compare showAssignAction in MetadataCard.svelte).
  //  We may also want to ensure that MdeDataOwner can only assign themselves
  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void assignUser(String metadataId, String userId) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));
    metadataCollection.setAssignedUserId(userId);

    // everyone who is assigned to the metadata collection should be added to the team
    addToTeam(metadataId, userId);

    // set the responsible role of the assigned user
    List<String> possibleRolesNames = Arrays.stream(Role.values()).map(Enum::name).toList();
    List<RoleRepresentation> keycloakUserRoles = keycloakService.getRealmRoles(userId);
    List<String> keycloakUserRoleNames =
        keycloakUserRoles.stream().map(RoleRepresentation::getName).toList();

    for (String keycloakUserRoleName : keycloakUserRoleNames) {
      if (possibleRolesNames.contains(keycloakUserRoleName)) {
        metadataCollection.setResponsibleRole(Role.valueOf(keycloakUserRoleName));
        break;
      }
    }
    if (metadataCollection.getStatus().equals(Status.PUBLISHED)) {
      metadataCollection.setStatus(Status.IN_EDIT);
    }

    repository.save(metadataCollection);
  }

  @PreAuthorize(
      "hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR') or hasRole('ROLE_MDEQUALITYASSURANCE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void unassignUser(String metadataId) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    metadataCollection.setAssignedUserId(null);
    repository.save(metadataCollection);
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void addToTeam(String metadataId, String userId) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    Set<String> teamMemberIds = metadataCollection.getTeamMemberIds();
    if (teamMemberIds == null) {
      teamMemberIds = new HashSet<>();
      metadataCollection.setTeamMemberIds(teamMemberIds);
    }
    teamMemberIds.add(userId);

    repository.save(metadataCollection);
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void removeFromTeam(String metadataId, String userId) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    Set<String> teamMemberIds = metadataCollection.getTeamMemberIds();

    if (teamMemberIds == null) {
      return;
    }

    metadataCollection.getTeamMemberIds().remove(userId);
    repository.save(metadataCollection);
  }

  @PreAuthorize(
      "hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR') or hasRole('ROLE_MDEQUALITYASSURANCE') or hasRole('ROLE_MDEDATAOWNER')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void assignRole(String metadataId, String role) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    String assignedUserId = metadataCollection.getAssignedUserId();

    if (assignedUserId != null) {
      this.unassignUser(metadataId);
    }
    if (metadataCollection.getStatus().equals(Status.PUBLISHED)) {
      metadataCollection.setStatus(Status.IN_EDIT);
    }

    metadataCollection.setResponsibleRole(Role.valueOf(role));
    repository.save(metadataCollection);
  }

  @PreAuthorize(
      "hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR') or hasRole('ROLE_MDEQUALITYASSURANCE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void unassignRole(String metadataId) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    metadataCollection.setResponsibleRole(null);
    repository.save(metadataCollection);
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Comment addComment(String metadataId, String text) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!authentication.isAuthenticated()) {
      throw new IllegalStateException("User must be authenticated to add a comment");
    }

    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    JsonClientMetadata data = metadataCollection.getClientMetadata();

    String userName =
        ((JwtAuthenticationToken) authentication)
            .getTokenAttributes()
            .get("preferred_username")
            .toString();
    Comment comment = new Comment(text, authentication.getName(), userName);

    if (data.getComments() == null) {
      data.setComments(new ArrayList<>());
    }
    data.getComments().add(comment);

    repository.save(metadataCollection);

    return comment;
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void deleteComment(String metadataId, UUID commentId) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    JsonClientMetadata data = metadataCollection.getClientMetadata();

    if (data.getComments() == null) {
      return;
    }

    data.getComments().removeIf(comment -> comment.getId().equals(commentId));

    repository.save(metadataCollection);
  }

  @PreAuthorize(
      "hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR') or hasRole('ROLE_MDEQUALITYASSURANCE') or hasRole('ROLE_MDEDATAOWNER')")
  @Transactional(readOnly = true)
  public List<UserData> getTeamWithRoles(String metadataId) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    Set<String> teamMemberIds = metadataCollection.getTeamMemberIds();

    if (teamMemberIds == null) {
      return Collections.emptyList();
    }

    return teamMemberIds.stream()
        .map(
            userId -> {
              UserData userData = new UserData();
              userData.setKeycloakId(userId);

              // set the responsible role of the assigned user
              List<String> possibleRolesNames =
                  Arrays.stream(Role.values()).map(Enum::name).toList();
              List<RoleRepresentation> keycloakUserRoles = keycloakService.getRealmRoles(userId);
              List<String> keycloakUserRoleNames =
                  keycloakUserRoles.stream().map(RoleRepresentation::getName).toList();

              for (String keycloakUserRoleName : keycloakUserRoleNames) {
                if (possibleRolesNames.contains(keycloakUserRoleName)) {
                  userData.setRole(keycloakUserRoleName);
                  break;
                }
              }

              UserResource userResource = keycloakService.getUserResource(userId);
              if (userResource != null) {
                userData.setDisplayName(
                    userResource.toRepresentation().getFirstName()
                        + " "
                        + userResource.toRepresentation().getLastName());
              }

              return userData;
            })
        .toList();
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEQUALITYASSURANCE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void setApprovalState(String metadataId, Boolean approved) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    metadataCollection.setApproved(approved);
    repository.save(metadataCollection);
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public String updateLayers(String metadataId, String serviceIdentification, List<Layer> layers) {
    MetadataCollection metadataCollection =
        repository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "MetadataCollection not found for metadataId: " + metadataId));

    JsonClientMetadata clientMetadata = metadataCollection.getClientMetadata();
    Map<String, List<Layer>> layerMap = clientMetadata.getLayers();

    if (layerMap == null) {
      layerMap = new HashMap<>();
    }

    layerMap.put(serviceIdentification, layers);
    clientMetadata.setLayers(layerMap);
    if (metadataCollection.getStatus().equals(Status.PUBLISHED)) {
      metadataCollection.setStatus(Status.IN_EDIT);
    }

    repository.save(metadataCollection);

    return serviceIdentification;
  }

  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void prepareServiceDeletion(String metadataId, String fileIdentifier) {
    var serviceDeletion = new ServiceDeletion(null, metadataId, fileIdentifier);
    serviceDeletionRepository.save(serviceDeletion);
  }
}
