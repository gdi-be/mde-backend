package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.ClientMetadataRepository;
import de.terrestris.mde.mde_backend.model.ClientMetadata;
import de.terrestris.mde.mde_backend.model.json.Comment;
import de.terrestris.mde.mde_backend.model.json.JsonClientMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.NoSuchElementException;

@Service
public class ClientMetadataService extends BaseMetadataService<ClientMetadataRepository, ClientMetadata> {

  @Autowired
  protected ClientMetadataRepository repository;

  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Comment addComment(String metadataId, String text) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!authentication.isAuthenticated()) {
      throw new IllegalStateException("User must be authenticated to add a comment");
    }

    ClientMetadata clientMetadata = repository.findByMetadataId(metadataId)
      .orElseThrow(() -> new NoSuchElementException("IsoMetadata not found for metadataId: " + metadataId));

    JsonClientMetadata data = clientMetadata.getData();

    String userName = ((JwtAuthenticationToken) authentication).getTokenAttributes().get("preferred_username").toString();
    Comment comment = new Comment(text, authentication.getName(), userName);

    if (data.getComments() == null) {
      data.setComments(new ArrayList<>());
    }
    data.getComments().add(comment);

    repository.save(clientMetadata);

    return comment;
  }

  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Comment updateComment(BigInteger commentId, String text) {
    return null;
  }

  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void deleteComment(BigInteger commentId) {
    return;
  }
}
