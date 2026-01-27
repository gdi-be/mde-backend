package de.terrestris.mde.mde_backend.service;

import static de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode.information;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_RoleCode.pointOfContact;
import static de.terrestris.mde.mde_backend.model.json.codelists.MD_CharacterSetCode.utf8;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.terrestris.mde.mde_backend.model.dto.MetadataVariables;
import de.terrestris.mde.mde_backend.model.json.Contact;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_MaintenanceFrequencyCode;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_ScopeCode;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GeneratorUtils {

  public static final Map<String, JsonIsoMetadata.InspireTheme> INSPIRE_THEME_MAP;

  public static final Map<JsonIsoMetadata.InspireTheme, String> INSPIRE_THEME_SPECIFICATION_MAP;

  public static final Map<JsonIsoMetadata.InspireTheme, String> INSPIRE_THEME_KEYWORD_MAP;

  public static final Map<JsonIsoMetadata.InspireTheme, List<String>> INSPIRE_THEME_APPSCHEMA_MAP =
      new HashMap<>();

  public static final Map<String, String> IMPORT_SCHEMA_MAP = new HashMap<>();

  public static final Contact DEFAULT_CONTACT;

  public static final MetadataVariables METADATA_VARIABLES;

  private static final Map<String, String> HVD_MAP =
      Map.of(
          "Georaum",
          "http://data.europa.eu/bna/c_ac64a52d",
          "Erdbeobachtung und Umwelt",
          "http://data.europa.eu/bna/c_dd313021",
          "Meteorologie",
          "http://data.europa.eu/bna/c_164e0bf5",
          "Statistik",
          "http://data.europa.eu/bna/c_e1da4e07",
          "Unternehmen und Eigentümerschaft von Unternehmen",
          "http://data.europa.eu/bna/c_a9135398",
          "Mobilität",
          "http://data.europa.eu/bna/c_b79e35eb");

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
    INSPIRE_THEME_SPECIFICATION_MAP = new HashMap<>();
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.RS,
        "D2.8.I.1 Data Specification on Coordinate Reference Systems");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.GG,
        "D2.8.I.2 Data Specification on Geographical Grids and Grid Systems");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.GN, "D2.8.I.3 Data Specification on Geographical Names");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.AU, "D2.8.I.4 Data Specification on Administrative Units");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.AD, "D2.8.I.5 Data Specification on Addresses");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.CP, "D2.8.I.6 Data Specification on Cadastral Parcels");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.TN, "D2.8.I.7 Data Specification on Transport Networks");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.HY, "D2.8.I.8 Data Specification on Hydrography");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.PS, "D2.8.I.9 Data Specification on Protected Sites");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.EL, "D2.8.II.1 Data Specification on Elevation");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.LC, "D2.8.II.2 Data Specification on Land Cover");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.OI, "D2.8.II.3 Data Specification on Orthoimagery");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.GE, "D2.8.II.4 Data Specification on Geology");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.SU, "D2.8.III.1 Data Specification on Statistical Units");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.BU, "D2.8.III.2 Data Specification on Buildings");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.SO, "D2.8.III.3 Data Specification on Soil");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.LU, "D2.8.III.4 Data Specification on Land Use");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.HH,
        "D2.8.III.5 Data Specification on Human Health and Safety");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.US,
        "D2.8.III.6 Data Specification on Utility and Government Services");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.EF,
        "D2.8.III.7 Data Specification on Environmental Monitoring Facilities");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.PF,
        "D2.8.III.8 Data Specification on Production and Industrial Facilities");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.AF,
        "D2.8.III.9 Data Specification on Agricultural and Aquaculture Facilities");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.PD,
        "D2.8.III.10 Data Specification on Population Distribution – Demography");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.AM,
        "D2.8.III.11 Data Specification on Area Management/Restriction/Regulation Zones");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.NZ, "D2.8.III.12 Data Specification on Natural Risk Zones");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.AC,
        "D2.8.III.13 Data Specification on Atmospheric Conditions");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.MF,
        "D2.8.III.14 Data Specification on Meteorological Geographical Features");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.OF,
        "D2.8.III.15 Data Specification on Oceanographic Geographical Features");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.SR, "D2.8.III.16 Data Specification on Sea Regions");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.BR,
        "D2.8.III.17 Data Specification on Bio-geographical Regions");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.HB, "D2.8.III.18 Data Specification on Habitats and Biotopes");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.SD, "D2.8.III.19 Data Specification on Species Distribution");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.ER, "D2.8.III.20 Data Specification on Energy Resources");
    INSPIRE_THEME_SPECIFICATION_MAP.put(
        JsonIsoMetadata.InspireTheme.MR, "D2.8.III.21 Data Specification on Mineral Resources");

    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.AC, List.of("AtmosphericConditions"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.AD, List.of("Addresses"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.AF, List.of("AgriculturalAndAquacultureFacilities"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.AM, List.of("AreaManagementRestrictionAndRegulationZones"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.AU, List.of("AdministrativeUnits", "MaritimeUnits"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.BR, List.of("BioGeographicalRegions"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.BU, List.of("Buildings"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.CP, List.of("CadastralParcels"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.EF, List.of("EnvironmentalMonitoringFacilities"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.EL,
        List.of(
            "ElevationBaseTypes",
            "ElevationGridCoverage",
            "ElevationVectorElements",
            "ElevationTIN"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.ER,
        List.of("EnergyResourcesVector", "EnergyResourcesCoverage", "EnergyResourcesBase"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.GE,
        List.of("GeologicUnit", "GeomorphologicFeature", "GeologicStructure", "Borehole"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.GG, List.of("GeographicalGridSystems"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.GN, List.of("GeographicalNames"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.HB, List.of("HabitatsAndBiotopes"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.HH, List.of("HumanHealth"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.HY,
        List.of("HydroBase", "HydroNetwork", "HydroPhysicalWaters"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.LC, List.of("LandCoverDataset", "LandCoverClassification"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.LU,
        List.of(
            "LandUse", "ExistingLandUse", "GriddedLandUse", "SampledLandUse", "PlannedLandUse"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.MF, List.of("MeteorologicalFeatures"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.MR, List.of("MineralResources"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.NZ, List.of("NaturalRiskZones"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.OF, List.of("OceanGeographicalFeatures"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.OI, List.of("Orthoimagery"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.PD, List.of("PopulationDistribution"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.PF, List.of("ProductionAndIndustrialFacilities"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.PS, List.of("ProtectedSites"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.RS, List.of("ReferenceSystems"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.SD, List.of("SpeciesDistribution"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.SO, List.of("Soil"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.SR, List.of("SeaRegions"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(JsonIsoMetadata.InspireTheme.SU, List.of("StatisticalUnits"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.TN,
        List.of(
            "RoadTransport",
            "RailTransport",
            "AirTransport",
            "WaterTransport",
            "CommonTransportElements"));
    INSPIRE_THEME_APPSCHEMA_MAP.put(
        JsonIsoMetadata.InspireTheme.US,
        List.of(
            "CommonUtilityElements",
            "ElectricityNetwork",
            "OilGasChemicalNetwork",
            "SewerNetwork",
            "ThermalNetwork",
            "WaterNetwork",
            "AdministrativeSocialGovernmentServices",
            "EnvironmentalManagementFacilities"));

    try {
      var mapper = new ObjectMapper(new YAMLFactory());
      // Check environment variable first (production), then System property (tests)
      String codelistsDir = System.getenv("CODELISTS_DIR");
      if (codelistsDir == null) {
        codelistsDir = System.getProperty("CODELISTS_DIR");
      }
      if (codelistsDir == null) {
        throw new IOException("CODELISTS_DIR is not set");
      }
      DEFAULT_CONTACT = mapper.readValue(new File(codelistsDir, "contact.yaml"), Contact.class);
      METADATA_VARIABLES =
          mapper.readValue(
              new File(codelistsDir, "metadatavariables.yaml"), MetadataVariables.class);
    } catch (IOException e) {
      log.error("Error when loading the default contact: {}", e.getMessage());
      log.trace("Stack trace:", e);
      throw new RuntimeException(e);
    }

    IMPORT_SCHEMA_MAP.put(
        "LandCoverVector GML Application Schema", "LandCoverDataset GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "Protected Sites Simple GML Application Schema", "ProtectedSites GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "Orthoimagery GML Application Schema", "Orthoimagery GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "NaturalRiskZones GML Application Schema", "NaturalRiskZones GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "ProductionAndIndustrialFacilities GML Application Schema",
        "ProductionAndIndustrialFacilities GML Application Schema");
    IMPORT_SCHEMA_MAP.put("Addresses GML Application Schema", "Addresses GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "Environmental Monitoring Facilities GML Application Schema",
        "EnvironmentalMonitoringFacilities GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "Planned Land Use GML Application Schema", "PlannedLandUse GML Application Schema");
    IMPORT_SCHEMA_MAP.put("Buildings2D GML Application Schema", "Buildings GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "SpeciesDistribution GML Application Schema", "SpeciesDistribution GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "AdministrativeUnits GML Application Schema", "AdministrativeUnits GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "AdministrativeAndSocialGovernmentalServices GML Application Schema",
        "AdministrativeSocialGovernmentServices GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "HabitatsAndBiotopes GML Application Schema", "HabitatsAndBiotopes GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "ElevationGridCoverage GML Application Schema",
        "ElevationGridCoverage GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "StatisticalUnits GML Application Schema", "StatisticalUnits GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "Existing Land Use GML Application Schema", "ExistingLandUse GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "Cadastral Parcels GML Application Schema", "CadastralParcels GML Application Schema");
    IMPORT_SCHEMA_MAP.put("Soil GML Application Schema", "Soil GML Application Schema");
    IMPORT_SCHEMA_MAP.put(
        "Geographical Names GML Application Schema", "GeographicalNames GML Application Schema");
    IMPORT_SCHEMA_MAP.put("Geology GML Application Schema", "GeologicUnit GML Application Schema");
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
    writeSimpleElement(writer, GCO, "CharacterString", METADATA_VARIABLES.getProfileName());
    writer.writeEndElement(); // metadataStandardName
    writer.writeStartElement(GMD, "metadataStandardVersion");
    writeSimpleElement(writer, GCO, "CharacterString", METADATA_VARIABLES.getProfileVersion());
    writer.writeEndElement(); // metadataStandardVersion
  }

  protected static void writeCrs(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    if (metadata.getCrs() == null) {
      return;
    }
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
    if (codeListValue == null) {
      return;
    }
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

  protected static void writeKeywordTypeCode(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "type");
    writer.writeStartElement(GMD, "MD_KeywordTypeCode");
    writer.writeAttribute("codeListValue", "theme");
    writer.writeAttribute(
        "codeList",
        "https://standards.iso.org/iso/19139/resources/gmxCodelists.xml#MD_KeywordTypeCode");
    writer.writeEndElement(); // MD_KeywordTypeCode
    writer.writeEndElement(); // type
  }

  protected static void writeRegionalKeyword(XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(GMD, "descriptiveKeywords");
    writer.writeStartElement(GMD, "MD_Keywords");
    writer.writeStartElement(GMD, "keyword");
    writer.writeStartElement(GMX, "Anchor");
    writer.writeAttribute(
        XLINK, "href", "http://inspire.ec.europa.eu/metadata-codelist/SpatialScope/regional");
    writer.writeCharacters("Regional");
    writer.writeEndElement(); // Anchor
    writer.writeEndElement(); // keyword
    writeKeywordTypeCode(writer);
    writer.writeStartElement(GMD, "thesaurusName");
    writer.writeStartElement(GMD, "CI_Citation");
    writer.writeStartElement(GMD, "title");
    writer.writeStartElement(GMX, "Anchor");
    writer.writeAttribute(
        XLINK, "href", "http://inspire.ec.europa.eu/metadata-codelist/SpatialScope");
    writer.writeCharacters("Räumlicher Anwendungsbereich");
    writer.writeEndElement(); // Anchor
    writer.writeEndElement(); // title
    writer.writeStartElement(GMD, "date");
    writer.writeStartElement(GMD, "CI_Date");
    writer.writeStartElement(GMD, "date");
    writeSimpleElement(writer, GCO, "Date", "2019-05-22");
    writer.writeEndElement(); // date
    writer.writeStartElement(GMD, "dateType");
    writeCodelistValue(writer, CI_DateTypeCode.publication);
    writer.writeEndElement(); // dateType
    writer.writeEndElement(); // CI_Date
    writer.writeEndElement(); // date
    writer.writeEndElement(); // CI_Citation
    writer.writeEndElement(); // thesaurusName
    writer.writeEndElement(); // MD_Keywords
    writer.writeEndElement(); // descriptiveKeywords
  }

  protected static void writeHvdKeyword(XMLStreamWriter writer, JsonIsoMetadata metadata)
      throws XMLStreamException {
    if (metadata.isHighValueDataset()) {
      for (var category : metadata.getHighValueDataCategory()) {
        writer.writeStartElement(GMD, "descriptiveKeywords");
        writer.writeStartElement(GMD, "MD_Keywords");
        writer.writeStartElement(GMD, "keyword");
        writer.writeStartElement(GMX, "Anchor");
        if (HVD_MAP.get(category) != null) {
          writer.writeAttribute(XLINK, "href", HVD_MAP.get(category));
        }
        writer.writeCharacters(category);
        writer.writeEndElement(); // Anchor
        writer.writeEndElement(); // keyword
        writer.writeStartElement(GMD, "thesaurusName");
        writer.writeStartElement(GMD, "CI_Citation");
        writer.writeStartElement(GMD, "title");
        writer.writeStartElement(GMX, "Anchor");
        writer.writeAttribute(XLINK, "href", "http://data.europa.eu/bna/asd487ae75");
        writer.writeCharacters("HVD-Kategorien");
        writer.writeEndElement(); // Anchor
        writer.writeEndElement(); // title
        writer.writeStartElement(GMD, "date");
        writer.writeStartElement(GMD, "CI_Date");
        writer.writeStartElement(GMD, "date");
        writeSimpleElement(writer, GCO, "Date", "2023-09-27");
        writer.writeEndElement(); // date
        writer.writeStartElement(GMD, "dateType");
        writeCodelistValue(writer, CI_DateTypeCode.publication);
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

  protected static void writeMaintenanceInfo(
      XMLStreamWriter writer, MD_MaintenanceFrequencyCode code) throws XMLStreamException {
    writer.writeStartElement(GMD, "resourceMaintenance");
    writer.writeStartElement(GMD, "MD_MaintenanceInformation");
    writer.writeStartElement(GMD, "maintenanceAndUpdateFrequency");
    writeCodelistValue(writer, code);
    writer.writeEndElement(); // maintenanceAndUpdateFrequency
    writer.writeEndElement(); // MD_MaintenanceInformation
    writer.writeEndElement(); // resourceMaintenance
  }
}
