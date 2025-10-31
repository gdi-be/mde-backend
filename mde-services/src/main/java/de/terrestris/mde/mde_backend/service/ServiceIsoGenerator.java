package de.terrestris.mde.mde_backend.service;

import static de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode.*;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_OnLineFunctionCode.information;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_PresentationFormCode.mapDigital;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_PresentationFormCode.tableDigital;
import static de.terrestris.mde.mde_backend.service.DatasetIsoGenerator.*;
import static de.terrestris.mde.mde_backend.service.GeneratorUtils.*;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.TERMS_OF_USE_BY_ID;
import static de.terrestris.mde.mde_backend.service.IsoGenerator.replaceValues;
import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.LegendImage;
import de.terrestris.mde.mde_backend.model.json.Service;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_ScopeCode;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.stax2.XMLOutputFactory2;
import org.springframework.stereotype.Component;

@Component
public class ServiceIsoGenerator {

  private static final XMLOutputFactory FACTORY = XMLOutputFactory2.newFactory();

  private static void writeOperations(
      XMLStreamWriter writer, Service service, JsonIsoMetadata metadata) throws XMLStreamException {
    writer.writeStartElement(SRV, "containsOperations");
    writer.writeStartElement(SRV, "SV_OperationMetadata");
    switch (service.getServiceType()) {
      case WFS, WMS, WMTS -> writeOperation(writer, service, "GetCapabilities");
      case ATOM -> writeOperation(writer, service, "Download");
    }
    writer.writeEndElement(); // SV_CouplingType
    writer.writeEndElement(); // containsOperations
    writer.writeStartElement(SRV, "operatesOn");
    writer.writeAttribute("uuidref", metadata.getIdentifier());
    writer.writeAttribute(
        XLINK, "href", "https://registry.gdi-de.org/id/de.be.csw/" + metadata.getIdentifier());
    writer.writeEndElement(); // operatesOn
  }

  private static void writeOperation(XMLStreamWriter writer, Service service, String operationName)
      throws XMLStreamException {
    writer.writeStartElement(SRV, "operationName");
    writeSimpleElement(writer, GCO, "CharacterString", operationName);
    writer.writeEndElement(); // operationName
    writer.writeStartElement(SRV, "DCP");
    writer.writeStartElement(SRV, "DCPList");
    writer.writeAttribute(
        "codeList", "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#DCPList");
    writer.writeAttribute("codeListValue", "WebServices");
    writer.writeEndElement(); // DCPList
    writer.writeEndElement(); // DCP
    writer.writeStartElement(SRV, "connectPoint");
    writer.writeStartElement(GMD, "CI_OnlineResource");
    writer.writeStartElement(GMD, "linkage");
    writeSimpleElement(writer, GMD, "URL", getServiceUrl(service, false));
    writer.writeEndElement(); // linkage
    writer.writeStartElement(GMD, "function");
    writeCodelistValue(writer, information);
    writer.writeEndElement(); // function
    writer.writeEndElement(); // CI_OnlineResource
    writer.writeEndElement(); // connectPoint
  }

  private static void writeLegend(XMLStreamWriter writer, LegendImage legendImage)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "graphicOverview");
    writer.writeStartElement(GMD, "MD_BrowseGraphic");
    writer.writeStartElement(GMD, "fileName");
    writeSimpleElement(writer, GCO, "CharacterString", replaceValues(legendImage.getUrl()));
    writer.writeEndElement(); // fileName
    writer.writeStartElement(GMD, "fileDescription");
    writeSimpleElement(writer, GCO, "CharacterString", "Legende");
    writer.writeEndElement(); // fileDescription
    if (legendImage.getFormat() != null) {
      writer.writeStartElement(GMD, "fileType");
      writeSimpleElement(writer, GCO, "CharacterString", legendImage.getFormat());
      writer.writeEndElement(); // fileType
    }
    writer.writeEndElement(); // MD_BrowseGraphic
    writer.writeEndElement(); // graphicOverview
  }

  private static void writeServiceIdentification(
      XMLStreamWriter writer, Service service, JsonIsoMetadata metadata)
      throws XMLStreamException, JsonProcessingException {
    writer.writeStartElement(GMD, "identificationInfo");
    writer.writeStartElement(SRV, "SV_ServiceIdentification");
    writer.writeAttribute("uuid", service.getServiceIdentification());
    writer.writeStartElement(GMD, "citation");
    writer.writeStartElement(GMD, "CI_Citation");
    writer.writeStartElement(GMD, "title");
    writeSimpleElement(
        writer,
        GCO,
        "CharacterString",
        service.getTitle() == null ? metadata.getTitle() : service.getTitle());
    writer.writeEndElement(); // title
    writeDate(
        writer,
        service.getCreated() == null ? metadata.getCreated() : service.getCreated(),
        creation);
    writeDate(
        writer,
        service.getPublished() == null ? metadata.getPublished() : service.getPublished(),
        publication);
    writeDate(
        writer,
        service.getUpdated() == null ? metadata.getModified() : service.getUpdated(),
        revision);
    writer.writeStartElement(GMD, "identifier");
    writer.writeStartElement(GMD, "MD_Identifier");
    writer.writeStartElement(GMD, "code");
    writeSimpleElement(
        writer,
        GCO,
        "CharacterString",
        String.format(
            "https://registry.gdi-de.org/id/de.be.csw/%s", service.getServiceIdentification()));
    writer.writeEndElement(); // code
    writer.writeEndElement(); // MD_Identifier
    writer.writeEndElement(); // identifier
    writer.writeStartElement(GMD, "presentationForm");
    if (service.getServiceType().equals(Service.ServiceType.WFS)) {
      writeCodelistValue(writer, tableDigital);
    } else {
      writeCodelistValue(writer, mapDigital);
    }
    writer.writeEndElement(); // presentationForm
    writer.writeEndElement(); // CI_Citation
    writer.writeEndElement(); // citation
    writer.writeStartElement(GMD, "abstract");
    if (service.getShortDescription() != null) {
      writeSimpleElement(writer, GCO, "CharacterString", service.getShortDescription());
    }
    writer.writeEndElement(); // abstract
    if (metadata.getPointsOfContact() != null) {
      for (var contact : metadata.getPointsOfContact()) {
        writeContact(writer, contact, "pointOfContact");
      }
    }
    writeMaintenanceInfo(writer, metadata.getMaintenanceFrequency());
    if (service.getPreview() != null) {
      writePreview(writer, service.getPreview());
    }
    if (service.getLegendImage() != null) {
      writeLegend(writer, service.getLegendImage());
    }
    writeKeywords(writer, metadata);
    writeInspireThemeKeywords(writer, metadata);
    writeRegionalKeyword(writer);
    switch (service.getServiceType()) {
      case WFS, ATOM -> writeServiceKeyword(writer, "infoFeatureAccessService");
      case WMS, WMTS -> writeServiceKeyword(writer, "infoMapAccessService");
    }
    if (metadata.getTermsOfUseId() != null) {
      writeResourceConstraints(
          writer,
          TERMS_OF_USE_BY_ID.get(metadata.getTermsOfUseId().intValue()),
          metadata.getTermsOfUseSource());
    }
    writer.writeStartElement(SRV, "serviceType");
    writer.writeStartElement(GCO, "LocalName");
    writer.writeAttribute(
        "codeSpace", "http://inspire.ec.europa.eu/metadata-codelist/SpatialDataServiceType");
    // HBD: using download/view here may seem correct, however, the invocable spatial data services
    // class tests for 'other'
    // whereas the network services tests check for download/view
    //    writer.writeCharacters("other");
    switch (service.getServiceType()) {
      case WFS, ATOM -> writer.writeCharacters("download");
      case WMS, WMTS -> writer.writeCharacters("view");
    }
    writer.writeEndElement(); // LocalName
    writer.writeEndElement(); // serviceType
    switch (service.getServiceType()) {
      case WFS -> {
        writeVersion(writer, "OGC:WFS 1.0.0");
        writeVersion(writer, "OGC:WFS 1.1.0");
        writeVersion(writer, "OGC:WFS 2.0.0");
      }
      case WMS -> {
        writeVersion(writer, "OGC:WMS 1.0.0");
        writeVersion(writer, "OGC:WMS 1.1.0");
        writeVersion(writer, "OGC:WMS 1.1.1");
        writeVersion(writer, "OGC:WMS 1.3.0");
      }
      case ATOM -> writeVersion(writer, "predefined ATOM");
      case WMTS -> writeVersion(writer, "OGC:WMTS 1.0.0");
    }
    if (metadata.getExtent() != null) {
      writeExtent(writer, metadata.getExtent(), SRV, metadata);
    }
    writer.writeStartElement(SRV, "couplingType");
    writer.writeStartElement(SRV, "SV_CouplingType");
    writer.writeAttribute(
        "codeList",
        "http://standards.iso.org/iso/19139/resources/gmxCodelists.xml#SV_CouplingType");
    writer.writeAttribute("codeListValue", "tight");
    writer.writeEndElement(); // SV_CouplingType
    writer.writeEndElement(); // couplingType
    writeOperations(writer, service, metadata);
    writer.writeEndElement(); // SV_ServiceIdentification
    writer.writeEndElement(); // identificationInfo
  }

  private static void writeServiceKeyword(XMLStreamWriter writer, String type)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "descriptiveKeywords");
    writer.writeStartElement(GMD, "MD_Keywords");
    writer.writeStartElement(GMD, "keyword");
    writeSimpleElement(writer, GCO, "CharacterString", type);
    writer.writeEndElement(); // keyword
    writer.writeEndElement(); // MD_Keywords
    writer.writeEndElement(); // descriptiveKeywords
  }

  /**
   * Write metadata to a XMLStreamWriter.
   *
   * @param metadata the metadata object
   * @param service the service object
   * @param writer the writer, must have already written the MD_Metadata element and the namespace
   *     bindings etc.
   * @throws IOException in case anything goes wrong
   * @throws XMLStreamException in case anything goes wrong
   */
  public void generateServiceMetadata(
      JsonIsoMetadata metadata, Service service, XMLStreamWriter writer)
      throws IOException, XMLStreamException {
    writeFileIdentifier(writer, service.getFileIdentifier());
    writeLanguage(writer);
    writeCharacterSet(writer);
    writeHierarchyLevel(writer, MD_ScopeCode.service);
    writeHierarchyLevelName(writer, metadata.getTitle());
    writeContact(writer, DEFAULT_CONTACT, "contact");
    writeDateStamp(writer, metadata);
    writeMetadataInfo(writer, !metadata.getMetadataProfile().equals(MetadataProfile.ISO));
    writeCrs(writer, metadata);
    writeServiceIdentification(writer, service, metadata);
    writeDistributionInfo(writer, metadata, service);
    writeDataQualityInfo(writer, metadata, service);
  }

  private void writeHierarchyLevelName(XMLStreamWriter writer, String title)
      throws XMLStreamException {
    writer.writeStartElement(GMD, "hierarchyLevelName");
    writeSimpleElement(writer, GCO, "CharacterString", String.format("%s Dienst", title));
    writer.writeEndElement(); // hierarchyLevelName
  }

  public void generateServiceMetadata(JsonIsoMetadata metadata, Service service, OutputStream out)
      throws IOException, XMLStreamException {
    var writer = FACTORY.createXMLStreamWriter(out);
    setNamespaceBindings(writer);
    writer.writeStartDocument();
    writer.writeStartElement(GMD, "MD_Metadata");
    writeNamespaceBindings(writer);
    generateServiceMetadata(metadata, service, writer);
    writer.writeEndElement(); // MD_Metadata
    writer.writeEndDocument();
    writer.close();
    out.close();
  }
}
