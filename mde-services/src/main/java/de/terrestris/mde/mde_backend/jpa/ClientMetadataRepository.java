package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.ClientMetadata;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface ClientMetadataRepository extends BaseRepository<ClientMetadata, BigInteger>, JpaSpecificationExecutor<ClientMetadata>, QuerydslPredicateExecutor<ClientMetadata> {

    @QueryHints(@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
    Optional<ClientMetadata> findByMetadataId(String metadataId);
}
