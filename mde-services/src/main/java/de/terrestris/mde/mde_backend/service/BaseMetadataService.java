package de.terrestris.mde.mde_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import de.terrestris.mde.mde_backend.model.BaseMetadata;
import de.terrestris.mde.mde_backend.jpa.BaseRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Log4j2
public abstract class BaseMetadataService<T extends BaseRepository<S, BigInteger> & JpaSpecificationExecutor<S>, S extends BaseMetadata> {

    @Autowired
    protected T repository;

    @Autowired
    @Lazy
    ObjectMapper objectMapper;

//    @PostFilter("hasRole('ROLE_ADMIN') or hasPermission(filterObject, 'READ')")
    @Transactional(readOnly = true)
    public List<S> findAll() {
        return (List<S>) repository.findAll();
    }

    // It's intentional to not have this method annotated with readOnly = true since getUserBySession might create
    // the currently logged-in user if it's not available already.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Page<S> findAll(Pageable pageable) {
        // TODO: permission handling

        return repository.findAll(pageable);
    }

//    @PostFilter("hasRole('ROLE_ADMIN') or hasPermission(filterObject, 'READ')")
    @Transactional(readOnly = true)
    public List<S> findAllBy(Specification specification) {
        return (List<S>) repository.findAll(specification);
    }

//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public Page<S> findAllBy(Specification specification, Pageable pageable) {
        return (Page<S>) repository.findAll(specification, pageable);
    }

//    @PostAuthorize("hasRole('ROLE_ADMIN') or hasPermission(returnObject.orElse(null), 'READ')")
    @Transactional(readOnly = true)
    public Optional<S> findOne(BigInteger id) {
        return repository.findById(id);
    }

//    @PostFilter("hasRole('ROLE_ADMIN') or hasPermission(filterObject, 'READ')")
    @Transactional(readOnly = true)
    public List<S> findAllById(List<BigInteger> id) {
        return (List<S>) repository.findAllById(id);
    }

//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public S create(S entity) {
        S persistedEntity = repository.save(entity);

        // TODO: permission handling

        return persistedEntity;
    }

//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public S update(BigInteger id, S entity) throws IOException {
        Optional<S> persistedEntityOpt = repository.findById(id);

//        // Ensure the created timestamp will not be overridden.
//        S persistedEntity = persistedEntityOpt.orElseThrow();
//        entity.setCreated(persistedEntity.getCreated());

        return repository.save(entity);
    }

//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public S updatePartial(S entity, JsonMergePatch patch) throws IOException, JsonPatchException {
        JsonNode entityNode = objectMapper.valueToTree(entity);
        JsonNode patchedEntityNode = patch.apply(entityNode);
        S updatedEntity = objectMapper.readerForUpdating(entity).readValue(patchedEntityNode);
        return repository.save(updatedEntity);
    }

    // @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'DELETE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(S entity) {

        // TODO: permission handling

        repository.delete(entity);
    }
}
