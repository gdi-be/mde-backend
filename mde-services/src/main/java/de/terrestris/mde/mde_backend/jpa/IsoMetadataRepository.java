package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.IsoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IsoMetadataRepository extends JpaRepository<IsoMetadata, Integer>, JpaSpecificationExecutor<IsoMetadata>, QuerydslPredicateExecutor<IsoMetadata>{
}
