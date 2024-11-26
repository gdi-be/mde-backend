package de.terrestris.mde.mde_importer.importer;

import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.model.json.*;
import de.terrestris.mde.mde_backend.model.json.Service.ServiceType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static de.terrestris.utils.xml.XmlUtils.nextElement;
import static de.terrestris.utils.xml.XmlUtils.skipToElement;

@Component
@Log4j2
public class ImportService {

  private static final String XLINK = "http://www.w3.org/1999/xlink";

  private static final Pattern ID_REGEXP = Pattern.compile(".*/([^/]+)");

  private static final XMLInputFactory FACTORY = XMLInputFactory.newFactory();

  private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Autowired
  private IsoMetadataRepository isoMetadataRepository;

  private final Map<String, List<Path>> servicesMap = new HashMap<>();

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri("https://localhost/auth/realms/metadata-editor").build();
  }

  public boolean importMetadata(String directory) {
    try {
      scanServices(directory);
    } catch (IOException e) {
      log.warn("Unable to scan services: {}", e.getMessage());
      log.trace("Stack trace", e);
      throw new ImportException(e);
    }
    var factory = XMLInputFactory.newFactory();
    var dir = Path.of(directory);
    try (Stream<Path> files = Files.find(dir, 1, (path, attributes) -> path.getFileName().toString().startsWith("ISO_"))) {
      files.forEach(file -> {
        log.info("Importing from {}", file);
        try {
          var reader = factory.createXMLStreamReader(Files.newInputStream(file));
          parseDatasetMetadata(reader);
        } catch (XMLStreamException | IOException | ParseException e) {
          log.warn("Error while importing metadata from file {}: {}", file, e.getMessage());
          log.trace("Stack trace", e);
          throw new ImportException(e);
        }
      });
    } catch (IOException e) {
      log.info("Error while importing metadata: {}", e.getMessage());
      log.trace("Stack trace", e);
      throw new ImportException(e);
    }
    return true;
  }

  private void scanServices(String directory) throws IOException {
    var dir = Path.of(directory);
    try (Stream<Path> files = Files.find(dir, 1, (path, attributes) -> !path.getFileName().toString().startsWith("ISO_"))) {
      files.forEach(file -> {
        if (Files.isDirectory(file)) {
          return;
        }
        log.info("Scanning service file {}", file);
        try {
          var reader = FACTORY.createXMLStreamReader(Files.newInputStream(file));
          scanServiceFile(reader, file);
        } catch (XMLStreamException | IOException e) {
          log.warn("Error scanning service file {}: {}", file, e.getMessage());
          log.trace("Stack trace", e);
          throw new ImportException(e);
        }
      });
    }
    log.info("Mappings from datasets to service files: {}", servicesMap.toString());
  }

  private void scanServiceFile(XMLStreamReader reader, Path file) throws XMLStreamException {
    try {
      skipToElement(reader, "operatesOn");
    } catch (Exception e) {
      log.warn("Unable to find operatesOn element.");
      log.trace("Stack trace", e);
      return;
    }
    var url = reader.getAttributeValue(XLINK, "href");
    var matcher = ID_REGEXP.matcher(url);
    if (matcher.matches()) {
      var id = matcher.group(1);
      if (!servicesMap.containsKey(id)) {
        servicesMap.put(id, new ArrayList<>());
      }
      servicesMap.get(id).add(file);
    } else {
      log.warn("Unable to figure out uuid from {}", url);
    }
  }

  private void parseDatasetMetadata(XMLStreamReader reader) throws XMLStreamException, ParseException {
    var metadata = new IsoMetadata();
    ArrayList<JsonIsoMetadata> data = new ArrayList<>();
    var json = new JsonIsoMetadata();
    data.add(json);
    metadata.setData(data);
    json.setContacts(new ArrayList<>());
    skipToElement(reader, "Metadaten");
    var type = reader.getAttributeValue(null, "metadatenTyp");
    json.setMetadataProfile(JsonIsoMetadata.MetadataProfile.valueOf(type));
    skipToElement(reader, "Titel");
    metadata.setTitle(reader.getElementText());
    skipToElement(reader, "fileIdentifier");
    skipToElement(reader, "CharacterString");
    json.setFileIdentifier(reader.getElementText());
    skipToElement(reader, "contact");
    parseContact(reader, json);
    skipToElement(reader, "dateStamp");
    skipToElement(reader, "DateTime");
    // TODO or use local time zone?
    json.setDateTime(Instant.parse(reader.getElementText() + "Z"));
    extractIdentificationInfo(reader, metadata, json);
    List<Path> list = servicesMap.get(metadata.getMetadataId());
    if (list != null) {
      list.forEach(file -> addService(file, json));
    }
    isoMetadataRepository.save(metadata);
  }

  private static void extractIdentificationInfo(XMLStreamReader reader, IsoMetadata metadata, JsonIsoMetadata json) throws XMLStreamException, ParseException {
    skipToElement(reader, "MD_DataIdentification");
    metadata.setMetadataId(reader.getAttributeValue(null, "uuid"));
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("MD_DataIdentification"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      if (reader.getLocalName().equals("date")) {
        skipToElement(reader, "Date");
        Instant date = FORMAT.parse(reader.getElementText()).toInstant();
        skipToElement(reader, "CI_DateTypeCode");
        switch (reader.getAttributeValue(null, "codeListValue")) {
          case "creation":
            json.setCreated(date);
            break;
          case "publication":
            json.setPublished(date);
            break;
          case "revision":
            json.setModified(date);
            break;
        }
      }
    }
  }

  private static void parseContact(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    Contact contact = new Contact();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("contact"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case "organisationName":
          skipToElement(reader, "CharacterString");
          contact.setOrganisation(reader.getElementText());
          break;
        case "voice":
          skipToElement(reader, "CharacterString");
          contact.setPhone(reader.getElementText());
          break;
        case "electronicMailAddress":
          skipToElement(reader, "CharacterString");
          contact.setEmail(reader.getElementText());
          break;
        case "CI_OnlineResource":
          skipToElement(reader, "URL");
          contact.setUrl(reader.getElementText());
          skipToElement(reader, "CI_OnLineFunctionCode");
          contact.setCode(CI_OnLineFunctionCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
          break;
        case "CI_RoleCode":
          contact.setRoleCode(CI_RoleCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
          break;
      }
    }
    json.getContacts().add(contact);
  }

  private void addService(Path file, JsonIsoMetadata json) {
    log.info("Adding service from {}", file.toString());
    try {
      Service service = new Service();
      if (json.getServices() == null) {
        json.setServices(new ArrayList<>());
      }
      json.getServices().add(service);
      service.setServiceDescriptions(new ArrayList<>());
      service.setDataBases(new ArrayList<>());
      service.setPublications(new ArrayList<>());
      service.setPreviews(new ArrayList<>());

      XMLStreamReader reader = FACTORY.createXMLStreamReader(Files.newInputStream(file));
      nextElement(reader);
      while (reader.hasNext() && !reader.getLocalName().equals("IsoMetadata") && !reader.getLocalName().equals("IsoMetadataWMTS")) {
        extractMetadataFields(reader, service);
      }

      extractIsoFields(reader, service);
    } catch (XMLStreamException | IOException | ParseException e) {
      log.warn("Unable to add service from {}: {}", file, e.getMessage());
      log.trace("Stack trace", e);
      throw new ImportException(e);
    }
  }

  private static void extractIsoFields(XMLStreamReader reader, Service service) throws XMLStreamException {
    skipToElement(reader, "fileIdentifier");
    skipToElement(reader, "CharacterString");
    var id = reader.getElementText();
    service.setFileIdentifier(id);
    skipToElement(reader, "SV_ServiceIdentification");
    id = reader.getAttributeValue(null, "uuid");
    service.setServiceIdentification(id);
    skipToElement(reader, "serviceTypeVersion");
    skipToElement(reader, "CharacterString");
    var serviceType = reader.getElementText();
    ServiceType type = null;
    if (serviceType.contains("WFS")) {
      type = ServiceType.WFS;
    }
    if (serviceType.contains("ATOM")) {
      type = ServiceType.ATOM;
    }
    if (serviceType.contains("WMS")) {
      type = ServiceType.WMS;
    }
    if (serviceType.contains("WMTS")) {
      type = ServiceType.WMTS;
    }
    service.setServiceType(type);
    skipToElement(reader, "SV_OperationMetadata");
    skipToElement(reader, "URL");
    var url = reader.getElementText();
    service.setUrl(url);
  }

  private static void extractMetadataFields(XMLStreamReader reader, Service service) throws XMLStreamException, ParseException {
    switch (reader.getLocalName()) {
      case "Dienstebeschreibung":
        var desc = new ServiceDescription(
          reader.getAttributeValue(null, "typ"),
          reader.getAttributeValue(null, "url")
        );
        service.getServiceDescriptions().add(desc);
        break;
      case "Title":
        service.setTitle(reader.getElementText());
        break;
      case "Kurzbeschreibung":
        service.setShortDescription(reader.getElementText());
        break;
      case "InhaltlicheBeschreibung":
        service.setContentDescription(reader.getElementText());
        break;
      case "TechnischeBeschreibung":
        service.setTechnicalDescription(reader.getElementText());
        break;
      case "LegendImage":
        var img = new LegendImage(
          reader.getAttributeValue(null, "format"),
          reader.getAttributeValue(null, "url"),
          Integer.parseInt(reader.getAttributeValue(null, "width")),
          Integer.parseInt(reader.getAttributeValue(null, "height"))
        );
        service.setLegendImage(img);
        break;
      case "Datengrundlage":
        var source = new Source();
        source.setType(reader.getAttributeValue(null, "Typ"));
        skipToElement(reader, "Inhalt");
        source.setContent(reader.getElementText());
        service.getDataBases().add(source);
        break;
      case "Veroeffentlichung":
        var publication = new Source();
        publication.setType(reader.getAttributeValue(null, "Typ"));
        skipToElement(reader, "Inhalt");
        publication.setContent(reader.getElementText());
        service.getPublications().add(publication);
        break;
      case "Erstellungsdatum":
        service.setCreated(FORMAT.parse(reader.getElementText()).toInstant());
        break;
      case "Revisionsdatum":
        service.setUpdated(FORMAT.parse(reader.getElementText()).toInstant());
        break;
      case "Veroeffentlichungsdatum":
        service.setPublished(FORMAT.parse(reader.getElementText()).toInstant());
        break;
      case "Vorschau":
        var preview = new Source();
        preview.setType(reader.getAttributeValue(null, "Typ"));
        skipToElement(reader, "Inhalt");
        preview.setContent(reader.getElementText());
        service.getPreviews().add(preview);
        break;
    }
    nextElement(reader);
  }

}
