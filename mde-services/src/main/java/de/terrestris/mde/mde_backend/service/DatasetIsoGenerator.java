package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.model.json.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.mde.mde_backend.utils.NamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Log4j2
public class DatasetIsoGenerator {

  private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();

  private static void writeLanguage(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "language");
    writer.writeStartElement(GMD, "LanguageCode");
    writer.writeAttribute("codeList", "http://www.loc.gov/standards/iso639-2/");
    writer.writeAttribute("codeListValue", "ger");
    writer.writeEndElement();
    writer.writeEndElement();
  }

  private static void writeFileIdentifier(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
    writer.writeStartElement(GMD, "fileIdentifier");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getFileIdentifier());
    writer.writeEndElement();
  }

  private static void writeCharacterSet(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "characterSet");
    writer.writeStartElement(GMD, "MD_CharacterSetCode");
    writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_CharacterSetCode");
    writer.writeAttribute("codeListValue", "utf8");
    writer.writeEndElement();
    writer.writeEndElement();
  }

  private static void writeHierarchyLevel(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "hierarchyLevel");
    writer.writeStartElement(GMD, "MD_ScopeCode");
    writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_ScopeCode");
    writer.writeAttribute("codeListValue", "dataset");
    writer.writeEndElement();
    writer.writeEndElement();
  }

  private static void writeContact(XMLStreamWriter writer, Contact contact, String localName) throws XMLStreamException {
    writer.writeStartElement(GMD, localName);
    writer.writeStartElement(GMD, "CI_ResponsibleParty");
    writer.writeStartElement(GMD, "organisationName");
    writeSimpleElement(writer, GCO, "CharacterString", contact.getOrganisation());
    writer.writeEndElement(); // organisationName
    writer.writeStartElement(GMD, "contactInfo");
    writer.writeStartElement(GMD, "CI_Contact");
    if (contact.getPhone() != null) {
      writer.writeStartElement(GMD, "phone");
      writer.writeStartElement(GMD, "CI_Telephone");
      writer.writeStartElement(GMD, "voice");
      writeSimpleElement(writer, GCO, "CharacterString", contact.getPhone());
      writer.writeEndElement(); // voice
      writer.writeEndElement(); // CI_Telephone
      writer.writeEndElement(); // phone
    }
    writer.writeStartElement(GMD, "address");
    writer.writeStartElement(GMD, "CI_Address");
    writer.writeStartElement(GMD, "electronicMailAddress");
    writeSimpleElement(writer, GCO, "CharacterString", contact.getEmail());
    writer.writeEndElement(); // electronicMailAddress
    writer.writeEndElement(); // CI_Address
    writer.writeEndElement(); // address
    if (contact.getUrl() != null) {
      writer.writeStartElement(GMD, "onlineResource");
      writer.writeStartElement(GMD, "CI_OnlineResource");
      writer.writeStartElement(GMD, "linkage");
      writeSimpleElement(writer, GMD, "URL", replaceValues(contact.getUrl()));
      writer.writeEndElement(); // linkage
      writer.writeStartElement(GMD, "function");
      writer.writeStartElement(GMD, "CI_OnLineFunctionCode");
      writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_OnLineFunctionCode");
      writer.writeAttribute("codeListValue", "information");
      writer.writeEndElement(); // CI_OnLineFunctionCode
      writer.writeEndElement(); // function
      writer.writeEndElement(); // CI_OnlineResource
      writer.writeEndElement(); // onlineResource
    }
    writer.writeEndElement(); // CI_Contact
    writer.writeEndElement(); // contactInfo
    writer.writeStartElement(GMD, "role");
    writer.writeStartElement(GMD, "CI_RoleCode");
    writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_RoleCode");
    writer.writeAttribute("codeListValue", contact.getRoleCode().toString());
    writer.writeEndElement(); // CI_RoleCode
    writer.writeEndElement(); // role
    writer.writeEndElement(); // CI_ResponsibleParty
    writer.writeEndElement(); // contact
  }

  private static void writeDateStamp(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
    writer.writeStartElement(GMD, "dateStamp");
    writeSimpleElement(writer, GCO, "DateTime", metadata.getDateTime().toString());
    writer.writeEndElement(); // dateStamp
  }

  private static void writeMetadataInfo(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "metadataStandardName");
    writeSimpleElement(writer, GCO, "CharacterString", "ISO 19115/19119 ? BE");
    writer.writeEndElement(); // metadataStandardName
    writer.writeStartElement(GMD, "metadataStandardVersion");
    writeSimpleElement(writer, GCO, "CharacterString", "1.0.0");
    writer.writeEndElement(); // metadataStandardVersion
  }

  private static void writeCrs(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
    writer.writeStartElement(GMD, "referenceSystemInfo");
    writer.writeStartElement(GMD, "MD_ReferenceSystem");
    writer.writeStartElement(GMD, "referenceSystemIdentifier");
    writer.writeStartElement(GMD, "RS_Identifier");
    writer.writeStartElement(GMD, "code");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getCrs());
    writer.writeEndElement(); // code
    writer.writeEndElement(); // RS_Identifier
    writer.writeEndElement(); // referenceSystemIdentifier
    writer.writeEndElement(); // MD_ReferenceSystem
    writer.writeEndElement(); // referenceSystemInfo
  }

  private static void writeDate(XMLStreamWriter writer, Instant date, String type) throws XMLStreamException {
    writer.writeStartElement(GMD, "date");
    writer.writeStartElement(GMD, "CI_Date");
    writer.writeStartElement(GMD, "date");
    writeSimpleElement(writer, GCO, "Date", DateTimeFormatter.ISO_DATE.format(date.atOffset(ZoneOffset.UTC)));
    writer.writeEndElement(); // Date
    writer.writeStartElement(GMD, "dateType");
    writer.writeStartElement(GMD, "CI_DateTypeCode");
    writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_DateTypeCode");
    writer.writeAttribute("codeListValue", type);
    writer.writeEndElement(); // CI_DateTypeCode
    writer.writeEndElement(); // dateType
    writer.writeEndElement(); // CI_Date
    writer.writeEndElement(); // date
  }

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
    writer.writeStartElement(GMD, "MD_MaintenanceFrequencyCode");
    writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_MaintenanceFrequencyCode");
    writer.writeAttribute("codeListValue", code.toString());
    writer.writeEndElement(); // MD_MaintenanceFrequencyCode
    writer.writeEndElement(); // maintenanceAndUpdateFrequency
    writer.writeEndElement(); // MD_MaintenanceInformation
    writer.writeEndElement(); // resourceMaintenance
  }

  private static void writePreviews(XMLStreamWriter writer, List<Source> previews) throws XMLStreamException {
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

  private static void writeResourceConstraints(XMLStreamWriter writer, List<List<Constraint>> constraints) throws XMLStreamException {
    for (var list : constraints) {
      writer.writeStartElement(GMD, "resourceConstraints");
      writer.writeStartElement(GMD, "MD_LegalConstraints");
      for (var constraint : list) {
        writer.writeStartElement(GMD, constraint.getType().toString());
        if (constraint.getRestrictionCode() != null) {
          writer.writeStartElement(GMD, "MD_RestrictionCode");
          writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_RestrictionCode");
          writer.writeAttribute("codeListValue", constraint.getRestrictionCode().toString());
          writer.writeEndElement(); // MD_RestrictionCode
        } else if (constraint.getNamespace() != null) {
          writer.writeStartElement(GMX, "Anchor");
          writer.writeAttribute(XLINK, "href", constraint.getNamespace());
          writer.writeCharacters(constraint.getText());
          writer.writeEndElement(); // Anchor
        } else {
          writeSimpleElement(writer, GCO, "CharacterString", constraint.getText());
        }
        writer.writeEndElement();
      }
      writer.writeEndElement(); // MD_LegalConstraints
      writer.writeEndElement(); // resourceConstraints
    }
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

  private static void writeExtent(XMLStreamWriter writer, Extent extent) throws XMLStreamException {
    writer.writeStartElement(GMD, "extent");
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

  private static void writeIdentificationInfo(XMLStreamWriter writer, JsonIsoMetadata metadata, String id) throws XMLStreamException {
    writer.writeStartElement(GMD, "identificationInfo");
    writer.writeStartElement(GMD, "MD_DataIdentification");
    writer.writeAttribute("uuid", id);
    writer.writeStartElement(GMD, "citation");
    writer.writeStartElement(GMD, "CI_Citation");
    writer.writeStartElement(GMD, "title");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getTitle());
    writer.writeEndElement(); // title
    if (metadata.getCreated() != null) {
      writeDate(writer, metadata.getCreated(), "creation");
    }
    if (metadata.getPublished() != null) {
      writeDate(writer, metadata.getPublished(), "publication");
    }
    if (metadata.getModified() != null) {
      writeDate(writer, metadata.getModified(), "revision");
    }
    writeIdentifier(writer, metadata.getIdentifier());
    writer.writeEndElement(); // CI_Citation
    writer.writeEndElement(); // citation
    writer.writeStartElement(GMD, "abstract");
    writeSimpleElement(writer, GCO, "CharacterString", metadata.getDescription());
    writer.writeEndElement(); // abstract
    writeContact(writer, metadata.getPointOfContact(), "pointOfContact");
    writeMaintenanceInfo(writer, metadata.getMaintenanceFrequency());
    writePreviews(writer, metadata.getPreviews());
    writeKeywords(writer, metadata);
    writeResourceConstraints(writer, metadata.getResourceConstraints());
    writeSpatialResolution(writer, metadata);
    writeLanguage(writer);
    writeCharacterSet(writer);
    writeTopicCategory(writer, metadata.getTopicCategory());
    writeExtent(writer, metadata.getExtent());
    writer.writeEndElement(); // MD_DataIdentification
    writer.writeEndElement(); // identificationInfo
  }

  private static void writeKeywords(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
    for (var entry : metadata.getKeywords().entrySet()) {
      var thesaurus = metadata.getThesauri().get(entry.getKey());
      writer.writeStartElement(GMD, "descriptiveKeywords");
      writer.writeStartElement(GMD, "MD_Keywords");
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
        writeSimpleElement(writer, GCO, "Date", DateTimeFormatter.ISO_DATE.format(thesaurus.getDate().atOffset(ZoneOffset.UTC)).substring(0, 10));
        writer.writeEndElement(); // date
        writer.writeStartElement(GMD, "dateType");
        writer.writeStartElement(GMD, "CI_DateTypeCode");
        writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_DateTypeCode");
        writer.writeAttribute("codeListValue", thesaurus.getCode().toString());
        writer.writeEndElement(); // CI_DateTypeCode
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

  private static void writeDistributionInfo(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
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
      writeSimpleElement(writer, GCO, "CharacterString", content.getDescription());
      writer.writeEndElement(); // description
      writer.writeStartElement(GMD, "function");
      writer.writeStartElement(GMD, "CI_OnLineFunctionCode");
      writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_OnLineFunctionCode");
      writer.writeAttribute("codeListValue", content.getCode().toString());
      writer.writeEndElement(); // CI_OnLineFunctionCode
      writer.writeEndElement(); // function
      writer.writeEndElement(); // CI_OnlineResource
      writer.writeEndElement(); // onLine
    }
    writer.writeEndElement(); // MD_DigitalTransferOptions
    writer.writeEndElement(); // transferOptions
    writer.writeEndElement(); // MD_Distribution
    writer.writeEndElement(); // distributionInfo
  }

  private static void writeDataQualityInfo(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
    writer.writeStartElement(GMD, "dataQualityInfo");
    writer.writeStartElement(GMD, "DQ_DataQuality");
    writer.writeStartElement(GMD, "scope");
    writer.writeStartElement(GMD, "DQ_Scope");
    writer.writeStartElement(GMD, "level");
    writer.writeStartElement(GMD, "MD_ScopeCode");
    writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_ScopeCode");
    writer.writeAttribute("codeListValue", "dataset");
    writer.writeEndElement(); // MD_ScopeCode
    writer.writeEndElement(); // level
    writer.writeEndElement(); // DQ_Scope
    writer.writeEndElement(); // scope
    if (!metadata.getMetadataProfile().equals(MetadataProfile.ISO)) {
      writer.writeStartElement(GMD, "report");
      writer.writeStartElement(GMD, "DQ_DomainConsistency");
      writer.writeStartElement(GMD, "result");
      writer.writeStartElement(GMD, "DQ_ConformanceResult");
      writer.writeStartElement(GMD, "specification");
      writer.writeStartElement(GMD, "CI_Citation");
      writer.writeStartElement(GMD, "title");
      writer.writeStartElement(GMX, "Anchor");
      writer.writeAttribute(XLINK, "href", "http://data.europa.eu/eli/reg/2010/1089");
      writer.writeCharacters("VERORDNUNG (EG) Nr. 1089/2010 DER KOMMISSION vom 23. November 2010 zur Durchführung der Richtlinie 2007/2/EG des Europäischen Parlaments und des Rates hinsichtlich der Interoperabilität von Geodatensätzen und -diensten");
      writer.writeEndElement(); // Anchor
      writer.writeEndElement(); // title
      writer.writeStartElement(GMD, "date");
      writer.writeStartElement(GMD, "CI_Date");
      writer.writeStartElement(GMD, "date");
      writeSimpleElement(writer, GCO, "Date", "2010-12-08");
      writer.writeEndElement(); // date
      writer.writeStartElement(GMD, "dateType");
      writer.writeStartElement(GMD, "CI_DateTypeCode");
      writer.writeAttribute("codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#CI_DateTypeCode");
      writer.writeAttribute("codeListValue", CI_DateTypeCode.publication.toString());
      writer.writeEndElement(); // CI_DateTypeCode
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

  public String generateDatasetMetadata(JsonIsoMetadata metadata, String id) throws IOException, XMLStreamException {
    try (var out = new ByteArrayOutputStream()) {
      var writer = FACTORY.createXMLStreamWriter(out);
      setNamespaceBindings(writer);
      writer.writeStartDocument();
      writer.writeStartElement(GMD, "MD_Metadata");
      writeNamespaceBindings(writer);
      writeFileIdentifier(writer, metadata);
      writeLanguage(writer);
      writeCharacterSet(writer);
      writeHierarchyLevel(writer);
      for (var contact : metadata.getContacts()) {
        writeContact(writer, contact, "contact");
      }
      writeDateStamp(writer, metadata);
      writeMetadataInfo(writer);
      writeCrs(writer, metadata);
      writeIdentificationInfo(writer, metadata, id);
      writeDistributionInfo(writer, metadata);
      writeDataQualityInfo(writer, metadata);
      writer.writeEndElement(); // MD_Metadata
      writer.writeEndDocument();
      writer.close();
      out.close();
      return out.toString(UTF_8);
    }
  }

}
