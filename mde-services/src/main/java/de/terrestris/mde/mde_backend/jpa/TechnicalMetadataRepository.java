package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.TechnicalMetadata;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface TechnicalMetadataRepository extends BaseRepository<TechnicalMetadata, BigInteger>, JpaSpecificationExecutor<TechnicalMetadata>, QuerydslPredicateExecutor<TechnicalMetadata> {

    @QueryHints(@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
    Optional<TechnicalMetadata> findByMetadataId(String metadataId);
}
