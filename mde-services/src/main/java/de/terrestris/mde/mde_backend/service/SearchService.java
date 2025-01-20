package de.terrestris.mde.mde_backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {

  @PersistenceContext
  private EntityManager entityManager;

  @Transactional(readOnly = true)
  public void reindexAll() {
    SearchSession searchSession = Search.session(entityManager);
    try {
      searchSession.massIndexer()
        .startAndWait();
    } catch (InterruptedException e) {
      throw new RuntimeException("Reindexing failed", e);
    }
  }
}
