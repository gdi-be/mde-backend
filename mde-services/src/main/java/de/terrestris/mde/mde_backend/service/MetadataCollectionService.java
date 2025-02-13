package de.terrestris.mde.mde_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchException;
import de.terrestris.mde.mde_backend.enumeration.Role;
import de.terrestris.mde.mde_backend.jpa.ClientMetadataRepository;
import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import de.terrestris.mde.mde_backend.jpa.TechnicalMetadataRepository;
import de.terrestris.mde.mde_backend.model.ClientMetadata;
import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.model.TechnicalMetadata;
import de.terrestris.mde.mde_backend.model.dto.MetadataCollection;
import de.terrestris.mde.mde_backend.model.json.JsonClientMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonTechnicalMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class MetadataCollectionService {

    @Autowired
    private ClientMetadataRepository clientMetadataRepository;

    @Autowired
    private TechnicalMetadataRepository technicalMetadataRepository;

    @Autowired
    private IsoMetadataRepository isoMetadataRepository;

    @Autowired
    @Lazy
    ObjectMapper objectMapper;

    @PostAuthorize("hasRole('ROLE_ADMIN') or hasPermission(returnObject.orElse(null), 'READ')")
    @Transactional(readOnly = true)
    public Optional<MetadataCollection> findOneByMetadataId(String metadataId) {
        MetadataCollection metadataCollection = new MetadataCollection();

        ClientMetadata clientMetadata = clientMetadataRepository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("ClientMetadata not found for metadataId: " + metadataId));
        TechnicalMetadata technicalMetadata = technicalMetadataRepository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("TechnicalMetadata not found for metadataId: " + metadataId));
        IsoMetadata isoMetadata = isoMetadataRepository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("IsoMetadata not found for metadataId: " + metadataId));

        metadataCollection.setClientMetadata(clientMetadata.getData());
        metadataCollection.setTechnicalMetadata(technicalMetadata.getData());
        metadataCollection.setIsoMetadata(isoMetadata.getData());

        return  Optional.of(metadataCollection);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String create(String title) {
        String metadataId = UUID.randomUUID().toString();

        ClientMetadata clientMetadata = new ClientMetadata(metadataId);
        TechnicalMetadata technicalMetadata = new TechnicalMetadata(metadataId);
        IsoMetadata isoMetadata = new IsoMetadata(metadataId);
        isoMetadata.getData().setTitle(title);
        isoMetadata.getData().setIdentifier(metadataId);
        isoMetadata.getData().setFileIdentifier(null);

        clientMetadataRepository.save(clientMetadata);
        technicalMetadataRepository.save(technicalMetadata);
        isoMetadataRepository.save(isoMetadata);

      return metadataId;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'CREATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String create(String title, String cloneMetadataId) throws IOException {
      String metadataId = UUID.randomUUID().toString();

      ClientMetadata clientMetadata = new ClientMetadata(metadataId);
      TechnicalMetadata technicalMetadata = new TechnicalMetadata(metadataId);
      IsoMetadata isoMetadata = new IsoMetadata(metadataId);

      IsoMetadata oIso = isoMetadataRepository.findByMetadataId(cloneMetadataId)
        .orElseThrow(() -> new NoSuchElementException("IsoMetadata not found for metadataId: " + cloneMetadataId));
      ClientMetadata oClient = clientMetadataRepository.findByMetadataId(cloneMetadataId)
        .orElseThrow(() -> new NoSuchElementException("ClientMetadata not found for metadataId: " + cloneMetadataId));
      TechnicalMetadata oTechnical = technicalMetadataRepository.findByMetadataId(cloneMetadataId)
        .orElseThrow(() -> new NoSuchElementException("TechnicalMetadata not found for metadataId: " + cloneMetadataId));

      JsonIsoMetadata clonedIsoData = objectMapper.readValue(
        objectMapper.writeValueAsString(oIso.getData()),
        new TypeReference<JsonIsoMetadata>() {}
      );
      clonedIsoData.setTitle(title);

      JsonClientMetadata clonedClientData = objectMapper.readValue(
        objectMapper.writeValueAsString(oClient.getData()),
        new TypeReference<JsonClientMetadata>() {}
      );

      JsonTechnicalMetadata clonedTechnicalData = objectMapper.readValue(
        objectMapper.writeValueAsString(oTechnical.getData()),
        new TypeReference<JsonTechnicalMetadata>() {}
      );
      clonedIsoData.setIdentifier(metadataId);
      clonedIsoData.setFileIdentifier(null);

      isoMetadata.setData(clonedIsoData);
      clientMetadata.setData(clonedClientData);
      technicalMetadata.setData(clonedTechnicalData);

      clientMetadataRepository.save(clientMetadata);
      technicalMetadataRepository.save(technicalMetadata);
      isoMetadataRepository.save(isoMetadata);

      return metadataId;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public IsoMetadata updateIsoJsonValue(String metadataId, String key, JsonNode value) throws IOException, IllegalArgumentException {
        IsoMetadata isoMetadata = isoMetadataRepository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("IsoMetadata not found for metadataId: " + metadataId));

        JsonIsoMetadata data = isoMetadata.getData();
        String jsonString = objectMapper.writeValueAsString(data);

        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
        jsonNode.replace(key, value);

        JsonIsoMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonIsoMetadata.class);
        isoMetadata.setData(updatedData);

        return isoMetadataRepository.save(isoMetadata);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ClientMetadata updateClientJsonValue(String metadataId, String key, JsonNode value) throws IOException, JsonPatchException {
        ClientMetadata clientMetadata = clientMetadataRepository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("ClientMetadata not found for metadataId: " + metadataId));

        JsonClientMetadata data = clientMetadata.getData();
        String jsonString = objectMapper.writeValueAsString(data);

        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
        jsonNode.replace(key, value);

        JsonClientMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonClientMetadata.class);
        clientMetadata.setData(updatedData);

        return clientMetadataRepository.save(clientMetadata);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TechnicalMetadata updateTechnicalJsonValue(String metadataId, String key, JsonNode value) throws IOException, JsonPatchException {
        TechnicalMetadata technicalMetadata = technicalMetadataRepository.findByMetadataId(metadataId)
            .orElseThrow(() -> new NoSuchElementException("TechnicalMetadata not found for metadataId: " + metadataId));

        JsonTechnicalMetadata data = technicalMetadata.getData();
        String jsonString = objectMapper.writeValueAsString(data);

        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(jsonString);
        jsonNode.replace(key, value);

        JsonTechnicalMetadata updatedData = objectMapper.treeToValue(jsonNode, JsonTechnicalMetadata.class);
        technicalMetadata.setData(updatedData);

        return technicalMetadataRepository.save(technicalMetadata);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void assignUser(String metadataId, String userId) {
        IsoMetadata isoMetadata = isoMetadataRepository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("IsoMetadata not found for metadataId: " + metadataId));
        ClientMetadata clientMetadata = clientMetadataRepository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("ClientMetadata not found for metadataId: " + metadataId));
        TechnicalMetadata technicalMetadata = technicalMetadataRepository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("TechnicalMetadata not found for metadataId: " + metadataId));

        isoMetadata.setResponsibleUserId(userId);
        clientMetadata.setResponsibleUserId(userId);
        technicalMetadata.setResponsibleUserId(userId);

        isoMetadataRepository.save(isoMetadata);
        clientMetadataRepository.save(clientMetadata);
        technicalMetadataRepository.save(technicalMetadata);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'UPDATE')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void assignRole(String metadataId, String role) {
        IsoMetadata isoMetadata = isoMetadataRepository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("IsoMetadata not found for metadataId: " + metadataId));
        ClientMetadata clientMetadata = clientMetadataRepository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("ClientMetadata not found for metadataId: " + metadataId));
        TechnicalMetadata technicalMetadata = technicalMetadataRepository.findByMetadataId(metadataId)
          .orElseThrow(() -> new NoSuchElementException("TechnicalMetadata not found for metadataId: " + metadataId));

        isoMetadata.setResponsibleRole(Role.valueOf(role));
        clientMetadata.setResponsibleRole(Role.valueOf(role));
        technicalMetadata.setResponsibleRole(Role.valueOf(role));

        isoMetadataRepository.save(isoMetadata);
        clientMetadataRepository.save(clientMetadata);
        technicalMetadataRepository.save(technicalMetadata);
    }

}
