package de.terrestris.mde.mde_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import de.terrestris.mde.mde_backend.utils.MdeException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@Component
@Log4j2
public class IsoGenerator {

  @Autowired
  private IsoMetadataRepository isoMetadataRepository;

  @Autowired
  private DatasetIsoGenerator datasetIsoGenerator;

  private static final Map<String, String> VALUES_MAP;

  static {
    try {
      VALUES_MAP = new ObjectMapper().readValue(new File(System.getenv("VARIABLE_FILE")), Map.class);
    } catch (IOException e) {
      throw new MdeException("Could not read the variables map file.", e);
    }
  }

  public static String replaceValues(String text) {
    for (var entry : VALUES_MAP.entrySet()) {
      text = text.replace(entry.getKey(), entry.getValue());
    }
    return text;
  }

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
