package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.model.json.Contact;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_ScopeCode;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode.information;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_CharacterSetCode.utf8;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_GeometricObjectTypeCode.complex;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;

public class GeneratorUtils {

  protected static void writeLanguage(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "language");
    writer.writeStartElement(GMD, "LanguageCode");
    writer.writeAttribute("codeList", "http://www.loc.gov/standards/iso639-2/");
    writer.writeAttribute("codeListValue", "ger");
    writer.writeEndElement();
    writer.writeEndElement();
  }

  protected static void writeFileIdentifier(XMLStreamWriter writer, String fileIdentifier) throws XMLStreamException {
    if (fileIdentifier == null) {
      return;
    }
    writer.writeStartElement(GMD, "fileIdentifier");
    writeSimpleElement(writer, GCO, "CharacterString", fileIdentifier);
    writer.writeEndElement();
  }

  protected static void writeCharacterSet(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "characterSet");
    writeCodelistValue(writer, utf8);
    writer.writeEndElement(); // characterSet
  }

  protected static void writeHierarchyLevel(XMLStreamWriter writer, MD_ScopeCode level) throws XMLStreamException {
    writer.writeStartElement(GMD, "hierarchyLevel");
    writeCodelistValue(writer, level);
    writer.writeEndElement(); // hierarchyLevel
  }

  protected static void writeContact(XMLStreamWriter writer, Contact contact, String localName) throws XMLStreamException {
    writer.writeStartElement(GMD, localName);
    writer.writeStartElement(GMD, "CI_ResponsibleParty");
    if (contact.getName() != null) {
      writer.writeStartElement(GMD, "individualName");
      writeSimpleElement(writer, GCO, "CharacterString", contact.getName());
      writer.writeEndElement(); // individual name
    }
    writer.writeStartElement(GMD, "organisationName");
    writeSimpleElement(writer, GCO, "CharacterString", contact.getOrganisation());
    writer.writeEndElement(); // organisationName
    writer.writeStartElement(GMD, "contactInfo");
    writer.writeStartElement(GMD, "CI_Contact");
    if (contact.getPhone() != null) {
      writer.writeStartElement(GMD, "phone");
      writer.writeStartElement(GMD, "CI_Telephone");
      if (contact.getPhone() != null) {
        writer.writeStartElement(GMD, "voice");
        writeSimpleElement(writer, GCO, "CharacterString", contact.getPhone());
        writer.writeEndElement(); // voice
      }
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
      writeCodelistValue(writer, information);
      writer.writeEndElement(); // function
      writer.writeEndElement(); // CI_OnlineResource
      writer.writeEndElement(); // onlineResource
    }
    writer.writeEndElement(); // CI_Contact
    writer.writeEndElement(); // contactInfo
    writer.writeStartElement(GMD, "role");
    if (contact.getRoleCode() != null) {
      writeCodelistValue(writer, contact.getRoleCode());
    }
    writer.writeEndElement(); // role
    writer.writeEndElement(); // CI_ResponsibleParty
    writer.writeEndElement(); // contact
  }

  protected static void writeDateStamp(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
    if (metadata.getDateTime() == null) {
      return;
    }
    writer.writeStartElement(GMD, "dateStamp");
    writeSimpleElement(writer, GCO, "DateTime", metadata.getDateTime().toString());
    writer.writeEndElement(); // dateStamp
  }

  protected static void writeMetadataInfo(XMLStreamWriter writer, boolean inspire) throws XMLStreamException {
    writer.writeStartElement(GMD, "metadataStandardName");
    writeSimpleElement(writer, GCO, "CharacterString", "ISO 19115/19119 ? BE");
    writer.writeEndElement(); // metadataStandardName
    writer.writeStartElement(GMD, "metadataStandardVersion");
    writeSimpleElement(writer, GCO, "CharacterString", "1.0.0");
    writer.writeEndElement(); // metadataStandardVersion
    if (!inspire) {
      return;
    }
    // TODO this is hardcoded for now, because details are not prompted in the ui
    writer.writeStartElement(GMD, "spatialRepresentationInfo");
    writer.writeStartElement(GMD, "MD_VectorSpatialRepresentation");
    writer.writeStartElement(GMD, "geometricObjects");
    writer.writeStartElement(GMD, "MD_GeometricObjects");
    writer.writeStartElement(GMD, "geometricObjectType");
    writeCodelistValue(writer, complex);
    writer.writeEndElement(); // geometricObjectType
    writer.writeEndElement(); // MD_GeometricObjects
    writer.writeEndElement(); // geometricObjects
    writer.writeEndElement(); // MD_VectorSpatialRepresentation
    writer.writeEndElement(); // spatialRepresentationInfo
  }

  protected static void writeCrs(XMLStreamWriter writer, JsonIsoMetadata metadata) throws XMLStreamException {
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

  protected static void writeDate(XMLStreamWriter writer, Instant date, CI_DateTypeCode type) throws XMLStreamException {
    if (date == null) {
      return;
    }
    writer.writeStartElement(GMD, "date");
    writer.writeStartElement(GMD, "CI_Date");
    writer.writeStartElement(GMD, "date");
    writeSimpleElement(writer, GCO, "Date", DateTimeFormatter.ISO_DATE.format(date.atOffset(ZoneOffset.UTC).toLocalDate()));
    writer.writeEndElement(); // Date
    writer.writeStartElement(GMD, "dateType");
    writeCodelistValue(writer, type);
    writer.writeEndElement(); // dateType
    writer.writeEndElement(); // CI_Date
    writer.writeEndElement(); // date
  }

  protected static <T> void writeCodelistValue(XMLStreamWriter writer, T codeListValue) throws XMLStreamException {
    writer.writeStartElement(GMD, codeListValue.getClass().getSimpleName());
    writer.writeAttribute("codeList", String.format("http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#%s", codeListValue.getClass().getSimpleName()));
    writer.writeAttribute("codeListValue", codeListValue.toString());
    writer.writeEndElement();
  }

  protected static void writeVersion(XMLStreamWriter writer, String version) throws XMLStreamException {
    writer.writeStartElement(SRV, "serviceTypeVersion");
    writeSimpleElement(writer, GCO, "CharacterString", version);
    writer.writeEndElement(); // serviceTypeVersion
  }

}
