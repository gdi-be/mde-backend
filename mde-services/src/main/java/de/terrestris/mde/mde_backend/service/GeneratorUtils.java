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
import java.util.HashMap;
import java.util.Map;

import static de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode.information;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_RoleCode.distributor;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_RoleCode.pointOfContact;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_CharacterSetCode.utf8;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_GeometricObjectTypeCode.complex;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;

public class GeneratorUtils {

  public static final Map<String, JsonIsoMetadata.InspireTheme> INSPIRE_THEME_MAP;

  public static final Map<JsonIsoMetadata.InspireTheme, String> INSPIRE_THEME_KEYWORD_MAP;

  public static final Contact DEFAULT_CONTACT = new Contact("GDI Berlin", "Senatsverwaltung für Standtentwicklung, Baune und Wohnen", null, null, "geoportal@senstadt.berlin.de", null, null, distributor);

  static {
    INSPIRE_THEME_MAP = new HashMap<>();
    INSPIRE_THEME_MAP.put("Atmosphärische Bedingungen", JsonIsoMetadata.InspireTheme.AC);
    INSPIRE_THEME_MAP.put("Adressen", JsonIsoMetadata.InspireTheme.AD);
    INSPIRE_THEME_MAP.put(
        "Landwirtschaftliche Anlagen und Aquakulturanlagen", JsonIsoMetadata.InspireTheme.AF);
    INSPIRE_THEME_MAP.put(
        "Bewirtschaftungsgebiete/Schutzgebiete/geregelte Gebiete und Berichterstattungseinheiten",
        JsonIsoMetadata.InspireTheme.AM);
    INSPIRE_THEME_MAP.put("Verwaltungseinheiten", JsonIsoMetadata.InspireTheme.AU);
    INSPIRE_THEME_MAP.put("Biogeografische Regionen", JsonIsoMetadata.InspireTheme.BR);
    INSPIRE_THEME_MAP.put("Gebäude", JsonIsoMetadata.InspireTheme.BU);
    INSPIRE_THEME_MAP.put(
        "Flurstücke/Grundstücke (Katasterparzellen)", JsonIsoMetadata.InspireTheme.CP);
    INSPIRE_THEME_MAP.put("Umweltüberwachung", JsonIsoMetadata.InspireTheme.EF);
    INSPIRE_THEME_MAP.put("Höhe", JsonIsoMetadata.InspireTheme.EL);
    INSPIRE_THEME_MAP.put("Energiequellen", JsonIsoMetadata.InspireTheme.ER);
    INSPIRE_THEME_MAP.put("Geologie", JsonIsoMetadata.InspireTheme.GE);
    INSPIRE_THEME_MAP.put("Geografische Gittersysteme", JsonIsoMetadata.InspireTheme.GG);
    INSPIRE_THEME_MAP.put("Geografische Bezeichnungen", JsonIsoMetadata.InspireTheme.GN);
    INSPIRE_THEME_MAP.put("Lebensräume und Biotope", JsonIsoMetadata.InspireTheme.HB);
    INSPIRE_THEME_MAP.put("Gesundheit und Sicherheit", JsonIsoMetadata.InspireTheme.HH);
    INSPIRE_THEME_MAP.put("Gewässernetz", JsonIsoMetadata.InspireTheme.HY);
    INSPIRE_THEME_MAP.put("Bodenbedeckung", JsonIsoMetadata.InspireTheme.LC);
    INSPIRE_THEME_MAP.put("Bodennutzung", JsonIsoMetadata.InspireTheme.LU);
    INSPIRE_THEME_MAP.put("Meteorologisch-geografische Kennwerte", JsonIsoMetadata.InspireTheme.MF);
    INSPIRE_THEME_MAP.put("Mineralische Bodenschätze", JsonIsoMetadata.InspireTheme.MR);
    INSPIRE_THEME_MAP.put("Gebiete mit naturbedingten Risiken", JsonIsoMetadata.InspireTheme.NZ);
    INSPIRE_THEME_MAP.put("Ozeanografisch-geografische Kennwerte", JsonIsoMetadata.InspireTheme.OF);
    INSPIRE_THEME_MAP.put("Orthofotografie", JsonIsoMetadata.InspireTheme.OI);
    INSPIRE_THEME_MAP.put(
        "Verteilung der Bevölkerung — Demografie", JsonIsoMetadata.InspireTheme.PD);
    INSPIRE_THEME_MAP.put("Produktions- und Industrieanlagen", JsonIsoMetadata.InspireTheme.PF);
    INSPIRE_THEME_MAP.put("Schutzgebiete", JsonIsoMetadata.InspireTheme.PS);
    INSPIRE_THEME_MAP.put("Koordinatenreferenzsysteme", JsonIsoMetadata.InspireTheme.RS);
    INSPIRE_THEME_MAP.put("Verteilung der Arten", JsonIsoMetadata.InspireTheme.SD);
    INSPIRE_THEME_MAP.put("Boden", JsonIsoMetadata.InspireTheme.SO);
    INSPIRE_THEME_MAP.put("Meeresregionen", JsonIsoMetadata.InspireTheme.SR);
    INSPIRE_THEME_MAP.put("Statistische Einheiten", JsonIsoMetadata.InspireTheme.SU);
    INSPIRE_THEME_MAP.put("Verkehrsnetze", JsonIsoMetadata.InspireTheme.TN);
    INSPIRE_THEME_MAP.put(
        "Versorgungswirtschaft und staatliche Dienste", JsonIsoMetadata.InspireTheme.US);
    INSPIRE_THEME_KEYWORD_MAP = new HashMap<>();
    for (var e : INSPIRE_THEME_MAP.entrySet()) {
      INSPIRE_THEME_KEYWORD_MAP.put(e.getValue(), e.getKey());
    }
  }

  protected static void writeLanguage(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "language");
    writer.writeStartElement(GMD, "LanguageCode");
    writer.writeAttribute("codeList", "http://www.loc.gov/standards/iso639-2/");
    writer.writeAttribute("codeListValue", "ger");
    writer.writeEndElement();
    writer.writeEndElement();
  }

  protected static void writeFileIdentifier(XMLStreamWriter writer, String fileIdentifier)
      throws XMLStreamException {
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

  protected static void writeHierarchyLevel(XMLStreamWriter writer, MD_ScopeCode level)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "hierarchyLevel");
    writeCodelistValue(writer, level);
    writer.writeEndElement(); // hierarchyLevel
  }

  protected static void writeContact(XMLStreamWriter writer, Contact contact, String localName)
      throws XMLStreamException {
    writer.writeStartElement(GMD, localName);
    writer.writeStartElement(GMD, "CI_ResponsibleParty");
    if (contact.getName() != null) {
      writer.writeStartElement(GMD, "individualName");
      writeSimpleElement(writer, GCO, "CharacterString", contact.getName());
      writer.writeEndElement(); // individual name
    }
    if (contact.getOrganisation() != null) {
      writer.writeStartElement(GMD, "organisationName");
      writeSimpleElement(writer, GCO, "CharacterString", contact.getOrganisation());
      writer.writeEndElement(); // organisationName
    }
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
      writeCodelistValue(writer, information);
      writer.writeEndElement(); // function
      writer.writeEndElement(); // CI_OnlineResource
      writer.writeEndElement(); // onlineResource
    }
    writer.writeEndElement(); // CI_Contact
    writer.writeEndElement(); // contactInfo
    writer.writeStartElement(GMD, "role");
    writeCodelistValue(
        writer, contact.getRoleCode() == null ? pointOfContact : contact.getRoleCode());
    writer.writeEndElement(); // role
    writer.writeEndElement(); // CI_ResponsibleParty
    writer.writeEndElement(); // contact
  }

  protected static void writeDateStamp(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    if (metadata.getDateTime() == null) {
      return;
    }
    writer.writeStartElement(GMD, "dateStamp");
    writeSimpleElement(writer, GCO, "DateTime", metadata.getDateTime().toString());
    writer.writeEndElement(); // dateStamp
  }

  protected static void writeMetadataInfo(XMLStreamWriter writer, boolean inspire)
      throws XMLStreamException {
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

  protected static void writeCrs(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
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

  protected static void writeDate(XMLStreamWriter writer, Instant date, CI_DateTypeCode type)
      throws XMLStreamException {
    if (date == null) {
      return;
    }
    writer.writeStartElement(GMD, "date");
    writer.writeStartElement(GMD, "CI_Date");
    writer.writeStartElement(GMD, "date");
    writeSimpleElement(
        writer,
        GCO,
        "Date",
        DateTimeFormatter.ISO_DATE.format(date.atOffset(ZoneOffset.UTC).toLocalDate()));
    writer.writeEndElement(); // Date
    writer.writeStartElement(GMD, "dateType");
    writeCodelistValue(writer, type);
    writer.writeEndElement(); // dateType
    writer.writeEndElement(); // CI_Date
    writer.writeEndElement(); // date
  }

  protected static <T> void writeCodelistValue(XMLStreamWriter writer, T codeListValue)
      throws XMLStreamException {
    writer.writeStartElement(GMD, codeListValue.getClass().getSimpleName());
    writer.writeAttribute(
        "codeList",
        String.format(
            "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#%s",
            codeListValue.getClass().getSimpleName()));
    writer.writeAttribute("codeListValue", codeListValue.toString());
    writer.writeEndElement();
  }

  protected static void writeVersion(XMLStreamWriter writer, String version)
      throws XMLStreamException {
    writer.writeStartElement(SRV, "serviceTypeVersion");
    writeSimpleElement(writer, GCO, "CharacterString", version);
    writer.writeEndElement(); // serviceTypeVersion
  }
}
