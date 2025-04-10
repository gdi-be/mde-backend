package de.terrestris.mde.mde_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchException;
import de.terrestris.mde.mde_backend.enumeration.Role;
import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.dto.QueryConfig;
import de.terrestris.mde.mde_backend.model.dto.UserData;
import de.terrestris.mde.mde_backend.model.json.Comment;
import de.terrestris.mde.mde_backend.model.json.JsonClientMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonTechnicalMetadata;
import de.terrestris.mde.mde_backend.specification.MetadataCollectionSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
public class MetadataCollectionService extends BaseMetadataService<MetadataCollectionRepository, MetadataCollection> {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    @Lazy
    ObjectMapper objectMapper;

    @Autowired
    KeycloakService keycloakService;

    @Transactional(readOnly = true)
    public Page<MetadataCollection> query(QueryConfig config, Pageable pageable) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String myKeycloakId = ((JwtAuthenticationToken) authentication).getTokenAttributes().get("sub").toString();

      Specification<MetadataCollection> specification = MetadataCollectionSpecification.searchMetadata(config, myKeycloakId);

      return findAllBy(specification, pageable);
    }

    @PostAuthorize("hasRole('ROLE_ADMIN') or hasPermission(returnObject.orElse(null), 'READ')")
    @Transactional(readOnly = true)
    public Optional<MetadataCollection> findOneByMetadataId(String metadataId) {
        return repository.findByMetadataId(metadataId);
    }

    @Transactional(readOnly = true)
    public SearchResult<MetadataCollection> search(String searchTerm, Integer offset, Integer limit) {
      SearchSession searchSession = Search.session(entityManager);

      return searchSession.search(MetadataCollection.class)
        .where(f -> f.simpleQueryString()
          .fields("isoMetadata.title")
          .matching(searchTerm + "*")
        )
        .fetch(offset, limit);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String create(String title) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String myKeycloakId = ((JwtAuthenticationToken) authentication).getTokenAttributes().get("sub").toString();
      Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

      String metadataId = UUID.randomUUID().toString();

      MetadataCollection metadataCollection = new MetadataCollection(metadataId);

      metadataCollection.getIsoMetadata().setTitle(title);
      metadataCollection.getIsoMetadata().setIdentifier(metadataId);

      // User and role assignment. Set responsibleRole, ownerId, assignedUserId, teamMemberIds.
      Role roleToSet = null;
      List<String> roleNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
      if (roleNames.contains("Editor")) {
        roleToSet = Role.Editor;
      } else if (roleNames.contains("DataOwner")) {
        roleToSet = Role.DataOwner;
      }
      if (roleToSet != null) {
        metadataCollection.setResponsibleRole(roleToSet);
      }
      metadataCollection.setAssignedUserId(myKeycloakId);
      metadataCollection.setOwnerId(myKeycloakId);

      repository.save(metadataCollection);

      addToTeam(metadataId, myKeycloakId);

      return metadataId;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String create(String title, String cloneMetadataId) throws IOException {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String myKeycloakId = ((JwtAuthenticationToken) authentication).getTokenAttributes().get("sub").toString();
      Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

      String metadataId = UUID.randomUUID().toString();

      MetadataCollection metadataCollection = new MetadataCollection(metadataId);

      MetadataCollection originalMetadataCollection = repository.findByMetadataId(cloneMetadataId)
        .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + cloneMetadataId));

      JsonIsoMetadata clonedIsoData = objectMapper.readValue(
        objectMapper.writeValueAsString(originalMetadataCollection.getIsoMetadata()),
        new TypeReference<JsonIsoMetadata>() {}
      );
      clonedIsoData.setTitle(title);

      JsonClientMetadata clonedClientData = objectMapper.readValue(
        objectMapper.writeValueAsString(originalMetadataCollection.getClientMetadata()),
        new TypeReference<JsonClientMetadata>() {}
      );

      JsonTechnicalMetadata clonedTechnicalData = objectMapper.readValue(
        objectMapper.writeValueAsString(originalMetadataCollection.getTechnicalMetadata()),
        new TypeReference<JsonTechnicalMetadata>() {}
      );
      clonedIsoData.setIdentifier(metadataId);
      clonedIsoData.setFileIdentifier(null);

      metadataCollection.setIsoMetadata(clonedIsoData);
      metadataCollection.setClientMetadata(clonedClientData);
      metadataCollection.setTechnicalMetadata(clonedTechnicalData);

      // User and role assignment. Set responsibleRole, ownerId, assignedUserId, teamMemberIds.
      Role roleToSet = null;
      List<String> roleNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
      if (roleNames.contains("Editor")) {
        roleToSet = Role.Editor;
      } else if (roleNames.contains("DataOwner")) {
        roleToSet = Role.DataOwner;
      }
      if (roleToSet != null) {
        metadataCollection.setResponsibleRole(roleToSet);
      }
      metadataCollection.setAssignedUserId(myKeycloakId);
      metadataCollection.setOwnerId(myKeycloakId);

      repository.save(metadataCollection);

      addToTeam(metadataId, myKeycloakId);

      return metadataId;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MetadataCollection updateIsoJsonValue(String metadataId, String key, JsonNode value) throws IOException, IllegalArgumentException {
      MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

        JsonIsoMetadata data = metadataCollection.getIsoMetadata();
        String jsonString = objectMapper.writeValueAsString(data);

        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
        jsonNode.replace(key, value);

        JsonIsoMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonIsoMetadata.class);
        metadataCollection.setIsoMetadata(updatedData);

        return repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MetadataCollection updateClientJsonValue(String metadataId, String key, JsonNode value) throws IOException, JsonPatchException {
        MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

        JsonClientMetadata data = metadataCollection.getClientMetadata();
        String jsonString = objectMapper.writeValueAsString(data);

        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
        jsonNode.replace(key, value);

        JsonClientMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonClientMetadata.class);
        metadataCollection.setClientMetadata(updatedData);

        return repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public MetadataCollection updateTechnicalJsonValue(String metadataId, String key, JsonNode value) throws IOException, JsonPatchException {
        MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

        JsonTechnicalMetadata data = metadataCollection.getTechnicalMetadata();
        String jsonString = objectMapper.writeValueAsString(data);

        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
        jsonNode.replace(key, value);

        JsonTechnicalMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonTechnicalMetadata.class);
        metadataCollection.setTechnicalMetadata(updatedData);

        return repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void assignUser(String metadataId, String userId) {
        MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));
        metadataCollection.setAssignedUserId(userId);

        // everyone who is assigned to the metadata collection should be added to the team
        addToTeam(metadataId, userId);

        // set the responsible role of the assigned user
        List<String> possibleRolesNames = Arrays.stream(Role.values()).map(Enum::name).toList();
        List<RoleRepresentation> keycloakUserRoles = keycloakService.getRealmRoles(userId);
        List<String> keycloakUserRoleNames = keycloakUserRoles.stream().map(RoleRepresentation::getName).toList();

        for (String keycloakUserRoleName : keycloakUserRoleNames) {
          if (possibleRolesNames.contains(keycloakUserRoleName)) {
              metadataCollection.setResponsibleRole(Role.valueOf(keycloakUserRoleName));
            break;
          }
        }

        repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void unassignUser(String metadataId) {
        MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

        metadataCollection.setAssignedUserId(null);
        repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addToTeam(String metadataId, String userId) {
        MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

        Set<String> teamMemberIds = metadataCollection.getTeamMemberIds();
        if (teamMemberIds == null) {
          teamMemberIds = new HashSet<>();
          metadataCollection.setTeamMemberIds(teamMemberIds);
        }
        teamMemberIds.add(userId);

        repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeFromTeam(String metadataId, String userId) {
        MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

        Set<String> teamMemberIds = metadataCollection.getTeamMemberIds();

        if (teamMemberIds == null) {
          return;
        }

        metadataCollection.getTeamMemberIds().remove(userId);
        repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void assignRole(String metadataId, String role) {
      MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
        .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

      String assignedUserId = metadataCollection.getAssignedUserId();
      List<RoleRepresentation> keycloakUserRoles = keycloakService.getRealmRoles(assignedUserId);
      List<String> keycloakUserRoleNames = keycloakUserRoles.stream().map(RoleRepresentation::getName).toList();

      if (!keycloakUserRoleNames.contains(role)) {
        this.unassignUser(metadataId);
      }

      metadataCollection.setResponsibleRole(Role.valueOf(role));
        repository.save(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void unassignRole(String metadataId) {
        MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

        metadataCollection.setResponsibleRole(null);
        repository.save(metadataCollection);
    }

  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Comment addComment(String metadataId, String text) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!authentication.isAuthenticated()) {
      throw new IllegalStateException("User must be authenticated to add a comment");
    }

    MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
      .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

    JsonClientMetadata data = metadataCollection.getClientMetadata();

    String userName = ((JwtAuthenticationToken) authentication).getTokenAttributes().get("preferred_username").toString();
    Comment comment = new Comment(text, authentication.getName(), userName);

    if (data.getComments() == null) {
      data.setComments(new ArrayList<>());
    }
    data.getComments().add(comment);

    repository.save(metadataCollection);

    return comment;
  }

  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'DELETE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void deleteComment(String metadataId, UUID commentId) {
    MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
      .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

    JsonClientMetadata data = metadataCollection.getClientMetadata();

    if (data.getComments() == null) {
      return;
    }

    data.getComments().removeIf(comment -> comment.getId().equals(commentId));

    repository.save(metadataCollection);
  }

  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'READ')")
  @Transactional(readOnly = true)
  public List<UserData> getTeamWithRoles(String metadataId) {
    MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
      .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

    Set<String> teamMemberIds = metadataCollection.getTeamMemberIds();

    if (teamMemberIds == null) {
      return Collections.emptyList();
    }

    return teamMemberIds.stream()
      .map(userId -> {
        UserData userData = new UserData();
        userData.setKeycloakId(userId);


        // set the responsible role of the assigned user
        List<String> possibleRolesNames = Arrays.stream(Role.values()).map(Enum::name).toList();
        List<RoleRepresentation> keycloakUserRoles = keycloakService.getRealmRoles(userId);
        List<String> keycloakUserRoleNames = keycloakUserRoles.stream().map(RoleRepresentation::getName).toList();

        for (String keycloakUserRoleName : keycloakUserRoleNames) {
          if (possibleRolesNames.contains(keycloakUserRoleName)) {
            userData.setRole(keycloakUserRoleName);
            break;
          }
        }

        UserResource userResource = keycloakService.getUserResource(userId);
        if (userResource != null) {
          userData.setDisplayName(userResource.toRepresentation().getFirstName() + " " + userResource.toRepresentation().getLastName());
        }

        return userData;
      })
      .toList();
  }

  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void setApprovalState(String metadataId, Boolean approved) {
    MetadataCollection metadataCollection = repository.findByMetadataId(metadataId)
      .orElseThrow(() -> new NoSuchElementException("MetadataCollection not found for metadataId: " + metadataId));

    metadataCollection.setApproved(approved);
    repository.save(metadataCollection);
  }

}
