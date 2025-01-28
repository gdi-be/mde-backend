package de.terrestris.mde.mde_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.terrestris.mde.mde_backend.model.json.Extent;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.Source;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_MaintenanceFrequencyCode;
import de.terrestris.mde.mde_backend.model.json.termsofuse.TermsOfUse;
import lombok.extern.log4j.Log4j2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static de.terrestris.mde.mde_backend.enumeration.MetadataProfile.ISO;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode.*;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_RestrictionCode.otherRestrictions;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_ScopeCode.dataset;
import static de.terrestris.mde.mde_backend.service.GeneratorUtils.*;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.TERMS_OF_USE_BY_ID;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;

@Component
@Log4j2
public class DatasetIsoGenerator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final XMLOutputFactory FACTORY = XMLOutputFactory2.newFactory();

  private static void writeIdentifier(XMLStreamWriter writer, String identifier) throws XMLStreamException {
    writer.writeStartElement(GMD, "identifier");
    writer.writeStartElement(GMD, "MD_Identifier");
    writer.writeStartElement(GMD, "code");
    writeSimpleElement(writer, GCO, "CharacterString", identifier);
    writer.writeEndElement(); // code
    writer.writeEndElement(); // MD_Identifier
    writer.writeEndElement(); // identifier
  }

  private static void writeMaintenanceInfo(XMLStreamWriter writer, MD_MaintenanceFrequencyCode code) throws XMLStreamException {
    writer.writeStartElement(GMD, "resourceMaintenance");
    writer.writeStartElement(GMD, "MD_MaintenanceInformation");
    writer.writeStartElement(GMD, "maintenanceAndUpdateFrequency");
    writeCodelistValue(writer, code);
    writer.writeEndElement(); // maintenanceAndUpdateFrequency
    writer.writeEndElement(); // MD_MaintenanceInformation
    writer.writeEndElement(); // resourceMaintenance
  }

  protected static void writePreviews(XMLStreamWriter writer, List<Source> previews) throws XMLStreamException {
    for (var source : previews) {
      writer.writeStartElement(GMD, "graphicOverview");
      writer.writeStartElement(GMD, "MD_BrowseGraphic");
      writer.writeStartElement(GMD, "fileName");
      writeSimpleElement(writer, GCO, "CharacterString", replaceValues(source.getContent()));
      writer.writeEndElement(); // fileName
      writer.writeEndElement(); // MD_BrowseGraphic
      writer.writeEndElement(); // graphicOverview
    }
  }

  protected static void writeResourceConstraints(XMLStreamWriter writer, TermsOfUse terms) throws XMLStreamException, JsonProcessingException {
    writer.writeStartElement(GMD, "resourceConstraints");
    writer.writeStartElement(GMD, "MD_LegalConstraints");
    writer.writeStartElement(GMD, "accessConstraints");
    writeCodelistValue(writer, otherRestrictions);
    writer.writeEndElement(); // accessConstraints
    writer.writeStartElement(GMD, "otherConstraints");
    writer.writeStartElement(GMX, "Anchor");
    writer.writeAttribute(XLINK, "href", "http://inspire.ec.europa.eu/metadata-codelist/LimitationsOnPublicAccess/noLimitations");
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

  private static void writeSpatialResolution(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
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

  private static void writeTopicCategory(XMLStreamWriter writer, String topic) throws XMLStreamException {
    if (topic != null) {
      writer.writeStartElement(GMD, "topicCategory");
      writeSimpleElement(writer, GMD, "MD_TopicCategoryCode", topic);
      writer.writeEndElement(); // topicCategory
    }
  }

  protected static void writeExtent(XMLStreamWriter writer, Extent extent, String extentNamespace) throws XMLStreamException {
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

  protected static void writeIdentificationInfo(XMLStreamWriter writer, JsonIsoMetadata metadata, String id) throws XMLStreamException, JsonProcessingException {
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
    writePreviews(writer, metadata.getPreviews());
    writeKeywords(writer, metadata);
    writeResourceConstraints(writer, TERMS_OF_USE_BY_ID.get(metadata.getTermsOfUseId().intValue()));
    writeSpatialResolution(writer, metadata);
    writeLanguage(writer);
    writeCharacterSet(writer);
    writeTopicCategory(writer, metadata.getTopicCategory());
    writeExtent(writer, metadata.getExtent(), GMD);
    writer.writeEndElement(); // MD_DataIdentification
    writer.writeEndElement(); // identificationInfo
  }

  private static void writeKeyword(XMLStreamWriter writer, String keyword) throws XMLStreamException {
    writer.writeStartElement(GMD, "keyword");
    writeSimpleElement(writer, GCO, "CharacterString", keyword);
    writer.writeEndElement(); // keyword
  }

  protected static void writeKeywords(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
    for (var entry : metadata.getKeywords().entrySet()) {
      var thesaurus = metadata.getThesauri().get(entry.getKey());
      writer.writeStartElement(GMD, "descriptiveKeywords");
      writer.writeStartElement(GMD, "MD_Keywords");
      if (entry.getKey().equals("default")) {
        if (!metadata.getMetadataProfile().equals(ISO)) {
          writeKeyword(writer, "inspireidentifiziert");
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
        // this hack is necessary because the INSPIRE tests seem to check for string equality instead of properly testing for date equality
        writeSimpleElement(writer, GCO, "Date", DateTimeFormatter.ISO_DATE.format(thesaurus.getDate().atOffset(ZoneOffset.UTC)).substring(0, 10));
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

  protected static void writeDistributionInfo(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
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
    writer.writeStartElement(GMD, "transferOptions");
    writer.writeStartElement(GMD, "MD_DigitalTransferOptions");
    for (var content : metadata.getContentDescriptions()) {
      writer.writeStartElement(GMD, "onLine");
      writer.writeStartElement(GMD, "CI_OnlineResource");
      writer.writeStartElement(GMD, "linkage");
      writeSimpleElement(writer, GMD, "URL", replaceValues(content.getUrl()));
      writer.writeEndElement(); // linkage
      writer.writeStartElement(GMD, "description");
      writeSimpleElement(writer, GCO, "CharacterString", content.getDescription() == null ? "" : content.getDescription());
      writer.writeEndElement(); // description
      writer.writeStartElement(GMD, "function");
      writeCodelistValue(writer, content.getCode());
      writer.writeEndElement(); // function
      writer.writeEndElement(); // CI_OnlineResource
      writer.writeEndElement(); // onLine
    }
    writer.writeEndElement(); // MD_DigitalTransferOptions
    writer.writeEndElement(); // transferOptions
    writer.writeEndElement(); // MD_Distribution
    writer.writeEndElement(); // distributionInfo
  }

  protected static void writeDataQualityInfo(XMLStreamWriter writer, JsonIsoMetadata metadata, boolean service) throws XMLStreamException {
    writer.writeStartElement(GMD, "dataQualityInfo");
    writer.writeStartElement(GMD, "DQ_DataQuality");
    writer.writeStartElement(GMD, "scope");
    writer.writeStartElement(GMD, "DQ_Scope");
    writer.writeStartElement(GMD, "level");
    writeCodelistValue(writer, dataset);
    writer.writeEndElement(); // level
    writer.writeEndElement(); // DQ_Scope
    writer.writeEndElement(); // scope
    if (!metadata.getMetadataProfile().equals(ISO)) {
      writeReport(writer, metadata, service);
    }
    writer.writeStartElement(GMD, "lineage");
    writer.writeStartElement(GMD, "LI_Lineage");
    writer.writeStartElement(GMD, "statement");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getLineage());
    writer.writeEndElement(); // statement
    writer.writeEndElement(); // LI_Lineage
    writer.writeEndElement(); // lineage
    writer.writeEndElement(); // DQ_DataQuality
    writer.writeEndElement(); // dataQualityInfo
  }

  protected static void writeReport(XMLStreamWriter writer, JsonIsoMetadata metadata, boolean service) throws XMLStreamException {
    var text = "VERORDNUNG (EG) Nr. 1089/2010 DER KOMMISSION vom 23. November 2010 zur Durchführung der Richtlinie 2007/2/EG des Europäischen Parlaments und des Rates hinsichtlich der Interoperabilität von Geodatensätzen und -diensten";
    var date = "2010-12-08";
    if (service) {
      text = "VERORDNUNG (EG) Nr. 976/2009 DER KOMMISSION vom 19. Oktober 2009 zur Durchführung der Richtlinie 2007/2/EG des Europäischen Parlaments und des Rates hinsichtlich der Netzdienste";
      date = "2009-10-20";
    }
    writer.writeStartElement(GMD, "report");
    writer.writeStartElement(GMD, "DQ_DomainConsistency");
    writer.writeStartElement(GMD, "result");
    writer.writeStartElement(GMD, "DQ_ConformanceResult");
    writer.writeStartElement(GMD, "specification");
    writer.writeStartElement(GMD, "CI_Citation");
    writer.writeStartElement(GMD, "title");
    writer.writeStartElement(GMX, "Anchor");
    writer.writeAttribute(XLINK, "href", "http://data.europa.eu/eli/reg/2010/1089");
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

  public void generateDatasetMetadata(JsonIsoMetadata metadata, String id, OutputStream out) throws IOException, XMLStreamException {
    var writer = FACTORY.createXMLStreamWriter(out);
    setNamespaceBindings(writer);
    writer.writeStartDocument();
    writer.writeStartElement(GMD, "MD_Metadata");
    writeNamespaceBindings(writer);
    writeFileIdentifier(writer, metadata.getFileIdentifier());
    writeLanguage(writer);
    writeCharacterSet(writer);
    writeHierarchyLevel(writer, dataset);
    for (var contact : metadata.getContacts()) {
      writeContact(writer, contact, "contact");
    }
    writeDateStamp(writer, metadata);
    writeMetadataInfo(writer);
    writeCrs(writer, metadata);
    writeIdentificationInfo(writer, metadata, id);
    writeDistributionInfo(writer, metadata);
    writeDataQualityInfo(writer, metadata, false);
    writer.writeEndElement(); // MD_Metadata
    writer.writeEndDocument();
    writer.close();
    out.close();
  }

}
