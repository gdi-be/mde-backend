package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.MetadataCollection;
import jakarta.persistence.QueryHint;
import java.math.BigInteger;
import java.util.Optional;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataCollectionRepository
    extends BaseRepository<MetadataCollection, BigInteger>,
        JpaSpecificationExecutor<MetadataCollection>,
        QuerydslPredicateExecutor<MetadataCollection> {

  @QueryHints(@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
  Optional<MetadataCollection> findByMetadataId(String metadataId);

  @Query(
      value =
          "SELECT mc.*, (mc.iso_metadata->>'title') as title FROM metadata_collection mc WHERE mc.iso_metadata->>'title' = :title LIMIT 1",
      nativeQuery = true)
  Optional<MetadataCollection> findByIsoMetadataTitle(@Param("title") String title);

  @Query(
      value =
          """
      SELECT mc.*, (mc.iso_metadata->>'title') as title
      FROM metadata_collection mc
      WHERE EXISTS (
          SELECT 1
          FROM jsonb_array_elements(mc.iso_metadata->'services') s
          WHERE s->>'serviceType' = :type
            AND s->>'workspace' = :workspace
      ) LIMIT 1
      """,
      nativeQuery = true)
  Optional<MetadataCollection> findByServiceTypeAndWorkspace(
      @Param("type") String type, @Param("workspace") String workspace);
}
