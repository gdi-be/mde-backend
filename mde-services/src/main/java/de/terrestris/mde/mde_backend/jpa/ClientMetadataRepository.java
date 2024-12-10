package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.ClientMetadata;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface ClientMetadataRepository extends BaseRepository<ClientMetadata, BigInteger>, JpaSpecificationExecutor<ClientMetadata>, QuerydslPredicateExecutor<ClientMetadata> {
}
