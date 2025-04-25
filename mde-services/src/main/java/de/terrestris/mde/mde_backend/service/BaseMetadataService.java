package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.BaseRepository;
import de.terrestris.mde.mde_backend.model.BaseMetadata;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
public abstract class BaseMetadataService<
    T extends BaseRepository<S, BigInteger> & JpaSpecificationExecutor<S>, S extends BaseMetadata> {

  @Autowired protected T repository;

  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  public Page<S> findAllBy(Specification<S> specification, Pageable pageable) {
    return repository.findAll(specification, pageable);
  }

  // TODO Remove
  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public S update(BigInteger id, S entity) throws IOException {
    Optional<S> persistedEntityOpt = repository.findById(id);
    return repository.save(entity);
  }

  // TODO Check
  @PreAuthorize("isAuthenticated()")
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void delete(S entity) {
    repository.delete(entity);
  }
}
