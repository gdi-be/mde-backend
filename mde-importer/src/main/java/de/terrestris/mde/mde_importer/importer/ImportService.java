package de.terrestris.mde.mde_importer.importer;

import static de.terrestris.mde.mde_backend.service.IsoGenerator.TERMS_OF_USE_MAP;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.XLINK;
import static de.terrestris.utils.xml.XmlUtils.*;

import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.Status;
import de.terrestris.mde.mde_backend.model.json.*;
import de.terrestris.mde.mde_backend.model.json.ColumnInfo.ColumnType;
import de.terrestris.mde.mde_backend.model.json.ColumnInfo.FilterType;
import de.terrestris.mde.mde_backend.model.json.Service.ServiceType;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_RoleCode;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_MaintenanceFrequencyCode;
import de.terrestris.mde.mde_backend.service.GeneratorUtils;
import de.terrestris.mde.mde_backend.service.KeycloakService;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ImportService {

  private static final Pattern ID_REGEXP = Pattern.compile(".*/([^/]+)");

  private static final Pattern PHONE_REGEXP = Pattern.compile("([+][\\d-]+)");

  private static final XMLInputFactory FACTORY = XMLInputFactory.newFactory();

  private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final Set<String> AUTO_KEYWORDS =
      Set.of(
          "inspireidentifiziert",
          "open data",
          "opendata",
          "Sachdaten",
          "Karten",
          "Geodaten",
          "Berlin",
          "infoFeatureAccessService",
          "infoMapAccessService");

  static {
    FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  @Autowired private MetadataCollectionRepository metadataCollectionRepository;

  @Autowired private KeycloakService keycloakService;

  @Value("${mde.assign-users-on-import:false}")
  private boolean assignUsersOnImport;

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
    try (Stream<Path> files =
        Files.find(
            dir, 1, (path, attributes) -> path.getFileName().toString().startsWith("ISO_"))) {
      files.forEach(
          file -> {
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
    try (Stream<Path> files =
        Files.find(
            dir, 1, (path, attributes) -> !path.getFileName().toString().startsWith("ISO_"))) {
      files.forEach(
          file -> {
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

  private void parseDatasetMetadata(XMLStreamReader reader)
      throws XMLStreamException, ParseException {
    var metadataCollection = new MetadataCollection();
    metadataCollection.setStatus(Status.PUBLISHED);
    var isoMetadata = new JsonIsoMetadata();
    var technicalMetadata = new JsonTechnicalMetadata();
    var clientMetadata = new JsonClientMetadata();
    clientMetadata.setLayers(new HashMap<>());

    metadataCollection.setTechnicalMetadata(technicalMetadata);
    metadataCollection.setClientMetadata(clientMetadata);
    metadataCollection.setIsoMetadata(isoMetadata);
    isoMetadata.setContacts(new ArrayList<>());
    skipToElement(reader, "Metadaten");
    var type = reader.getAttributeValue(null, "metadatenTyp");
    if (type.equals("ISO")) {
      isoMetadata.setMetadataProfile(MetadataProfile.ISO);
    }
    if (type.equals("INSPIRE")) {
      isoMetadata.setMetadataProfile(MetadataProfile.INSPIRE_IDENTIFIED);
    }
    skipToElement(reader, "fileIdentifier");
    skipToElement(reader, "CharacterString");
    isoMetadata.setFileIdentifier(reader.getElementText());
    skipToElement(reader, "contact");
    var contact = parseContact(reader, "contact");
    if (isoMetadata.getContacts() == null) {
      isoMetadata.setContacts(new ArrayList<>());
    }
    isoMetadata.getContacts().add(contact);
    skipToElement(reader, "dateStamp");
    skipToElement(reader, "DateTime");
    isoMetadata.setDateTime(Instant.parse(reader.getElementText() + "Z"));
    extractCoordinateSystem(reader, isoMetadata);
    extractFromIso(reader, metadataCollection);
    var list = servicesMap.get(metadataCollection.getMetadataId());
    if (list != null) {
      list.forEach(file -> addService(file, metadataCollection));
    }
    // replace content and technical description with values from a service if not set
    if (isoMetadata.getServices() != null) {
      for (var service : isoMetadata.getServices()) {
        if (isoMetadata.getContentDescription() == null
            && service.getContentDescription() != null) {
          isoMetadata.setContentDescription(service.getContentDescription());
        }
        if (isoMetadata.getTechnicalDescription() == null
            && service.getTechnicalDescription() != null) {
          isoMetadata.setTechnicalDescription(service.getTechnicalDescription());
        }
      }
    }
    if (isoMetadata.getTermsOfUseId() == null) {
      log.info(
          "Terms of use could not be mapped for {}, using standard.",
          metadataCollection.getMetadataId());
      isoMetadata.setTermsOfUseId(BigInteger.ONE);
    }

    if (assignUsersOnImport) {
      var ids = new HashSet<String>();
      for (var c : isoMetadata.getContacts()) {
        var id = keycloakService.getUserIdByEmail(c.getEmail());
        if (id != null) {
          ids.add(id);
        }
      }
      for (var c : isoMetadata.getPointsOfContact()) {
        var id = keycloakService.getUserIdByEmail(c.getEmail());
        if (id != null) {
          ids.add(id);
        }
      }
      if (!ids.isEmpty()) {
        metadataCollection.setOwnerId(ids.iterator().next());
        metadataCollection.setTeamMemberIds(ids);
      }
    }

    metadataCollectionRepository.save(metadataCollection);
  }

  private static void extractCoordinateSystem(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException {
    while (reader.hasNext()
        && !(reader.isStartElement() && reader.getLocalName().equals("identificationInfo"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      if (reader.getLocalName().equals("referenceSystemInfo")) {
        skipToElement(reader, "code");
        skipToElement(reader, "CharacterString");
        json.setCrs(reader.getElementText());
      }
    }
  }

  private static void extractFromIso(XMLStreamReader reader, MetadataCollection metadataCollection)
      throws XMLStreamException, ParseException {
    skipToElement(reader, "MD_DataIdentification");
    metadataCollection.setMetadataId(reader.getAttributeValue(null, "uuid"));
    var isoMetadata = metadataCollection.getIsoMetadata();
    isoMetadata.setIdentifier(metadataCollection.getMetadataId());
    isoMetadata.setPointsOfContact(new ArrayList<>());
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals("MD_Metadata"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      if (reader.isStartElement() && reader.getLocalName().equals("title")) {
        skipToElement(reader, "CharacterString");
        isoMetadata.setTitle(reader.getElementText());
      }
      if (reader.isStartElement() && reader.getLocalName().equals("abstract")) {
        skipToElement(reader, "CharacterString");
        isoMetadata.setDescription(reader.getElementText());
      }
      if (reader.isStartElement() && reader.getLocalName().equals("pointOfContact")) {
        var contact = parseContact(reader, "pointOfContact");
        isoMetadata.getPointsOfContact().add(contact);
      }
      if (reader.isStartElement() && reader.getLocalName().equals("topicCategory")) {
        skipToElement(reader, "MD_TopicCategoryCode");
        if (isoMetadata.getTopicCategory() == null) {
          isoMetadata.setTopicCategory(new ArrayList<>());
        }
        isoMetadata.getTopicCategory().add(reader.getElementText());
      }
      if (reader.isStartElement() && reader.getLocalName().equals("graphicOverview")) {
        skipToElement(reader, "CharacterString");
        isoMetadata.setPreview(reader.getElementText());
      }
      extractConformanceResult(reader, isoMetadata);
      extractKeyword(reader, isoMetadata);
      if (reader.isStartElement() && reader.getLocalName().equals("resourceMaintenance")) {
        skipToElement(reader, "MD_MaintenanceFrequencyCode");
        isoMetadata.setMaintenanceFrequency(
            MD_MaintenanceFrequencyCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
      }
      extractExtent(reader, isoMetadata);
      extractSpatialResolution(reader, isoMetadata);
      extractGraphicOverview(reader, isoMetadata);
      extractTransferOptions(reader, isoMetadata);
      extractResourceConstraints(reader, isoMetadata);
      extractDistributionFormat(reader, isoMetadata);

      if (reader.isStartElement() && reader.getLocalName().equals("statement")) {
        skipToElement(reader, "CharacterString");
        List<Lineage> lineageList = new ArrayList<>();
        Lineage lineage = new Lineage();
        lineage.setTitle(reader.getElementText());
        lineageList.add(lineage);
        isoMetadata.setLineage(lineageList);
      }
      extractDate(reader, isoMetadata);
    }
  }

  private static void extractExtent(XMLStreamReader reader, JsonIsoMetadata metadata)
      throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("EX_GeographicBoundingBox")) {
      var extent = new Extent();
      metadata.setExtent(extent);
      while (reader.hasNext()
          && !(reader.isEndElement() && reader.getLocalName().equals("EX_GeographicBoundingBox"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        switch (reader.getLocalName()) {
          case "westBoundLongitude":
            skipToElement(reader, "Decimal");
            extent.setMinx(Double.parseDouble(reader.getElementText()));
            break;
          case "eastBoundLongitude":
            skipToElement(reader, "Decimal");
            extent.setMaxx(Double.parseDouble(reader.getElementText()));
            break;
          case "southBoundLatitude":
            skipToElement(reader, "Decimal");
            extent.setMiny(Double.parseDouble(reader.getElementText()));
            break;
          case "northBoundLatitude":
            skipToElement(reader, "Decimal");
            extent.setMaxy(Double.parseDouble(reader.getElementText()));
            break;
        }
      }
    }
  }

  private static void extractDistributionFormat(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("distributionFormat")) {
      if (json.getDistributionVersions() == null) {
        json.setDistributionVersions(new ArrayList<>());
      }
      var version = new DistributionVersion();
      json.getDistributionVersions().add(version);
      while (reader.hasNext()
          && !(reader.isEndElement() && reader.getLocalName().equals("distributionFormat"))) {
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

  private static void extractDate(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException, ParseException {
    if (reader.isStartElement() && reader.getLocalName().equals("date")) {
      skipToElement(reader, "Date");
      var text = reader.getElementText();
      if (!text.endsWith("Z")) {
        text += "Z";
      }
      var date = FORMAT.parse(text).toInstant();
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

  private static void extractGraphicOverview(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("graphicOverview")) {
      skipToElement(reader, "CharacterString");
      json.setPreview(reader.getElementText());
    }
  }

  private static void extractConformanceResult(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("DQ_ConformanceResult")) {
      while (reader.hasNext()
          && !(reader.isEndElement() && reader.getLocalName().equals("DQ_ConformanceResult"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("Boolean")) {
          json.setValid(Boolean.parseBoolean(reader.getElementText()));
          if (json.isValid()
              && json.getMetadataProfile().equals(MetadataProfile.INSPIRE_IDENTIFIED)) {
            json.setMetadataProfile(MetadataProfile.INSPIRE_HARMONISED);
          }
        }
      }
    }
  }

  private static void extractSpatialResolution(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("spatialResolution")) {
      while (reader.hasNext()
          && !(reader.isEndElement() && reader.getLocalName().equals("spatialResolution"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("Distance")) {
          if (json.getResolutions() == null) {
            json.setResolutions(new ArrayList<>());
          }
          json.getResolutions().add(Double.parseDouble(reader.getElementText()));
        }
        if (reader.getLocalName().equals("denominator")) {
          skipToElement(reader, "Integer");
          json.setScale(Integer.parseInt(reader.getElementText()));
        }
      }
    }
  }

  private static void extractTransferOptions(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("transferOptions")) {
      if (json.getContentDescriptions() == null) {
        json.setContentDescriptions(new ArrayList<>());
      }
      while (reader.hasNext()
          && !(reader.isEndElement() && reader.getLocalName().equals("transferOptions"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("CI_OnlineResource")) {
          var description = new ContentDescription();
          while (reader.hasNext()
              && !(reader.isEndElement() && reader.getLocalName().equals("CI_OnlineResource"))) {
            reader.next();
            if (!reader.isStartElement()) {
              continue;
            }
            switch (reader.getLocalName()) {
              case "URL":
                description.setUrl(reader.getElementText());
                break;
              case "CharacterString":
                description.setDescription(reader.getElementText());
                break;
              case "CI_OnLineFunctionCode":
                String code = reader.getAttributeValue(null, "codeListValue");
                description.setCode(CI_OnLineFunctionCode.valueOf(code));
                break;
            }
          }
          if (description.getDescription() != null
              && description.getDescription().trim().equals("Inhaltliche Beschreibung")) {
            json.setContentDescription(description.getUrl());
          } else if (description.getDescription() != null
              && description.getDescription().trim().equals("Technische Beschreibung")) {
            json.setTechnicalDescription(description.getUrl());
          } else {
            json.getContentDescriptions().add(description);
          }
        }
      }
    }
  }

  private static void extractResourceConstraints(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("resourceConstraints")) {
      while (!(reader.isEndElement() && reader.getLocalName().equals("resourceConstraints"))) {
        reader.next();
        if (!reader.isStartElement()
            || reader.getLocalName().equals("MD_LegalConstraints")
            || reader.getLocalName().equals("useLimitation")
            || reader.getLocalName().equals("CharacterString")) {
          continue;
        }
        skipToOneOf(reader, "Anchor", "CharacterString", "MD_RestrictionCode");
        if (reader.getLocalName().equals("CharacterString")) {
          String text = reader.getElementText();
          if (TERMS_OF_USE_MAP.get(text) != null) {
            json.setTermsOfUseId(BigInteger.valueOf(TERMS_OF_USE_MAP.get(text).getId()));
          }
        }
      }
    }
  }

  private static void extractKeyword(XMLStreamReader reader, JsonIsoMetadata json)
      throws XMLStreamException, ParseException {
    if (reader.isStartElement() && reader.getLocalName().equals("MD_Keywords")) {
      var keywords = new ArrayList<Keyword>();
      var thesaurus = new Thesaurus();
      while (reader.hasNext()
          && !(reader.isEndElement() && reader.getLocalName().equals("MD_Keywords"))) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        switch (reader.getLocalName()) {
          case "keyword":
            skipToOneOf(reader, "CharacterString", "Anchor");
            if (reader.getLocalName().equals("CharacterString")) {
              var keyword = reader.getElementText();
              if (!AUTO_KEYWORDS.contains(keyword)) {
                keywords.add(new Keyword(null, keyword));
              }
            } else {
              var keyword = new Keyword();
              keyword.setNamespace(reader.getAttributeValue(XLINK, "href"));
              var text = reader.getElementText();
              keyword.setKeyword(text);
              if (!AUTO_KEYWORDS.contains(text)) {
                keywords.add(keyword);
              }
            }
            break;
          case "thesaurusName":
            skipToOneOf(reader, "CharacterString", "Anchor");
            if (reader.getLocalName().equals("CharacterString")) {
              thesaurus.setTitle(reader.getElementText());
            } else {
              thesaurus.setNamespace(reader.getAttributeValue(XLINK, "href"));
              thesaurus.setTitle(reader.getElementText());
            }
            skipToElement(reader, "Date");
            var text = reader.getElementText();
            text += text.endsWith("Z") ? "" : "Z";
            thesaurus.setDate(FORMAT.parse(text).toInstant());
            skipToElement(reader, "CI_DateTypeCode");
            thesaurus.setCode(
                CI_DateTypeCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
            break;
        }
      }
      if (keywords.isEmpty()) {
        return;
      }
      if (thesaurus.getTitle() != null
          && thesaurus.getTitle().equals("GEMET - INSPIRE themes, version 1.0")) {
        if (json.getInspireTheme() == null) {
          json.setInspireTheme(new ArrayList<>());
        }
        json.getInspireTheme()
            .add(GeneratorUtils.INSPIRE_THEME_MAP.get(keywords.getFirst().getKeyword()));
      } else {
        json.getKeywords()
            .put(thesaurus.getTitle() == null ? "default" : thesaurus.getTitle(), keywords);
        json.getThesauri()
            .put(thesaurus.getTitle() == null ? "default" : thesaurus.getTitle(), thesaurus);
      }
    }
  }

  private static Contact parseContact(XMLStreamReader reader, String elementName)
      throws XMLStreamException {
    var contact = new Contact();
    while (reader.hasNext()
        && !(reader.isEndElement() && reader.getLocalName().equals(elementName))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case "individualName":
          skipToElement(reader, "CharacterString");
          contact.setName(reader.getElementText());
        case "organisationName":
          skipToElement(reader, "CharacterString");
          contact.setOrganisation(reader.getElementText());
          break;
        case "voice":
          skipToElement(reader, "CharacterString");
          var text = reader.getElementText();
          var matcher = PHONE_REGEXP.matcher(text);
          if (matcher.matches()) {
            var number = matcher.group(1).replace("-", "");
            contact.setPhone(number);
          } else {
            log.warn("Unable to extract phone number from {}", text);
          }
          break;
        case "electronicMailAddress":
          skipToElement(reader, "CharacterString");
          contact.setEmail(reader.getElementText());
          break;
        case "CI_OnlineResource":
          skipToElement(reader, "URL");
          contact.setUrl(reader.getElementText());
          skipToElement(reader, "CI_OnLineFunctionCode");
          contact.setCode(
              CI_OnLineFunctionCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
          break;
        case "CI_RoleCode":
          contact.setRoleCode(CI_RoleCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
          break;
      }
    }
    return contact;
  }

  private void addService(Path file, MetadataCollection metadataCollection) {
    log.info("Adding service from {}", file.toString());
    var isoMetadata = metadataCollection.getIsoMetadata();
    var technicalMetadata = metadataCollection.getTechnicalMetadata();
    var clientMetadata = metadataCollection.getClientMetadata();
    try {
      var service = new Service();
      if (isoMetadata.getServices() == null) {
        isoMetadata.setServices(new ArrayList<>());
      }
      isoMetadata.getServices().add(service);
      service.setServiceDescriptions(new ArrayList<>());
      service.setDataBases(new ArrayList<>());
      service.setPublications(new ArrayList<>());

      var reader = FACTORY.createXMLStreamReader(Files.newInputStream(file));
      nextElement(reader);
      List<Layer> layers = null;
      while (reader.hasNext()
          && !reader.getLocalName().equals("IsoMetadata")
          && !reader.getLocalName().equals("IsoMetadataWMTS")) {
        var res = extractMetadataFields(reader, service, technicalMetadata);
        if (!res.isEmpty()) {
          layers = res;
        }
      }

      extractIsoFields(reader, service);
      parseCapabilities(service, clientMetadata);

      if (replaceValues(service.getUrl()).contains("fbadmin.senstadtdmz.verwalt-berlin.de")) {
        isoMetadata.getServices().removeLast();
        log.info("Removing service as it's not migrated yet.");
      } else {
        clientMetadata.getLayers().put(service.getServiceIdentification(), layers);
      }

    } catch (XMLStreamException | IOException | ParseException | URISyntaxException e) {
      log.warn("Problem while adding service from {}: {}", file, e.getMessage());
      log.trace("Stack trace", e);
      log.warn("Continuing anyway...");
    }
  }

  private static void extractIsoFields(XMLStreamReader reader, Service service)
      throws XMLStreamException {
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

  private static void parseLayer(XMLStreamReader reader, List<Layer> layers)
      throws XMLStreamException {
    var layer = new Layer();
    while (!(reader.isEndElement() && reader.getLocalName().equals("Layer"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case "Layer":
          parseLayer(reader, layers);
          break;
        case "Title":
          layer.setTitle(reader.getElementText());
          break;
        case "Name":
          layer.setName(reader.getElementText());
          break;
        case "Abstract":
          layer.setShortDescription(reader.getElementText());
          break;
        case "Style":
          parseStyle(reader, layer);
          break;
      }
    }
    // prevent previous possibly recursive calls from ending prematurely
    reader.next();
    layers.add(layer);
  }

  private static void parseStyle(XMLStreamReader reader, Layer layer) throws XMLStreamException {
    while (!(reader.isEndElement() && reader.getLocalName().equals("Style"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case "Title":
          layer.setStyleTitle(reader.getElementText());
          break;
        case "Name":
          layer.setStyleName(reader.getElementText());
          break;
        case "LegendURL":
          layer.setLegendImage(reader.getAttributeValue(XLINK, "href"));
          break;
      }
    }
  }

  private static void parseLayers(XMLStreamReader reader, List<Layer> layers)
      throws XMLStreamException {
    while (!(reader.isEndElement() && reader.getLocalName().equals("WMS_Capabilities"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      if (reader.getLocalName().equals("Layer")) {
        parseLayer(reader, layers);
      }
    }
  }

  private static void parseCapabilities(Service service, JsonClientMetadata client)
      throws URISyntaxException, IOException, XMLStreamException {
    var url = replaceValues(service.getUrl());
    if (!url.contains("gdi.berlin.de")) {
      log.info("Not reading capabilities from {}", url);
      return;
    }
    log.info("Reading capabilities from {}", url);
    if (!service.getServiceType().equals(ServiceType.WMS)) {
      return;
    }
    var layers = new ArrayList<Layer>();
    client.getLayers().put(service.getFileIdentifier(), layers);
    var uri =
        new URIBuilder(url)
            .clearParameters()
            .setParameter("request", "GetCapabilities")
            .setParameter("service", "WMS")
            .build();
    var reader = FACTORY.createXMLStreamReader(uri.toURL().openStream());
    parseLayers(reader, layers);
  }

  private static List<Layer> extractMetadataFields(
      XMLStreamReader reader, Service service, JsonTechnicalMetadata technical)
      throws XMLStreamException, ParseException, URISyntaxException {
    var layers = new ArrayList<Layer>();
    if (service.getFeatureTypes() == null) {
      service.setFeatureTypes(new ArrayList<>());
    }
    if (technical.getCategories() == null) {
      technical.setCategories(new ArrayList<>());
    }
    switch (reader.getLocalName()) {
      case "Dienstebeschreibung":
        var desc =
            new ServiceDescription(
                reader.getAttributeValue(null, "typ"), reader.getAttributeValue(null, "url"));
        service.getServiceDescriptions().add(desc);
        break;
      case "Title":
        service.setTitle(reader.getElementText());
        break;
      case "CapabilitiesUrl":
        service.setCapabilitiesUrl(reader.getElementText());
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
      case "GIS-Typ":
        if (reader.getElementText().equals("ogcmap")) {
          service.setServiceType(ServiceType.WMS);
        }
        break;
      case "LegendImage":
        var img =
            new LegendImage(
                reader.getAttributeValue(null, "format"),
                reader.getAttributeValue(null, "url"),
                Integer.parseInt(reader.getAttributeValue(null, "width")),
                Integer.parseInt(reader.getAttributeValue(null, "height")));
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
        {
          var txt = reader.getElementText();
          service.setCreated(FORMAT.parse(txt + (txt.endsWith("Z") ? "" : "Z")).toInstant());
          break;
        }
      case "Revisionsdatum":
        {
          var txt = reader.getElementText();
          service.setUpdated(FORMAT.parse(txt + (txt.endsWith("Z") ? "" : "Z")).toInstant());
          break;
        }
      case "Veroeffentlichungsdatum":
        {
          var txt = reader.getElementText();
          service.setPublished(FORMAT.parse(txt + (txt.endsWith("Z") ? "" : "Z")).toInstant());
          break;
        }
      case "Vorschau":
        // ignored
        break;
      case "Kategorie":
        while (reader.hasNext()
            && !(reader.isEndElement() && reader.getLocalName().equals("Kategorie"))) {
          reader.next();
          if (!reader.isStartElement()) {
            continue;
          }
          if (reader.getLocalName().equals("Link")) {
            technical
                .getCategories()
                .add(
                    new Category(
                        reader.getAttributeValue(null, "title"),
                        reader.getAttributeValue(null, "type"),
                        reader.getElementText()));
          }
        }
        break;
      case "SelectColumn":
        var columnInfo = new ColumnInfo();
        var featureTypes = service.getFeatureTypes();

        // TODO: handle multiple featuretypes in xml
        var featureType = new FeatureType();
        featureType.setColumns(new ArrayList<>());
        featureType.getColumns().add(columnInfo);
        featureTypes.add(featureType);

        while (reader.hasNext()
            && !(reader.isEndElement() && reader.getLocalName().equals("SelectColumn"))) {
          reader.next();
          if (!reader.isStartElement()) {
            continue;
          }
          switch (reader.getLocalName()) {
            case "ColumnName":
              columnInfo.setName(reader.getElementText());
              break;
            case "ColumnAlias":
              columnInfo.setAlias(reader.getElementText());
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
              while (reader.hasNext()
                  && !(reader.isEndElement() && reader.getLocalName().equals("ColumnFilter"))) {
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
        layers.add(layer);
        layer.setName(reader.getAttributeValue(null, "name"));
        while (reader.hasNext()
            && !(reader.isEndElement() && reader.getLocalName().equals("Kartenebene"))) {
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
              layer.setLegendImage(reader.getAttributeValue(null, "url"));
              break;
          }
        }
        break;
    }
    nextElement(reader);
    return layers;
  }
}
