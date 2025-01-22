package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.codelists.MD_ScopeCode;
import de.terrestris.mde.mde_backend.model.json.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static de.terrestris.mde.mde_backend.model.json.codelists.CI_DateTypeCode.*;
import static de.terrestris.mde.mde_backend.model.json.codelists.CI_PresentationFormCode.mapDigital;
import static de.terrestris.mde.mde_backend.service.GeneratorUtils.*;
import static de.terrestris.mde.mde_backend.utils.NamespaceUtils.*;
import static de.terrestris.utils.xml.XmlUtils.writeSimpleElement;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ServiceIsoGenerator {

  private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();

  private static void writeServiceIdentification(XMLStreamWriter writer, Service service, JsonIsoMetadata metadata) throws XMLStreamException {
    writer.writeStartElement(GMD, "gmd:identificationInfo");
    writer.writeStartElement(SRV, "SV_ServiceIdentification");
    writer.writeAttribute("uuid", service.getServiceIdentification());
    writer.writeStartElement(GMD, "citation");
    writer.writeStartElement(GMD, "CI_Citation");
    writer.writeStartElement(GMD, "title");
    writeSimpleElement(writer, GCO, "CharacterString", service.getTitle());
    writer.writeEndElement(); // title
    writeDate(writer, service.getCreated(), creation);
    writeDate(writer, service.getPublished(), publication);
    writeDate(writer, service.getUpdated(), revision);
    writer.writeStartElement(GMD, "identifier");
    writer.writeStartElement(GMD, "MD_Identifier");
    writer.writeStartElement(GMD, "code");
    writeSimpleElement(writer, GCO, "CharacterString", String.format("https://registry.gdi-de.org/id/de.be.csw/%s", service.getServiceIdentification()));
    writer.writeEndElement(); // code
    writer.writeEndElement(); // MD_Identifier
    writer.writeEndElement(); // identifier
    writer.writeStartElement(GMD, "presentationForm");
    writeCodelistValue(writer, mapDigital);
    writer.writeEndElement(); // presentationForm
    writer.writeEndElement(); // CI_Citation
    writer.writeEndElement(); // citation
    writer.writeStartElement(GMD, "abstract");
    writeSimpleElement(writer, GCO, "CharacterString", service.getShortDescription());
    writer.writeEndElement(); // abstract
    for (var contact : metadata.getPointsOfContact()) {
      writeContact(writer, contact, "pointOfContact");
    }
    // TODO continue here
    writer.writeEndElement(); // SV_ServiceIdentification
    writer.writeEndElement(); // identificationInfo
  }

  public String generateServiceMetadata(JsonIsoMetadata metadata, Service service) throws IOException, XMLStreamException {
    try (var out = new ByteArrayOutputStream()) {
      var writer = FACTORY.createXMLStreamWriter(out);
      setNamespaceBindings(writer);
      writer.writeStartDocument();
      writer.writeStartElement(GMD, "MD_Metadata");
      writeNamespaceBindings(writer);
      writeFileIdentifier(writer, service.getFileIdentifier());
      writeLanguage(writer);
      writeCharacterSet(writer);
      writeHierarchyLevel(writer, MD_ScopeCode.service);
      for (var contact : metadata.getContacts()) {
        writeContact(writer, contact, "contact");
      }
      writeDateStamp(writer, metadata);
      writeMetadataInfo(writer);
      writeCrs(writer, metadata);
      writeServiceIdentification(writer, service, metadata);
      // TODO continue here
      writer.writeEndElement(); // MD_Metadata
      writer.writeEndDocument();
      writer.close();
      out.close();
      return out.toString(UTF_8);
    }
  }

}
