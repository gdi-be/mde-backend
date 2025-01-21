package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

@Component
@Log4j2
public class IsoGenerator {

  @Autowired
  private IsoMetadataRepository isoMetadataRepository;

  @Autowired
  private DatasetIsoGenerator datasetIsoGenerator;

  public String generateMetadata(String metadataId) throws XMLStreamException, IOException {
    var metadata = isoMetadataRepository.findByMetadataId(metadataId);
    if (metadata.isEmpty()) {
      log.info("Metadata with ID {} is not available.", metadataId);
      return null;
    }
    var data = metadata.get().getData();
    return datasetIsoGenerator.generateDatasetMetadata(data, metadataId);
  }

}
