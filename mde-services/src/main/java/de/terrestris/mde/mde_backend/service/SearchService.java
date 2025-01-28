package de.terrestris.mde.mde_backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.log4j.Log4j2;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
public class SearchService {

  @PersistenceContext
  private EntityManager entityManager;

  @Transactional(readOnly = true)
  public void reindexAll() {
    SearchSession searchSession = Search.session(entityManager);
    log.info("Reindexing started");
    try {
      searchSession.massIndexer()
        .startAndWait();
      log.info("Reindexing finished");
    } catch (InterruptedException e) {
      throw new RuntimeException("Reindexing failed", e);
    }
  }
}
