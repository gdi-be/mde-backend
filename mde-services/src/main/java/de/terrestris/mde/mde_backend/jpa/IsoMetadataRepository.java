package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.IsoMetadata;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface IsoMetadataRepository extends BaseRepository<IsoMetadata, BigInteger>, JpaSpecificationExecutor<IsoMetadata>, QuerydslPredicateExecutor<IsoMetadata> {
}
