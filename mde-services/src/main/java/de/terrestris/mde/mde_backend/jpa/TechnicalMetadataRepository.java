package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.TechnicalMetadata;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface TechnicalMetadataRepository extends BaseRepository<TechnicalMetadata, BigInteger>, JpaSpecificationExecutor<TechnicalMetadata>, QuerydslPredicateExecutor<TechnicalMetadata> {
}
