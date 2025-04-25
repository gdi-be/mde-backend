package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.MetadataCollection;
import jakarta.persistence.QueryHint;
import java.math.BigInteger;
import java.util.Optional;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataCollectionRepository
    extends BaseRepository<MetadataCollection, BigInteger>,
        JpaSpecificationExecutor<MetadataCollection>,
        QuerydslPredicateExecutor<MetadataCollection> {

  @QueryHints(@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
  Optional<MetadataCollection> findByMetadataId(String metadataId);
}
