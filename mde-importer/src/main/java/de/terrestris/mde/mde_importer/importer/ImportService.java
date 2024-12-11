package de.terrestris.mde.mde_importer.importer;

import de.terrestris.mde.mde_backend.jpa.ClientMetadataRepository;
import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import de.terrestris.mde.mde_backend.jpa.TechnicalMetadataRepository;
import de.terrestris.mde.mde_backend.model.ClientMetadata;
import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.model.TechnicalMetadata;
import de.terrestris.mde.mde_backend.model.json.*;
import de.terrestris.mde.mde_backend.model.json.ColumnInfo.ColumnType;
import de.terrestris.mde.mde_backend.model.json.ColumnInfo.FilterType;
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

import static de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata.MetadataProfile.*;
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

  @Autowired
  private ClientMetadataRepository clientMetadataRepository;

  @Autowired
  private TechnicalMetadataRepository technicalMetadataRepository;

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
    var json = new JsonIsoMetadata();
    var client = new ClientMetadata();
    var technical = new TechnicalMetadata();
    technical.setData(new JsonTechnicalMetadata());
    client.setData(new JsonClientMetadata());
    metadata.setData(json);
    json.setContacts(new ArrayList<>());
    skipToElement(reader, "Metadaten");
    var type = reader.getAttributeValue(null, "metadatenTyp");
    if (type.equals("ISO")) {
      json.setMetadataProfile(ISO);
    }
    if (type.equals("INSPIRE")) {
      json.setMetadataProfile(INSPIRE_IDENTIFIED);
    }
    skipToElement(reader, "Titel");
    metadata.setTitle(reader.getElementText());
    client.setTitle(metadata.getTitle());
    technical.setTitle(metadata.getTitle());
    json.setTitle(metadata.getTitle());
    skipToElement(reader, "fileIdentifier");
    skipToElement(reader, "CharacterString");
    json.setFileIdentifier(reader.getElementText());
    skipToElement(reader, "contact");
    parseContact(reader, json, "contact");
    skipToElement(reader, "dateStamp");
    skipToElement(reader, "DateTime");
    json.setDateTime(Instant.parse(reader.getElementText() + "Z"));
    extractFromIso(reader, metadata, json, client, technical);
    var list = servicesMap.get(metadata.getMetadataId());
    if (list != null) {
      list.forEach(file -> addService(file, json, client.getData(), technical.getData()));
    }
    isoMetadataRepository.save(metadata);
    clientMetadataRepository.save(client);
    technicalMetadataRepository.save(technical);
  }

  private static void extractFromIso(XMLStreamReader reader, IsoMetadata metadata, JsonIsoMetadata json, ClientMetadata client, TechnicalMetadata technical) throws XMLStreamException, ParseException {
    json.setResourceConstraints(new ArrayList<>());
    skipToElement(reader, "MD_DataIdentification");
    metadata.setMetadataId(reader.getAttributeValue(null, "uuid"));
    client.setMetadataId(metadata.getMetadataId());
    technical.setMetadataId(metadata.getMetadataId());
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("MD_Metadata"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      if (reader.isStartElement() && reader.getLocalName().equals("abstract")) {
        skipToElement(reader, "CharacterString");
        json.setDescription(reader.getElementText());
      }
      if (reader.isStartElement() && reader.getLocalName().equals("pointOfContact")) {
        parseContact(reader, json, "pointOfContact");
      }
      if (reader.isStartElement() && reader.getLocalName().equals("graphicOverview")) {
        skipToElement(reader, "CharacterString");
        if (json.getPreviews() == null) {
          json.setPreviews(new ArrayList<>());
        }
        var source = new Source(null, reader.getElementText());
        json.getPreviews().add(source);
      }
      extractConformanceResult(reader, json);
      extractKeyword(reader, json);
      if (reader.isStartElement() && reader.getLocalName().equals("resourceMaintenance")) {
        skipToElement(reader, "MD_MaintenanceFrequencyCode");
        json.setMaintenanceFrequency(MD_MaintenanceFrequencyCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
      }
      extractSpatialResolution(reader, json);
      extractGraphicOverview(reader, json);
      extractCitation(reader, json);
      extractTransferOptions(reader, json);
      extractResourceConstraints(reader, json);
      extractDistributionFormat(reader, json);
      if (reader.isStartElement() && reader.getLocalName().equals("statement")) {
        skipToElement(reader, "CharacterString");
        json.setLineage(reader.getElementText());
      }
      extractDate(reader, json);
    }
  }

  private static void extractDistributionFormat(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("distributionFormat")) {
      if (json.getDistributionVersions() == null) {
        json.setDistributionVersions(new ArrayList<>());
      }
      var version = new DistributionVersion();
      json.getDistributionVersions().add(version);
      while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("distributionFormat"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        switch (reader.getLocalName()) {
          case "name":
            skipToElement(reader, "CharacterString");
            version.setName(reader.getElementText());
            break;
          case "version":
            skipToElement(reader, "CharacterString");
            version.setVersion(reader.getElementText());
            break;
          case "specification":
            skipToElement(reader, "CharacterString");
            version.setSpecification(reader.getElementText());
            break;
        }
      }
    }
  }

  private static void extractDate(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException, ParseException {
    if (reader.isStartElement() && reader.getLocalName().equals("date")) {
      skipToElement(reader, "Date");
      var date = FORMAT.parse(reader.getElementText()).toInstant();
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

  private static void extractGraphicOverview(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("graphicOverview")) {
      if (json.getPreviews() == null) {
        json.setPreviews(new ArrayList<>());
      }
      skipToElement(reader, "CharacterString");
      json.getPreviews().add(new Source("url", reader.getElementText()));
    }
  }

  private static void extractConformanceResult(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("DQ_ConformanceResult")) {
      while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("DQ_ConformanceResult"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("Boolean")) {
          json.setValid(Boolean.parseBoolean(reader.getElementText()));
          if (json.isValid() && json.getMetadataProfile().equals(INSPIRE_IDENTIFIED)) {
            json.setMetadataProfile(INSPIRE_HARMONISED);
          }
        }
      }
    }
  }

  private static void extractCitation(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException, ParseException {
    if (reader.isStartElement() && reader.getLocalName().equals("citation")) {
      while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("citation"))) {
        var citation = new Citation();
        json.setCitation(citation);
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("title")) {
          skipToElement(reader, "CharacterString");
          citation.setTitle(reader.getElementText());
        }
        if (reader.getLocalName().equals("Date")) {
          var date = FORMAT.parse(reader.getElementText()).toInstant();
          citation.setDate(date);
        }
        if (reader.getLocalName().equals("code")) {
          skipToElement(reader, "CharacterString");
          citation.setUrl(reader.getElementText());
        }
      }
    }
  }

  private static void extractSpatialResolution(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("spatialResolution")) {
      while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("spatialResolution"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("Distance")) {
          json.setResolution(Double.parseDouble(reader.getElementText()));
        }
        if (reader.getLocalName().equals("denominator")) {
          skipToElement(reader, "Integer");
          json.setScale(Integer.parseInt(reader.getElementText()));
        }
      }
    }
  }

  private static void extractTransferOptions(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("transferOptions")) {
      if (json.getContentDescriptions() == null) {
        json.setContentDescriptions(new ArrayList<>());
      }
      while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("transferOptions"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("CI_OnlineResource")) {
          skipToElement(reader, "URL");
          String url = reader.getElementText();
          skipToElement(reader, "CharacterString");
          String text = reader.getElementText();
          json.getContentDescriptions().add(new ContentDescription(url, text));
        }
      }
    }
  }

  private static void extractResourceConstraints(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("resourceConstraints")) {
      ResourceConstraints resourceConstraints = new ResourceConstraints();
      resourceConstraints.setConstraints(new ArrayList<>());
      while (!(reader.isEndElement() && reader.getLocalName().equals("resourceConstraints"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        switch (reader.getLocalName()) {
          case "accessConstraints":
          case "otherConstraints":
          case "useConstraints":
            do {
              reader.next();
            } while (!reader.isStartElement());
            Constraint constraint = new Constraint();
            resourceConstraints.getConstraints().add(constraint);
            switch (reader.getLocalName()) {
              case "MD_RestrictionCode":
                constraint.setRestrictionCode(MD_RestrictionCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
                break;
              case "Anchor":
                constraint.setUrl(reader.getAttributeValue(XLINK, "href"));
                constraint.setText(reader.getElementText());
                break;
              case "CharacterString":
                constraint.setText(reader.getElementText());
                break;
            }
        }
      }
      json.getResourceConstraints().add(resourceConstraints);
    }
  }

  private static void extractKeyword(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("keyword")) {
      do {
        reader.next();
      } while (!reader.isStartElement());
      if (json.getKeywords() == null) {
        json.setKeywords(new ArrayList<>());
      }
      var keyword = new Keyword();
      if (reader.getLocalName().equals("CharacterString")) {
        keyword.setKeyword(reader.getElementText());
      } else {
        keyword.setNamespace(reader.getAttributeValue(XLINK, "href"));
        keyword.setKeyword(reader.getElementText());
      }
      json.getKeywords().add(keyword);
    }
  }

  private static void parseContact(XMLStreamReader reader, JsonIsoMetadata json, String elementName) throws XMLStreamException {
    var contact = new Contact();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals(elementName))) {
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

  private void addService(Path file, JsonIsoMetadata json, JsonClientMetadata clientMetadata, JsonTechnicalMetadata technical) {
    log.info("Adding service from {}", file.toString());
    try {
      var service = new Service();
      if (json.getServices() == null) {
        json.setServices(new ArrayList<>());
      }
      json.getServices().add(service);
      service.setServiceDescriptions(new ArrayList<>());
      service.setDataBases(new ArrayList<>());
      service.setPublications(new ArrayList<>());
      service.setPreviews(new ArrayList<>());

      var reader = FACTORY.createXMLStreamReader(Files.newInputStream(file));
      nextElement(reader);
      while (reader.hasNext() && !reader.getLocalName().equals("IsoMetadata") && !reader.getLocalName().equals("IsoMetadataWMTS")) {
        extractMetadataFields(reader, service, clientMetadata, technical);
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

  private static void extractMetadataFields(XMLStreamReader reader, Service service, JsonClientMetadata client, JsonTechnicalMetadata technical) throws XMLStreamException, ParseException {
    client.setLayers(new ArrayList<>());
    if (service.getColumns() == null) {
      service.setColumns(new ArrayList<>());
    }
    if (technical.getCategories() == null) {
      technical.setCategories(new ArrayList<>());
    }
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
      case "Kategorie":
        var category = new Category();
        technical.getCategories().add(category);
        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("Kategorie"))) {
          reader.next();
          if (!reader.isStartElement()) {
            continue;
          }
          if (reader.getLocalName().equals("Link")) {
            category.setLink(new Link(
              reader.getAttributeValue(null, "title"),
              reader.getAttributeValue(null, "type"),
              reader.getElementText()
            ));
          }
        }
        break;
      case "SelectColumn":
        var columnInfo = new ColumnInfo();
        service.getColumns().add(columnInfo);
        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("SelectColumn"))) {
          reader.next();
          if (!reader.isStartElement()) {
            continue;
          }
          switch (reader.getLocalName()) {
            case "ColumnName":
              columnInfo.setName(reader.getElementText());
              break;
            case "ColumnAlias":
              columnInfo.setTitle(reader.getElementText());
              break;
            case "ColumnDescription":
              columnInfo.setDescription(reader.getElementText());
              break;
            case "ColumnType":
              String tp = reader.getElementText();
              if (tp.equals("T")) {
                tp = "Text";
              }
              if (tp.equals("N")) {
                tp = "Long";
              }
              columnInfo.setType(ColumnType.valueOf(tp));
              break;
            case "ColumnFilter":
              while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("ColumnFilter"))) {
                reader.next();
                if (!reader.isStartElement()) {
                  continue;
                }
                if (reader.getLocalName().equals("FilterType")) {
                  String value = reader.getElementText().trim();
                  if (value.equals("Catalogbox")) {
                    columnInfo.setFilterType(FilterType.CatalogBox);
                  } else if (!value.isBlank()) {
                    columnInfo.setFilterType(FilterType.valueOf(value));
                  }
                }
              }
              break;
          }
        }
        break;
      case "Kartenebene":
        var layer = new Layer();
        client.getLayers().add(layer);
        layer.setRelatedTopic(reader.getAttributeValue(null, "mt_klasse"));
        layer.setName(reader.getAttributeValue(null, "name"));
        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("Kartenebene"))) {
          reader.next();
          if (!reader.isStartElement()) {
            continue;
          }
          switch (reader.getLocalName()) {
            case "Titel":
              layer.setTitle(reader.getElementText());
              break;
            case "Kurzbeschreibung":
              layer.setShortDescription(reader.getElementText());
              break;
            case "Style":
              layer.setStyleName(reader.getAttributeValue(null, "name"));
              layer.setStyleTitle(reader.getAttributeValue(null, "title"));
              break;
            case "LegendImage":
              var image = new LegendImage(
                reader.getAttributeValue(null, "format"),
                reader.getAttributeValue(null, "url"),
                Integer.parseInt(reader.getAttributeValue(null, "width")),
                Integer.parseInt(reader.getAttributeValue(null, "height"))
              );
              layer.setLegendImage(image);
              break;
          }
        }
        break;
    }
    nextElement(reader);
  }

}
