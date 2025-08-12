package de.terrestris.mde.mde_backend.jpa;

import de.terrestris.mde.mde_backend.model.ServiceDeletion;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceDeletionRepository extends CrudRepository<ServiceDeletion, String> {
  List<ServiceDeletion> findByMetadataId(String metadataId);
}
