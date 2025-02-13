package de.terrestris.mde.mde_importer.importer;

import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.jpa.ClientMetadataRepository;
import de.terrestris.mde.mde_backend.jpa.IsoMetadataRepository;
import de.terrestris.mde.mde_backend.jpa.TechnicalMetadataRepository;
import de.terrestris.mde.mde_backend.model.ClientMetadata;
import de.terrestris.mde.mde_backend.model.IsoMetadata;
import de.terrestris.mde.mde_backend.model.TechnicalMetadata;
import de.terrestris.mde.mde_backend.model.json.*;
import de.terrestris.mde.mde_backend.model.json.ColumnInfo.ColumnType;
import de.terrestris.mde.mde_backend.model.json.ColumnInfo.FilterType;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata.InspireTheme;
import de.terrestris.mde.mde_backend.model.json.Service.ServiceType;
import de.terrestris.mde.mde_backend.model.json.codelists.*;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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

import static de.terrestris.mde.mde_backend.service.IsoGenerator.TERMS_OF_USE_MAP;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.XLINK;
import static de.terrestris.utils.xml.XmlUtils.*;

@Component
@Log4j2
public class ImportService {

  private static final Pattern ID_REGEXP = Pattern.compile(".*/([^/]+)");

  private static final Pattern PHONE_REGEXP = Pattern.compile("([+][\\d-]+)");

  private static final XMLInputFactory FACTORY = XMLInputFactory.newFactory();

  private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  public static final Map<String, JsonIsoMetadata.InspireTheme> INSPIRE_THEME_MAP;

  private static final Set<String> AUTO_KEYWORDS = Set.of(
    "inspireidentifiziert",
    "open data",
    "opendata",
    "Sachdaten",
    "Karten",
    "Geodaten",
    "Berlin",
    "infoFeatureAccessService",
    "infoMapAccessService"
  );

  static {
    FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    INSPIRE_THEME_MAP = new HashMap<>();
    INSPIRE_THEME_MAP.put("Atmosphärische Bedingungen", InspireTheme.AC);
    INSPIRE_THEME_MAP.put("Adressen", InspireTheme.AD);
    INSPIRE_THEME_MAP.put("Landwirtschaftliche Anlagen und Aquakulturanlagen", InspireTheme.AF);
    INSPIRE_THEME_MAP.put("Bewirtschaftungsgebiete/Schutzgebiete/geregelte Gebiete und Berichterstattungseinheiten", InspireTheme.AM);
    INSPIRE_THEME_MAP.put("Verwaltungseinheiten", InspireTheme.AU);
    INSPIRE_THEME_MAP.put("Biogeografische Regionen", InspireTheme.BR);
    INSPIRE_THEME_MAP.put("Gebäude", InspireTheme.BU);
    INSPIRE_THEME_MAP.put("Flurstücke/Grundstücke (Katasterparzellen)", InspireTheme.CP);
    INSPIRE_THEME_MAP.put("Umweltüberwachung", InspireTheme.EF);
    INSPIRE_THEME_MAP.put("Höhe", InspireTheme.EL);
    INSPIRE_THEME_MAP.put("Energiequellen", InspireTheme.ER);
    INSPIRE_THEME_MAP.put("Geologie", InspireTheme.GE);
    INSPIRE_THEME_MAP.put("Geografische Gittersysteme", InspireTheme.GG);
    INSPIRE_THEME_MAP.put("Geografische Bezeichnungen", InspireTheme.GN);
    INSPIRE_THEME_MAP.put("Lebensräume und Biotope", InspireTheme.HB);
    INSPIRE_THEME_MAP.put("Gesundheit und Sicherheit", InspireTheme.HH);
    INSPIRE_THEME_MAP.put("Gewässernetz", InspireTheme.HY);
    INSPIRE_THEME_MAP.put("Bodenbedeckung", InspireTheme.LC);
    INSPIRE_THEME_MAP.put("Bodennutzung", InspireTheme.LU);
    INSPIRE_THEME_MAP.put("Meteorologisch-geografische Kennwerte", InspireTheme.MF);
    INSPIRE_THEME_MAP.put("Mineralische Bodenschätze", InspireTheme.MR);
    INSPIRE_THEME_MAP.put("Gebiete mit naturbedingten Risiken", InspireTheme.NZ);
    INSPIRE_THEME_MAP.put("Ozeanografisch-geografische Kennwerte", InspireTheme.OF);
    INSPIRE_THEME_MAP.put("Orthofotografie", InspireTheme.OI);
    INSPIRE_THEME_MAP.put("Verteilung der Bevölkerung — Demografie", InspireTheme.PD);
    INSPIRE_THEME_MAP.put("Produktions- und Industrieanlagen", InspireTheme.PF);
    INSPIRE_THEME_MAP.put("Schutzgebiete", InspireTheme.PS);
    INSPIRE_THEME_MAP.put("Koordinatenreferenzsysteme", InspireTheme.RS);
    INSPIRE_THEME_MAP.put("Verteilung der Arten", InspireTheme.SD);
    INSPIRE_THEME_MAP.put("Boden", InspireTheme.SO);
    INSPIRE_THEME_MAP.put("Meeresregionen", InspireTheme.SR);
    INSPIRE_THEME_MAP.put("Statistische Einheiten", InspireTheme.SU);
    INSPIRE_THEME_MAP.put("Verkehrsnetze", InspireTheme.TN);
    INSPIRE_THEME_MAP.put("Versorgungswirtschaft und staatliche Dienste", InspireTheme.US);
  }

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
    client.getData().setLayers(new HashMap<>());
    metadata.setData(json);
    json.setContacts(new ArrayList<>());
    skipToElement(reader, "Metadaten");
    var type = reader.getAttributeValue(null, "metadatenTyp");
    if (type.equals("ISO")) {
      json.setMetadataProfile(MetadataProfile.ISO);
    }
    if (type.equals("INSPIRE")) {
      json.setMetadataProfile(MetadataProfile.INSPIRE_IDENTIFIED);
    }
    skipToElement(reader, "Titel");
    json.setTitle(reader.getElementText());
    skipToElement(reader, "fileIdentifier");
    skipToElement(reader, "CharacterString");
    json.setFileIdentifier(reader.getElementText());
    skipToElement(reader, "contact");
    var contact = parseContact(reader, "contact");
    if (json.getContacts() == null) {
      json.setContacts(new ArrayList<>());
    }
    json.getContacts().add(contact);
    skipToElement(reader, "dateStamp");
    skipToElement(reader, "DateTime");
    json.setDateTime(Instant.parse(reader.getElementText() + "Z"));
    extractCoordinateSystem(reader, json);
    extractFromIso(reader, metadata, json, client, technical);
    var list = servicesMap.get(metadata.getMetadataId());
    if (list != null) {
      list.forEach(file -> addService(file, json, client.getData(), technical.getData()));
    }
    if (json.getTermsOfUseId() == null) {
      log.info("Terms of use could not be mapped for {}, using standard.", metadata.getMetadataId());
      json.setTermsOfUseId(BigInteger.ONE);
    }
    isoMetadataRepository.save(metadata);
    clientMetadataRepository.save(client);
    technicalMetadataRepository.save(technical);
  }

  private static void extractCoordinateSystem(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    while (reader.hasNext() && !(reader.isStartElement() && reader.getLocalName().equals("identificationInfo"))) {
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

  private static void extractFromIso(XMLStreamReader reader, IsoMetadata metadata, JsonIsoMetadata json, ClientMetadata client, TechnicalMetadata technical) throws XMLStreamException, ParseException {
    skipToElement(reader, "MD_DataIdentification");
    metadata.setMetadataId(reader.getAttributeValue(null, "uuid"));
    client.setMetadataId(metadata.getMetadataId());
    technical.setMetadataId(metadata.getMetadataId());
    json.setPointsOfContact(new ArrayList<>());
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("MD_Metadata"))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      if (reader.isStartElement() && reader.getLocalName().equals("identifier")) {
        skipToElement(reader, "CharacterString");
        json.setIdentifier(reader.getElementText());
      }
      if (reader.isStartElement() && reader.getLocalName().equals("abstract")) {
        skipToElement(reader, "CharacterString");
        json.setDescription(reader.getElementText());
      }
      if (reader.isStartElement() && reader.getLocalName().equals("pointOfContact")) {
        var contact = parseContact(reader, "pointOfContact");
        json.getPointsOfContact().add(contact);
      }
      if (reader.isStartElement() && reader.getLocalName().equals("topicCategory")) {
        skipToElement(reader, "MD_TopicCategoryCode");
        json.setTopicCategory(reader.getElementText());
      }
      if (reader.isStartElement() && reader.getLocalName().equals("graphicOverview")) {
        skipToElement(reader, "CharacterString");
        json.setPreview(reader.getElementText());
      }
      extractConformanceResult(reader, json);
      extractKeyword(reader, json);
      if (reader.isStartElement() && reader.getLocalName().equals("resourceMaintenance")) {
        skipToElement(reader, "MD_MaintenanceFrequencyCode");
        json.setMaintenanceFrequency(MD_MaintenanceFrequencyCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
      }
      extractExtent(reader, json);
      extractSpatialResolution(reader, json);
      extractGraphicOverview(reader, json);
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

  private static void extractExtent(XMLStreamReader reader, JsonIsoMetadata metadata) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("EX_GeographicBoundingBox")) {
      var extent = new Extent();
      metadata.setExtent(extent);
      while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("EX_GeographicBoundingBox"))) {
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

  private static void extractGraphicOverview(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("graphicOverview")) {
      skipToElement(reader, "CharacterString");
      json.setPreview(reader.getElementText());
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
          if (json.isValid() && json.getMetadataProfile().equals(MetadataProfile.INSPIRE_IDENTIFIED)) {
            json.setMetadataProfile(MetadataProfile.INSPIRE_HARMONISED);
          }
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
          var description = new ContentDescription();
          while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("CI_OnlineResource"))) {
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
          json.getContentDescriptions().add(description);
        }
      }
    }
  }

  private static void extractResourceConstraints(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException {
    if (reader.isStartElement() && reader.getLocalName().equals("resourceConstraints")) {
      while (!(reader.isEndElement() && reader.getLocalName().equals("resourceConstraints"))) {
        reader.next();
        if (!reader.isStartElement() || reader.getLocalName().equals("MD_LegalConstraints") ||
          reader.getLocalName().equals("useLimitation") || reader.getLocalName().equals("CharacterString")) {
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

  private static void extractKeyword(XMLStreamReader reader, JsonIsoMetadata json) throws XMLStreamException, ParseException {
    if (reader.isStartElement() && reader.getLocalName().equals("MD_Keywords")) {
      var keywords = new ArrayList<Keyword>();
      var thesaurus = new Thesaurus();
      while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("MD_Keywords"))) {
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
            thesaurus.setCode(CI_DateTypeCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
            break;
        }
      }
      if (keywords.isEmpty()) {
        return;
      }
      json.getKeywords().put(thesaurus.getTitle() == null ? "default" : thesaurus.getTitle(), keywords);
      json.getThesauri().put(thesaurus.getTitle() == null ? "default" : thesaurus.getTitle(), thesaurus);
      if (thesaurus.getTitle() != null && thesaurus.getTitle().equals("GEMET - INSPIRE themes, version 1.0")) {
        json.setInspireTheme(INSPIRE_THEME_MAP.get(keywords.getFirst().getKeyword()));
      }
    }
  }

  private static Contact parseContact(XMLStreamReader reader, String elementName) throws XMLStreamException {
    var contact = new Contact();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals(elementName))) {
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
        case "facsimile":
          var voice = reader.getLocalName().equals("voice");
          skipToElement(reader, "CharacterString");
          var text = reader.getElementText();
          var matcher = PHONE_REGEXP.matcher(text);
          if (matcher.matches()) {
            var number = matcher.group(1).replace("-", "");
            if (voice) {
              contact.setPhone(number);
            } else {
              contact.setFax(number);
            }
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
          contact.setCode(CI_OnLineFunctionCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
          break;
        case "CI_RoleCode":
          contact.setRoleCode(CI_RoleCode.valueOf(reader.getAttributeValue(null, "codeListValue")));
          break;
      }
    }
    return contact;
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

      var reader = FACTORY.createXMLStreamReader(Files.newInputStream(file));
      nextElement(reader);
      List<Layer> layers = null;
      while (reader.hasNext() && !reader.getLocalName().equals("IsoMetadata") && !reader.getLocalName().equals("IsoMetadataWMTS")) {
        var res = extractMetadataFields(reader, service, technical);
        if (!res.isEmpty()) {
          layers = res;
        }
      }

      extractIsoFields(reader, service);
      clientMetadata.getLayers().put(service.getFileIdentifier(), layers);
      parseCapabilities(service, clientMetadata);
    } catch (XMLStreamException | IOException | ParseException | URISyntaxException e) {
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

  private static void parseLayer(XMLStreamReader reader, List<Layer> layers) throws XMLStreamException {
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
          var legend = new LegendImage();
          legend.setWidth(Integer.parseInt(reader.getAttributeValue(null, "width")));
          legend.setHeight(Integer.parseInt(reader.getAttributeValue(null, "height")));
          skipToElement(reader, "Format");
          legend.setFormat(reader.getElementText());
          skipToElement(reader, "OnlineResource");
          legend.setUrl(reader.getAttributeValue(XLINK, "href"));
          layer.setLegendImage(legend);
          break;
      }
    }
  }

  private static void parseLayers(XMLStreamReader reader, List<Layer> layers) throws XMLStreamException {
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

  private static void parseCapabilities(Service service, JsonClientMetadata client) throws URISyntaxException, IOException, XMLStreamException {
    var url = service.getUrl();
    if (!url.contains("@@WMSServiceServer@senstadt@@")) {
      log.info("Not reading capabilities from {}", url);
      return;
    }
    url = url.replace("@@WMSServiceServer@senstadt@@", "gdi.berlin.de/services/wms/");
    log.info("Reading capabilities from {}", url);
    if (!service.getServiceType().equals(ServiceType.WMS)) {
      return;
    }
    var layers = new ArrayList<Layer>();
    client.getLayers().put(service.getFileIdentifier(), layers);
    var uri = new URIBuilder(url)
      .clearParameters()
      .setParameter("request", "GetCapabilities")
      .setParameter("service", "WMS")
      .build();
    var reader = FACTORY.createXMLStreamReader(uri.toURL().openStream());
    parseLayers(reader, layers);
  }

  private static List<Layer> extractMetadataFields(XMLStreamReader reader, Service service, JsonTechnicalMetadata technical) throws XMLStreamException, ParseException, URISyntaxException {
    var layers = new ArrayList<Layer>();
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
      case "GIS-Typ":
        if (reader.getElementText().equals("ogcmap")) {
          service.setServiceType(ServiceType.WMS);
        }
        break;
      case "GIS-ServerName":
        service.setUrl(reader.getElementText());
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
      case "Erstellungsdatum": {
        var txt = reader.getElementText();
        service.setCreated(FORMAT.parse(txt + (txt.endsWith("Z") ? "" : "Z")).toInstant());
        break;
      }
      case "Revisionsdatum": {
        var txt = reader.getElementText();
        service.setUpdated(FORMAT.parse(txt + (txt.endsWith("Z") ? "" : "Z")).toInstant());
        break;
      }
      case "Veroeffentlichungsdatum": {
        var txt = reader.getElementText();
        service.setPublished(FORMAT.parse(txt + (txt.endsWith("Z") ? "" : "Z")).toInstant());
        break;
      }
      case "Vorschau":
        // ignored
        break;
      case "Kategorie":
        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals("Kategorie"))) {
          reader.next();
          if (!reader.isStartElement()) {
            continue;
          }
          if (reader.getLocalName().equals("Link")) {
            technical.getCategories().add(new Category(
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
        layers.add(layer);
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
    return layers;
  }

}
