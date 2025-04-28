package de.terrestris.mde.mde_backend.service;

import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.nimbusds.jose.util.Base64;
import de.terrestris.mde.mde_backend.enumeration.Role;
import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.Status;
import de.terrestris.mde.mde_backend.model.json.FileIdentifier;
import de.terrestris.mde.mde_backend.utils.PublicationException;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class PublicationService {

  private static final XMLOutputFactory FACTORY = XMLOutputFactory2.newFactory();

  private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory2.newFactory();

  private static final String CSW = "http://www.opengis.net/cat/csw/2.0.2";

  private static final String OGC = "http://www.opengis.net/ogc";

  @Value("${csw.server:}")
  private String cswServer;

  @Value("${csw.user:}")
  private String cswUser;

  @Value("${csw.password:}")
  private String cswPassword;

  private String publicationUrl;

  private String meUrl;

  @Autowired private DatasetIsoGenerator datasetIsoGenerator;

  @Autowired private ServiceIsoGenerator serviceIsoGenerator;

  @Autowired private MetadataCollectionService metadataCollectionService;

  @Autowired private MetadataCollectionRepository metadataCollectionRepository;

  @PostConstruct
  public void completeCswUrl() {
    if (!cswServer.endsWith("/")) {
      cswServer = cswServer + "/";
    }
    publicationUrl = String.format("%ssrv/api/records", cswServer);
    meUrl = String.format("%ssrv/api/me", cswServer);
    cswServer = String.format("%ssrv/eng/csw-publication", cswServer);
  }

  private void sendTransaction(
      Function<XMLStreamWriter, Void> generator, boolean insert, FileIdentifier object)
      throws URISyntaxException, IOException, InterruptedException, XMLStreamException {
    var out = new ByteArrayOutputStream();
    var writer = FACTORY.createXMLStreamWriter(out);
    setNamespaceBindings(writer);
    writer.setPrefix("csw", CSW);
    writer.writeStartDocument();
    writer.writeStartElement(CSW, "Transaction");
    writeNamespaceBindings(writer);
    writer.writeNamespace("csw", CSW);
    writer.writeAttribute("service", "CSW");
    writer.writeAttribute("version", "2.0.2");
    writer.writeStartElement(CSW, insert ? "Insert" : "Update");
    writer.writeStartElement(GMD, "MD_Metadata");
    log.debug("Writing document");
    generator.apply(writer);
    log.debug("Wrote document");
    writer.writeEndElement(); // MD_Metadata
    writer.writeEndElement(); // Insert/Update
    writer.writeEndElement(); // Transaction
    writer.flush();
    writer.close();
    out.flush();
    out.close();
    var in = new ByteArrayInputStream(out.toByteArray());
    if (log.isTraceEnabled()) {
      var tmp = Files.createTempFile(null, null);
      log.debug("Writing transaction to {}", tmp);
      IOUtils.copy(new ByteArrayInputStream(out.toByteArray()), Files.newOutputStream(tmp));
    }
    var publisher = HttpRequest.BodyPublishers.ofInputStream(() -> in);
    try (var client = HttpClient.newHttpClient()) {
      var builder = HttpRequest.newBuilder(new URI(cswServer));
      var req = builder.POST(publisher);
      var encoded = Base64.encode(String.format("%s:%s", cswUser, cswPassword)).toString();
      req.header("Authorization", "Basic " + encoded).header("Content-Type", "application/xml");
      log.debug("Sending request");
      var response = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
      log.debug("Sent request");
      var is = response.body();
      if (log.isTraceEnabled()) {
        var bs = IOUtils.toByteArray(is);
        is = new ByteArrayInputStream(bs);
        log.debug("Response: {}", new String(bs, UTF_8));
      }
      var reader = INPUT_FACTORY.createXMLStreamReader(is);
      while (reader.hasNext()) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("identifier")) {
          var id = reader.getElementText();
          log.debug("Extracted file identifier {}", id);
          object.setFileIdentifier(id);
        }
      }
    }
  }

  private void publishRecords(List<String> uuids)
      throws URISyntaxException, IOException, InterruptedException {
    // HBD: when using try-with-resources the publish request hangs indefinitely for an unknown
    // reason
    var client = HttpClient.newHttpClient();
    var builder = HttpRequest.newBuilder(new URI(meUrl));
    var req = builder.GET();
    var response = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
    var cookiesList = response.headers().allValues("set-cookie");
    var csrf =
        cookiesList.stream()
            .map(s -> s.split(";")[0])
            .filter(s -> s.startsWith("XSRF"))
            .findFirst()
            .get()
            .split("=")[1];
    var cookies = String.join("; ", cookiesList.stream().map(s -> s.split(";")[0]).toList());
    for (var uuid : uuids) {
      var url = String.format("%s/%s/publish?publicationType=", publicationUrl, uuid);
      builder = HttpRequest.newBuilder(new URI(url));
      req = builder.PUT(HttpRequest.BodyPublishers.ofString(""));
      var encoded = Base64.encode(String.format("%s:%s", cswUser, cswPassword)).toString();
      req.header("Authorization", "Basic " + encoded)
          .header("X-XSRF-TOKEN", csrf)
          .header("Cookie", cookies);
      log.debug("Publishing record");
      response = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
      log.debug("Published record");
      log.debug("Response status: {}", response.statusCode());
    }
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR')")
  public void publishMetadata(String metadataId)
      throws XMLStreamException,
          IOException,
          URISyntaxException,
          InterruptedException,
          PublicationException {
    var metadata =
        metadataCollectionRepository
            .findByMetadataId(metadataId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Metadata with ID " + metadataId + " not found."));

    if (metadata.getApproved() == null || !metadata.getApproved()) {
      throw new PublicationException("Metadata with ID " + metadataId + " is not approved.");
    }

    if (metadata.getAssignedUserId() == null) {
      throw new PublicationException(
          "Metadata with ID " + metadataId + " is not assigned to a user.");
    }

    if (metadata.getResponsibleRole() == null
        || !metadata.getResponsibleRole().equals(Role.MdeEditor)) {
      throw new PublicationException(
          "Metadata with ID "
              + metadataId
              + " is not assigned to "
              + "required role "
              + Role.MdeEditor
              + ".");
    }

    metadata.setStatus(Status.PUBLISHED);

    var isoMetadata = metadata.getIsoMetadata();
    var uuids = new ArrayList<String>();
    var insert = isoMetadata.getFileIdentifier() == null;
    sendTransaction(
        writer -> {
          try {
            datasetIsoGenerator.generateDatasetMetadata(isoMetadata, metadataId, writer);
            uuids.add(isoMetadata.getFileIdentifier());
          } catch (IOException | XMLStreamException e) {
            log.warn("Unable to generate dataset metadata: {}", e.getMessage());
            log.trace("Stack trace:", e);
          }
          return null;
        },
        insert,
        isoMetadata);
    if (isoMetadata.getServices() != null) {
      isoMetadata
          .getServices()
          .forEach(
              service -> {
                try {
                  sendTransaction(
                      writer -> {
                        try {
                          serviceIsoGenerator.generateServiceMetadata(isoMetadata, service, writer);
                          uuids.add(service.getFileIdentifier());
                        } catch (IOException | XMLStreamException e) {
                          log.warn("Unable to generate service metadata: {}", e.getMessage());
                          log.trace("Stack trace:", e);
                        }
                        return null;
                      },
                      insert,
                      service);
                } catch (URISyntaxException
                    | IOException
                    | InterruptedException
                    | XMLStreamException e) {
                  log.warn("Unable to generate service metadata: {}", e.getMessage());
                  log.trace("Stack trace:", e);
                }
              });
    }

    metadataCollectionRepository.save(metadata);
    publishRecords(uuids);
  }

  /**
   * Removes the given record from the catalog server.
   *
   * @param metadataId The identifier of the metadata to be removed.
   * @throws URISyntaxException
   * @throws IOException
   * @throws InterruptedException
   * @throws XMLStreamException
   */
  public void removeMetadata(String metadataId)
      throws URISyntaxException, IOException, InterruptedException, XMLStreamException {
    StringWriter stringWriter = new StringWriter();
    var writer = FACTORY.createXMLStreamWriter(stringWriter);
    setNamespaceBindings(writer);
    writer.setPrefix("csw", CSW);
    writer.writeStartDocument();
    writer.writeStartElement(CSW, "Transaction");
    writeNamespaceBindings(writer);
    writer.writeNamespace("csw", CSW);
    writer.writeNamespace("ogc", OGC);
    writer.writeAttribute("service", "CSW");
    writer.writeAttribute("version", "2.0.2");
    writer.writeStartElement(CSW, "Delete");
    writer.writeStartElement(CSW, "Constraint");
    writer.writeAttribute("version", "1.1.0");
    writer.writeStartElement(OGC, "Filter");
    writer.writeStartElement(OGC, "PropertyIsEqualTo");
    writer.writeStartElement(OGC, "PropertyName");
    writer.writeCharacters("dc:identifier");
    writer.writeEndElement(); // PropertyName
    writer.writeStartElement(OGC, "Literal");
    writer.writeCharacters(metadataId);
    writer.writeEndElement(); // Literal
    writer.writeEndElement(); // PropertyIsEqualTo
    writer.writeEndElement(); // Filter
    writer.writeEndElement(); // Constraint
    writer.writeEndElement(); // Delete
    writer.writeEndElement(); // Transaction
    writer.flush();
    writer.close();
    String xmlString = stringWriter.toString();

    try (var client = HttpClient.newHttpClient()) {
      var builder = HttpRequest.newBuilder(new URI(cswServer));
      var body = HttpRequest.BodyPublishers.ofString(xmlString);
      var req = builder.POST(body);
      var encoded = Base64.encode(String.format("%s:%s", cswUser, cswPassword)).toString();
      req.header("Authorization", "Basic " + encoded).header("Content-Type", "application/xml");

      log.debug("Sending authenticated request to CSW server at {}", cswServer);

      var response = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());

      var in = response.body();

      if (log.isTraceEnabled()) {
        var bs = IOUtils.toByteArray(in);
        in = new ByteArrayInputStream(bs);
        log.trace("Response is: {}", new String(bs, UTF_8));
      }

      if (!HttpStatus.valueOf(response.statusCode()).is2xxSuccessful()) {
        throw new IOException("Catalog server returned status code " + response.statusCode());
      }

      var reader = INPUT_FACTORY.createXMLStreamReader(in);
      var deletedRecords = 0;
      while (reader.hasNext()) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        if (reader.getLocalName().equals("totalDeleted")) {
          deletedRecords = Integer.parseInt(reader.getElementText());
          break;
        }
      }

      if (deletedRecords == 0) {
        throw new IOException(
            "No records deleted for metadata with ID "
                + metadataId
                + ". This is probably due to "
                + "a wrong or non existing metadata ID.");
      } else {
        log.debug("Deleted {} records for metadata with ID {}", deletedRecords, metadataId);
      }
    }
  }
}
