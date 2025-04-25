package de.terrestris.mde.mde_backend.service;

import static de.terrestris.mde.mde_backend.enumeration.MetadataProfile.ISO;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode.*;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode.information;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_RestrictionCode.otherRestrictions;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_ScopeCode.dataset;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_SpatialRepresentationTypeCode.vector;
import static de.terrestris.mde.mde_backend.service.GeneratorUtils.*;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.TERMS_OF_USE_BY_ID;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.terrestris.mde.mde_backend.model.json.Extent;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.Service;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_MaintenanceFrequencyCode;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_ScopeCode;
import de.terrestris.mde.mde_backend.model.json.termsofuse.TermsOfUse;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import lombok.extern.log4j.Log4j2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class DatasetIsoGenerator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final XMLOutputFactory FACTORY = XMLOutputFactory2.newFactory();

  private static void writeIdentifier(XMLStreamWriter writer, String identifier)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "identifier");
    writer.writeStartElement(GMD, "MD_Identifier");
    writer.writeStartElement(GMD, "code");
    writeSimpleElement(
        writer,
        GCO,
        "CharacterString",
        String.format("https://registry.gdi-de.org/id/de.be.csw/%s", identifier));
    writer.writeEndElement(); // code
    writer.writeEndElement(); // MD_Identifier
    writer.writeEndElement(); // identifier
  }

  private static void writeMaintenanceInfo(XMLStreamWriter writer, MD_MaintenanceFrequencyCode code)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "resourceMaintenance");
    writer.writeStartElement(GMD, "MD_MaintenanceInformation");
    writer.writeStartElement(GMD, "maintenanceAndUpdateFrequency");
    writeCodelistValue(writer, code);
    writer.writeEndElement(); // maintenanceAndUpdateFrequency
    writer.writeEndElement(); // MD_MaintenanceInformation
    writer.writeEndElement(); // resourceMaintenance
  }

  protected static void writePreview(XMLStreamWriter writer, String preview)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "graphicOverview");
    writer.writeStartElement(GMD, "MD_BrowseGraphic");
    writer.writeStartElement(GMD, "fileName");
    writeSimpleElement(writer, GCO, "CharacterString", preview);
    writer.writeEndElement(); // fileName
    writer.writeEndElement(); // MD_BrowseGraphic
    writer.writeEndElement(); // graphicOverview
  }

  protected static void writeResourceConstraints(XMLStreamWriter writer, TermsOfUse terms)
      throws XMLStreamException, JsonProcessingException {
    writer.writeStartElement(GMD, "resourceConstraints");
    writer.writeStartElement(GMD, "MD_LegalConstraints");
    writer.writeStartElement(GMD, "accessConstraints");
    writeCodelistValue(writer, otherRestrictions);
    writer.writeEndElement(); // accessConstraints
    writer.writeStartElement(GMD, "otherConstraints");
    writer.writeStartElement(GMX, "Anchor");
    writer.writeAttribute(
        XLINK,
        "href",
        "http://inspire.ec.europa.eu/metadata-codelist/LimitationsOnPublicAccess/noLimitations");
    writer.writeCharacters("Es gelten keine Zugriffsbeschränkungen");
    writer.writeEndElement(); // Anchor
    writer.writeEndElement(); // otherConstraints
    writer.writeEndElement(); // MD_LegalConstraints
    writer.writeEndElement(); // resourceConstraints
    writer.writeStartElement(GMD, "resourceConstraints");
    writer.writeStartElement(GMD, "MD_LegalConstraints");
    writer.writeStartElement(GMD, "useConstraints");
    writeCodelistValue(writer, otherRestrictions);
    writer.writeEndElement(); // useConstraints
    writer.writeStartElement(GMD, "otherConstraints");
    writeSimpleElement(writer, GCO, "CharacterString", terms.getDescription());
    writer.writeEndElement(); // otherConstraints
    writer.writeStartElement(GMD, "otherConstraints");
    writeSimpleElement(writer, GCO, "CharacterString", MAPPER.writeValueAsString(terms.getJson()));
    writer.writeEndElement(); // otherConstraints
    writer.writeEndElement(); // MD_LegalConstraints
    writer.writeEndElement(); // resourceConstraints
  }

  private static void writeSpatialResolution(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    if (metadata.getScale() != null) {
      writer.writeStartElement(GMD, "spatialResolution");
      writer.writeStartElement(GMD, "MD_Resolution");
      writer.writeStartElement(GMD, "equivalentScale");
      writer.writeStartElement(GMD, "MD_RepresentativeFraction");
      writer.writeStartElement(GMD, "denominator");
      writeSimpleElement(writer, GCO, "Integer", metadata.getScale().toString());
      writer.writeEndElement(); // denominator
      writer.writeEndElement(); // MD_RepresentativeFraction
      writer.writeEndElement(); // equivalentScale
      writer.writeEndElement(); // MD_Resolution
      writer.writeEndElement(); // spatialResolution
    }
    if (metadata.getResolutions() != null) {
      for (var res : metadata.getResolutions()) {
        writer.writeStartElement(GMD, "spatialResolution");
        writer.writeStartElement(GMD, "MD_Resolution");
        writer.writeStartElement(GMD, "distance");
        writer.writeStartElement(GCO, "Distance");
        writer.writeAttribute("uom", "metres");
        writer.writeCharacters(res.toString());
        writer.writeEndElement(); // Distance
        writer.writeEndElement(); // distance
        writer.writeEndElement(); // MD_Resolution
        writer.writeEndElement(); // spatialResolution
      }
    }
  }

  private static void writeTopicCategory(XMLStreamWriter writer, List<String> topics)
      throws XMLStreamException {
    if (topics != null) {

      for (var topic : topics) {
        writer.writeStartElement(GMD, "topicCategory");
        writeSimpleElement(writer, GMD, "MD_TopicCategoryCode", topic);
        writer.writeEndElement(); // topicCategory
      }
    }
  }

  protected static void writeExtent(XMLStreamWriter writer, Extent extent, String extentNamespace)
      throws XMLStreamException {
    writer.writeStartElement(extentNamespace, "extent");
    writer.writeStartElement(GMD, "EX_Extent");
    writer.writeStartElement(GMD, "geographicElement");
    writer.writeStartElement(GMD, "EX_GeographicBoundingBox");
    writer.writeStartElement(GMD, "extentTypeCode");
    writeSimpleElement(writer, GCO, "Boolean", "true");
    writer.writeEndElement(); // extentTypeCode
    writer.writeStartElement(GMD, "westBoundLongitude");
    writeSimpleElement(writer, GCO, "Decimal", Double.toString(extent.getMinx()));
    writer.writeEndElement(); // westBoundLongitude
    writer.writeStartElement(GMD, "eastBoundLongitude");
    writeSimpleElement(writer, GCO, "Decimal", Double.toString(extent.getMaxx()));
    writer.writeEndElement(); // eastBoundLongitude
    writer.writeStartElement(GMD, "southBoundLatitude");
    writeSimpleElement(writer, GCO, "Decimal", Double.toString(extent.getMiny()));
    writer.writeEndElement(); // southBoundLatitude
    writer.writeStartElement(GMD, "northBoundLatitude");
    writeSimpleElement(writer, GCO, "Decimal", Double.toString(extent.getMaxy()));
    writer.writeEndElement(); // northBoundLatitude
    writer.writeEndElement(); // EX_GeographicBoundingBox
    writer.writeEndElement(); // geographicElement
    writer.writeStartElement(GMD, "geographicElement");
    writer.writeStartElement(GMD, "EX_GeographicDescription");
    writer.writeStartElement(GMD, "extentTypeCode");
    writeSimpleElement(writer, GCO, "Boolean", "true");
    writer.writeEndElement(); // extentTypeCode
    writer.writeStartElement(GMD, "geographicIdentifier");
    writer.writeStartElement(GMD, "MD_Identifier");
    writer.writeStartElement(GMD, "code");
    writeSimpleElement(writer, GCO, "CharacterString", "110000000000");
    writer.writeEndElement(); // code
    writer.writeEndElement(); // MD_Identifier
    writer.writeEndElement(); // geographicIdentifier
    writer.writeEndElement(); // EX_GeographicDescription
    writer.writeEndElement(); // geographicElement
    writer.writeEndElement(); // EX_Extent
    writer.writeEndElement(); // extent
  }

  protected static void writeIdentificationInfo(
      XMLStreamWriter writer, JsonIsoMetadata metadata, String id)
      throws XMLStreamException, JsonProcessingException {
    writer.writeStartElement(GMD, "identificationInfo");
    writer.writeStartElement(GMD, "MD_DataIdentification");
    writer.writeAttribute("uuid", id);
    writer.writeStartElement(GMD, "citation");
    writer.writeStartElement(GMD, "CI_Citation");
    writer.writeStartElement(GMD, "title");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getTitle());
    writer.writeEndElement(); // title
    writeDate(writer, metadata.getCreated(), creation);
    writeDate(writer, metadata.getPublished(), publication);
    writeDate(writer, metadata.getModified(), revision);
    writeIdentifier(writer, metadata.getIdentifier());
    writer.writeEndElement(); // CI_Citation
    writer.writeEndElement(); // citation
    writer.writeStartElement(GMD, "abstract");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getDescription());
    writer.writeEndElement(); // abstract
    for (var contact : metadata.getPointsOfContact()) {
      writeContact(writer, contact, "pointOfContact");
    }
    writeMaintenanceInfo(writer, metadata.getMaintenanceFrequency());
    writePreview(writer, metadata.getPreview());
    writeKeywords(writer, metadata);
    writeInspireThemeKeywords(writer, metadata);
    writeResourceConstraints(writer, TERMS_OF_USE_BY_ID.get(metadata.getTermsOfUseId().intValue()));
    writeSpatialRepresentationType(writer);
    writeSpatialResolution(writer, metadata);
    writeLanguage(writer);
    writeCharacterSet(writer);
    writeTopicCategory(writer, metadata.getTopicCategory());
    writeExtent(writer, metadata.getExtent(), GMD);
    writer.writeEndElement(); // MD_DataIdentification
    writer.writeEndElement(); // identificationInfo
  }

  static void writeSpatialRepresentationType(XMLStreamWriter writer) throws XMLStreamException {
    // TODO hardcoded for now, since info is not prompted for in the ui
    writer.writeStartElement(GMD, "spatialRepresentationType");
    writeCodelistValue(writer, vector);
    writer.writeEndElement(); // spatialRepresentationType
  }

  private static void writeKeyword(XMLStreamWriter writer, String keyword)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "keyword");
    writeSimpleElement(writer, GCO, "CharacterString", keyword);
    writer.writeEndElement(); // keyword
  }

  public static List<String> getAutomaticKeywords(JsonIsoMetadata isoMetadata) {
    var list = new ArrayList<String>();
    if (isoMetadata.getMetadataProfile() == null) {
      return list;
    }

    if (!isoMetadata.getMetadataProfile().equals(ISO)) {
      list.add("inspireidentifiziert");
    }
    if (isoMetadata.getTermsOfUseId() != null
        && TERMS_OF_USE_BY_ID.get(isoMetadata.getTermsOfUseId().intValue()).isOpenData()) {
      list.add("open data");
      list.add("opendata");
    }
    if (isoMetadata.getServices() != null) {
      for (var service : isoMetadata.getServices()) {
        switch (service.getServiceType()) {
          case WFS, ATOM -> {
            if (!list.contains("Sachdaten")) {
              list.add("Sachdaten");
            }
          }
          case WMS, WMTS -> {
            if (!list.contains("Karten")) {
              list.add("Karten");
            }
          }
        }
      }
    }
    list.add("Geodaten");
    list.add("Berlin");
    return list;
  }

  protected static void writeKeywords(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    for (var entry : metadata.getKeywords().entrySet()) {
      var thesaurus = metadata.getThesauri().get(entry.getKey());
      writer.writeStartElement(GMD, "descriptiveKeywords");
      writer.writeStartElement(GMD, "MD_Keywords");
      if (entry.getKey().equals("default")) {
        for (var word : getAutomaticKeywords(metadata)) {
          writeKeyword(writer, word);
        }
      }
      for (var keyword : entry.getValue()) {
        writer.writeStartElement(GMD, "keyword");
        if (keyword.getNamespace() != null) {
          writer.writeStartElement(GMX, "Anchor");
          writer.writeAttribute(XLINK, "href", keyword.getNamespace());
          writer.writeCharacters(keyword.getKeyword());
          writer.writeEndElement(); // Anchor
        } else {
          writeSimpleElement(writer, GCO, "CharacterString", keyword.getKeyword());
        }
        writer.writeEndElement(); // keyword
      }
      if (thesaurus.getTitle() != null) {
        writer.writeStartElement(GMD, "thesaurusName");
        writer.writeStartElement(GMD, "CI_Citation");
        writer.writeStartElement(GMD, "title");
        if (thesaurus.getNamespace() != null) {
          writer.writeStartElement(GMX, "Anchor");
          writer.writeAttribute(XLINK, "href", thesaurus.getNamespace());
          writer.writeCharacters(thesaurus.getTitle());
          writer.writeEndElement(); // Anchor
        } else {
          writeSimpleElement(writer, GCO, "CharacterString", thesaurus.getTitle());
        }
        writer.writeEndElement(); // title
        writer.writeStartElement(GMD, "date");
        writer.writeStartElement(GMD, "CI_Date");
        writer.writeStartElement(GMD, "date");
        writeSimpleElement(
            writer,
            GCO,
            "Date",
            DateTimeFormatter.ISO_DATE.format(
                thesaurus.getDate().atOffset(ZoneOffset.UTC).toLocalDate()));
        writer.writeEndElement(); // date
        writer.writeStartElement(GMD, "dateType");
        writeCodelistValue(writer, thesaurus.getCode());
        writer.writeEndElement(); // dateType
        writer.writeEndElement(); // CI_Date
        writer.writeEndElement(); // date
        writer.writeEndElement(); // CI_Citation
        writer.writeEndElement(); // thesaurusName
      }
      writer.writeEndElement(); // MD_Keywords
      writer.writeEndElement(); // descriptiveKeywords
    }
  }

  protected static void writeInspireThemeKeywords(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    if (metadata.getInspireTheme() != null) {
      for (var theme : metadata.getInspireTheme()) {
        writer.writeStartElement(GMD, "descriptiveKeywords");
        writer.writeStartElement(GMD, "MD_Keywords");
        writer.writeStartElement(GMD, "keyword");
        writeSimpleElement(writer, GCO, "CharacterString", INSPIRE_THEME_KEYWORD_MAP.get(theme));
        writer.writeEndElement(); // keyword
        writer.writeStartElement(GMD, "thesaurusName");
        writer.writeStartElement(GMD, "CI_Citation");
        writer.writeStartElement(GMD, "title");
        writeSimpleElement(writer, GCO, "CharacterString", "GEMET - INSPIRE themes, version 1.0");
        writer.writeEndElement(); // title
        writer.writeStartElement(GMD, "date");
        writer.writeStartElement(GMD, "CI_Date");
        writer.writeStartElement(GMD, "date");
        writeSimpleElement(writer, GCO, "Date", "2008-06-01");
        writer.writeEndElement(); // date
        writer.writeStartElement(GMD, "dateType");
        writeCodelistValue(writer, publication);
        writer.writeEndElement(); // dateType
        writer.writeEndElement(); // CI_Date
        writer.writeEndElement(); // date
        writer.writeEndElement(); // CI_Citation
        writer.writeEndElement(); // thesaurusName
        writer.writeEndElement(); // MD_Keywords
        writer.writeEndElement(); // descriptiveKeywords
      }
    }
  }

  protected static void writeDistributionInfo(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "distributionInfo");
    writer.writeStartElement(GMD, "MD_Distribution");
    writer.writeStartElement(GMD, "distributionFormat");
    writer.writeStartElement(GMD, "MD_Format");
    writer.writeStartElement(GMD, "name");
    writeSimpleElement(writer, GCO, "CharacterString", "Text/HTML");
    writer.writeEndElement(); // name
    writer.writeStartElement(GMD, "version");
    writeSimpleElement(writer, GCO, "CharacterString", "4.01");
    writer.writeEndElement(); // version
    writer.writeEndElement(); // MD_Format
    writer.writeEndElement(); // distributionFormat
    for (var service : metadata.getServices()) {
      writer.writeStartElement(GMD, "transferOptions");
      writer.writeStartElement(GMD, "MD_DigitalTransferOptions");
      writer.writeStartElement(GMD, "onLine");
      writer.writeStartElement(GMD, "CI_OnlineResource");
      writer.writeStartElement(GMD, "linkage");
      var capas = service.getCapabilitiesUrl();
      if (!service.getServiceType().equals(Service.ServiceType.ATOM)) {
        capas += "?request=GetCapabilities&service=" + service.getServiceType();
      }
      writeSimpleElement(writer, GMD, "URL", replaceValues(capas));
      writer.writeEndElement(); // linkage
      writer.writeStartElement(GMD, "protocol");
      writer.writeStartElement(GMX, "Anchor");
      var type =
          switch (service.getServiceType()) {
            case WFS -> "http://www.opengis.net/def/serviceType/ogc/wfs";
            case WMS -> "http://www.opengis.net/def/serviceType/ogc/wms";
            case ATOM -> "https://tools.ietf.org/html/rfc4287";
            case WMTS -> "http://www.opengis.net/def/serviceType/ogc/wmts";
          };
      var text =
          switch (service.getServiceType()) {
            case WFS -> "OGC Web Feature Service";
            case WMS -> "OGC Web Map Service";
            case ATOM -> "ATOM Syndication Format";
            case WMTS -> "OGC Web Map Tile Service";
          };
      writer.writeAttribute(XLINK, "href", type);
      writer.writeCharacters(text);
      writer.writeEndElement(); // Anchor
      writer.writeEndElement(); // protocol
      writer.writeStartElement(GMD, "applicationProfile");
      writer.writeStartElement(GMX, "Anchor");
      var inspireType =
          switch (service.getServiceType()) {
            case WFS, ATOM ->
                "http://inspire.ec.europa.eu/metadata-codelist/SpatialDataServiceType/download";
            case WMS, WMTS ->
                "http://inspire.ec.europa.eu/metadata-codelist/SpatialDataServiceType/view";
          };
      var inspireText =
          switch (service.getServiceType()) {
            case WFS, ATOM -> "Download Service";
            case WMS, WMTS -> "View Service";
          };
      writer.writeAttribute(XLINK, "href", inspireType);
      writer.writeCharacters(inspireText);
      writer.writeEndElement(); // Anchor
      writer.writeEndElement(); // applicationProfile
      writer.writeEndElement(); // CI_OnlineResource
      writer.writeEndElement(); // onLine
      writer.writeEndElement(); // MD_DigitalTransferOptions
      writer.writeEndElement(); // transferOptions
    }
    writeSpecialDescriptions(writer, metadata);
    writer.writeStartElement(GMD, "transferOptions");
    writer.writeStartElement(GMD, "MD_DigitalTransferOptions");
    if (metadata.getContentDescriptions() != null) {
      for (var content : metadata.getContentDescriptions()) {
        writer.writeStartElement(GMD, "onLine");
        writer.writeStartElement(GMD, "CI_OnlineResource");
        writer.writeStartElement(GMD, "linkage");
        writeSimpleElement(writer, GMD, "URL", replaceValues(content.getUrl()));
        writer.writeEndElement(); // linkage
        writer.writeStartElement(GMD, "description");
        writer.writeStartElement(GMX, "Anchor");
        writer.writeAttribute(
            XLINK,
            "href",
            "http://inspire.ec.europa.eu/metadata-codelist/OnLineDescriptionCode/accessPoint");
        writer.writeCharacters(
            "http://inspire.ec.europa.eu/metadata-codelist/OnLineDescriptionCode/accessPoint");
        writer.writeEndElement(); // Anchor
        writer.writeEndElement(); // description
        writer.writeStartElement(GMD, "function");
        writeCodelistValue(writer, content.getCode());
        writer.writeEndElement(); // function
        writer.writeEndElement(); // CI_OnlineResource
        writer.writeEndElement(); // onLine
      }
    }
    writer.writeEndElement(); // MD_DigitalTransferOptions
    writer.writeEndElement(); // transferOptions
    writer.writeEndElement(); // MD_Distribution
    writer.writeEndElement(); // distributionInfo
  }

  private static void writeDescription(XMLStreamWriter writer, String url)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "transferOptions");
    writer.writeStartElement(GMD, "MD_DigitalTransferOptions");
    writer.writeStartElement(GMD, "onLine");
    writer.writeStartElement(GMD, "CI_OnlineResource");
    writer.writeStartElement(GMD, "linkage");
    writeSimpleElement(writer, GMD, "URL", replaceValues(url));
    writer.writeEndElement(); // linkage
    writer.writeStartElement(GMD, "description");
    writer.writeStartElement(GMX, "Anchor");
    writer.writeAttribute(
        XLINK,
        "href",
        "http://inspire.ec.europa.eu/metadata-codelist/OnLineDescriptionCode/accessPoint");
    writer.writeCharacters(
        "http://inspire.ec.europa.eu/metadata-codelist/OnLineDescriptionCode/accessPoint");
    writer.writeEndElement(); // Anchor
    writer.writeEndElement(); // description
    writer.writeStartElement(GMD, "function");
    writeCodelistValue(writer, information);
    writer.writeEndElement(); // function
    writer.writeEndElement(); // CI_OnlineResource
    writer.writeEndElement(); // onLine
    writer.writeEndElement(); // MD_DigitalTransferOptions
    writer.writeEndElement(); // transferOptions
  }

  private static void writeSpecialDescriptions(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    if (metadata.getContentDescription() != null) {
      writeDescription(writer, metadata.getContentDescription());
    }
    if (metadata.getTechnicalDescription() != null) {
      writeDescription(writer, metadata.getTechnicalDescription());
    }
  }

  protected static void writeDataQualityInfo(
      XMLStreamWriter writer, JsonIsoMetadata metadata, Service service) throws XMLStreamException {
    writer.writeStartElement(GMD, "dataQualityInfo");
    writer.writeStartElement(GMD, "DQ_DataQuality");
    writer.writeStartElement(GMD, "scope");
    writer.writeStartElement(GMD, "DQ_Scope");
    writer.writeStartElement(GMD, "level");
    writeCodelistValue(writer, service != null ? MD_ScopeCode.service : dataset);
    writer.writeEndElement(); // level
    if (service != null) {
      writer.writeStartElement(GMD, "levelDescription");
      writer.writeStartElement(GMD, "MD_ScopeDescription");
      writer.writeStartElement(GMD, "other");
      writeSimpleElement(writer, GCO, "CharacterString", "Dienst");
      writer.writeEndElement(); // other
      writer.writeEndElement(); // MD_ScopeDescription
      writer.writeEndElement(); // levelDescription
    }
    writer.writeEndElement(); // DQ_Scope
    writer.writeEndElement(); // scope
    if (!metadata.getMetadataProfile().equals(ISO)) {
      writeReport(writer, metadata);
    }

    writer.writeStartElement(GMD, "lineage");
    writer.writeStartElement(GMD, "LI_Lineage");
    writer.writeStartElement(GMD, "statement");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getLineage().getFirst().getTitle());
    writer.writeEndElement(); // statement
    if (service != null && service.getDataBases() != null) {
      for (var ds : service.getDataBases()) {
        writer.writeStartElement(GMD, "source");
        writer.writeStartElement(GMD, "LI_Source");
        writer.writeStartElement(GMD, "description");
        writeSimpleElement(writer, GCO, "CharacterString", ds.getContent());
        writer.writeEndElement(); // description
        writer.writeEndElement(); // LI_Source
        writer.writeEndElement(); // source
      }
    }
    writer.writeEndElement(); // LI_Lineage
    writer.writeEndElement(); // lineage

    writer.writeEndElement(); // DQ_DataQuality
    writer.writeEndElement(); // dataQualityInfo
  }

  protected static void writeReport(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    var text =
        "VERORDNUNG (EG) Nr. 1089/2010 DER KOMMISSION vom 23. November 2010 zur Durchführung der Richtlinie 2007/2/EG des Europäischen Parlaments und des Rates hinsichtlich der Interoperabilität von Geodatensätzen und -diensten";
    var date = "2010-12-08";
    // HBD: the 1089/2010 has been replaced with (s.o.), else the invocable spatial data services
    // test class fails
    // also, we need at least 3 reports, else the tests will also fail
    var text2 =
        "VERORDNUNG (EU) Nr. 102/2011 DER KOMMISSION vom 4. Februar 2011 zur Änderung der Verordnung (EU) Nr. (s.o.) zur Durchführung der Richtlinie 2007/2/EG des Europäischen Parlaments und des Rates hinsichtlich der Interoperabilität von Geodatensätzen und -diensten";
    var date2 = "2011-02-05";
    var text3 =
        "VERORDNUNG (EG) Nr. 976/2009 DER KOMMISSION vom 19. Oktober 2009 zur Durchführung der Richtlinie 2007/2/EG des Europäischen Parlaments und des Rates hinsichtlich der Netzdienste";
    var date3 = "2009-10-20";
    writeReport(writer, metadata, text, date, "http://data.europa.eu/eli/reg/2010/1089");
    writeReport(writer, metadata, text2, date2, "http://data.europa.eu/eli/reg/2010/1089");
    writeReport(writer, metadata, text3, date3, "http://data.europa.eu/eli/reg/2010/1089");
    writeReport(
        writer,
        metadata,
        "invocable",
        date,
        "http://inspire.ec.europa.eu/id/ats/metadata/2.0/sds-invocable");
  }

  private static void writeReport(
      XMLStreamWriter writer, JsonIsoMetadata metadata, String text, String date, String url)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "report");
    writer.writeStartElement(GMD, "DQ_DomainConsistency");
    writer.writeStartElement(GMD, "result");
    writer.writeStartElement(GMD, "DQ_ConformanceResult");
    writer.writeStartElement(GMD, "specification");
    writer.writeStartElement(GMD, "CI_Citation");
    writer.writeStartElement(GMD, "title");
    writer.writeStartElement(GMX, "Anchor");
    writer.writeAttribute(XLINK, "href", url);
    writer.writeCharacters(text);
    writer.writeEndElement(); // Anchor
    writer.writeEndElement(); // title
    writer.writeStartElement(GMD, "date");
    writer.writeStartElement(GMD, "CI_Date");
    writer.writeStartElement(GMD, "date");
    writeSimpleElement(writer, GCO, "Date", date);
    writer.writeEndElement(); // date
    writer.writeStartElement(GMD, "dateType");
    writeCodelistValue(writer, publication);
    writer.writeEndElement(); // dateType
    writer.writeEndElement(); // CI_Date
    writer.writeEndElement(); // date
    writer.writeEndElement(); // CI_Citation
    writer.writeEndElement(); // specification
    writer.writeStartElement(GMD, "explanation");
    writeSimpleElement(writer, GCO, "CharacterString", "see referenced specification");
    writer.writeEndElement(); // explanation
    writer.writeStartElement(GMD, "pass");
    writeSimpleElement(writer, GCO, "Boolean", Boolean.toString(metadata.isValid()));
    writer.writeEndElement(); // pass
    writer.writeEndElement(); // DQ_ConformanceResult
    writer.writeEndElement(); // result
    writer.writeEndElement(); // DQ_DomainConsistency
    writer.writeEndElement(); // report
  }

  /**
   * Write metadata to a XMLStreamWriter.
   *
   * @param metadata the metadata object
   * @param id the metadata id
   * @param writer the writer, must have already written the MD_Metadata element and the namespace
   *     bindings etc.
   * @throws IOException in case anything goes wrong
   * @throws XMLStreamException in case anything goes wrong
   */
  public void generateDatasetMetadata(JsonIsoMetadata metadata, String id, XMLStreamWriter writer)
      throws IOException, XMLStreamException {
    writeFileIdentifier(writer, metadata.getFileIdentifier());
    writeLanguage(writer);
    writeCharacterSet(writer);
    writeHierarchyLevel(writer, dataset);
    if (metadata.getContacts() != null) {
      for (var contact : metadata.getContacts()) {
        writeContact(writer, contact, "contact");
      }
    }
    writeDateStamp(writer, metadata);
    writeMetadataInfo(writer, !metadata.getMetadataProfile().equals(ISO));
    writeCrs(writer, metadata);
    writeIdentificationInfo(writer, metadata, id);
    writeDistributionInfo(writer, metadata);
    writeDataQualityInfo(writer, metadata, null);
  }

  public void generateDatasetMetadata(JsonIsoMetadata metadata, String id, OutputStream out)
      throws IOException, XMLStreamException {
    var writer = FACTORY.createXMLStreamWriter(out);
    setNamespaceBindings(writer);
    writer.writeStartDocument();
    writer.writeStartElement(GMD, "MD_Metadata");
    writeNamespaceBindings(writer);
    generateDatasetMetadata(metadata, id, writer);
    writer.writeEndElement(); // MD_Metadata
    writer.writeEndDocument();
    writer.close();
    out.close();
  }
}
