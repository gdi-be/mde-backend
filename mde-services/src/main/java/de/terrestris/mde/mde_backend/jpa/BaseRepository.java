package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.BaseMetadata;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.hibernate.jpa.AvailableHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;

@NoRepositoryBean
public interface BaseRepository<T, ID>
    extends CrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {

  @QueryHints(@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
  @Override
  @NonNull
  List<T> findAll();

  /**
   * Returns a {@link Page} of entities without checking any permissions.
   *
   * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must
   *     not be {@literal null}.
   * @return A page of entities.
   */
  @QueryHints(@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
  @Override
  @NonNull
  Page<T> findAll(@NonNull Pageable pageable);

  @QueryHints(@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true"))
  @Override
  @NonNull
  Iterable<T> findAllById(@NonNull Iterable<ID> ids);

  <S extends BaseMetadata> Optional<S> findByMetadataId(String metadataId);
}
