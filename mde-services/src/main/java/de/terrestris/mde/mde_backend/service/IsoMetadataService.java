package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import de.terrestris.mde.mde_backend.model.IsoMetadata;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IsoMetadataService extends BaseMetadataService<IsoMetadataRepository, IsoMetadata> {

  @PersistenceContext
  private EntityManager entityManager;

  // TODO: add security check
//  @PostAuthorize("hasRole('ROLE_ADMIN') or hasPermission(returnObject.orElse(null), 'READ')")
  @Transactional(readOnly = true)
  public SearchResult<IsoMetadata> search(String searchTerm, Integer offset, Integer limit) {
    SearchSession searchSession = Search.session(entityManager);

    return searchSession.search(IsoMetadata.class)
      .where(f -> f.simpleQueryString()
        .fields("data.title", "data.description")
        // title is tokenized with standard analyzer so a wildcard prefix is not necessary
        .matching(searchTerm + "*")
      )
      .fetch(offset, limit);
  }
}
