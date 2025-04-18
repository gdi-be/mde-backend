package de.terrestris.mde.mde_backend.service;

import com.nimbusds.jose.util.Base64;
import de.terrestris.mde.mde_backend.model.json.FileIdentifier;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static java.nio.charset.StandardCharsets.UTF_8;

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

  @Autowired
  private DatasetIsoGenerator datasetIsoGenerator;

  @Autowired
  private ServiceIsoGenerator serviceIsoGenerator;

  @Autowired
  private MetadataCollectionService metadataCollectionService;

  @PostConstruct
  public void completeCswUrl() {
    if (!cswServer.endsWith("/")) {
      cswServer = cswServer + "/";
    }
    publicationUrl = String.format("%ssrv/api/records", cswServer);
    meUrl = String.format("%ssrv/api/me", cswServer);
    cswServer = String.format("%ssrv/eng/csw-publication", cswServer);
  }

  private void sendTransaction(Function<XMLStreamWriter, Void> generator, boolean insert, FileIdentifier object) throws URISyntaxException, IOException, InterruptedException, XMLStreamException {
    var publisher = HttpRequest.BodyPublishers.ofInputStream(() -> {
      var in = new PipedInputStream();
      try (var pool = ForkJoinPool.commonPool()) {
        pool.submit(() -> {
          var out = new BufferedOutputStream(new PipedOutputStream(in));
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
          return null;
        });
      }
      return in;
    });
    try (var client = HttpClient.newHttpClient()) {
      var builder = HttpRequest.newBuilder(new URI(cswServer));
      var req = builder.POST(publisher);
      var encoded = Base64.encode(String.format("%s:%s", cswUser, cswPassword)).toString();
      req.header("Authorization", "Basic " + encoded)
        .header("Content-Type", "application/xml");
      log.debug("Sending request");
      var response = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
      log.debug("Sent request");
      var in = response.body();
      if (log.isTraceEnabled()) {
        var bs = IOUtils.toByteArray(in);
        in = new ByteArrayInputStream(bs);
        log.debug("Response: {}", new String(bs, UTF_8));
      }
      var reader = INPUT_FACTORY.createXMLStreamReader(in);
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

  private void saveTransaction(Function<XMLStreamWriter, Void> generator, boolean insert) throws IOException, XMLStreamException {
    var tmp = Files.createTempFile(null, null);
    log.debug("Writing transaction to {}", tmp);
    var out = new BufferedOutputStream(Files.newOutputStream(tmp));
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
  }

  private void publishRecords(List<String> uuids) throws URISyntaxException, IOException, InterruptedException {
    try (var client = HttpClient.newHttpClient()) {
      var builder = HttpRequest.newBuilder(new URI(meUrl));
      var req = builder.GET();
      var response = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
      var cookiesList = response.headers().allValues("set-cookie");
      var csrf = cookiesList.stream().map(s -> s.split(";")[0]).filter(s -> s.startsWith("XSRF")).findFirst().get().split("=")[1];
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
  }

  public void publishMetadata(String metadataId) throws XMLStreamException, IOException, URISyntaxException, InterruptedException {
    var metadata = metadataCollectionService.findOneByMetadataId(metadataId);
    if (metadata.isEmpty()) {
      log.info("Metadata with ID {} is not available.", metadataId);
      return;
    }
    var entity = metadata.get();
    var isoMetadata = entity.getIsoMetadata();
    var uuids = new ArrayList<String>();
    var insert = isoMetadata.getFileIdentifier() == null;
    if (log.isTraceEnabled()) {
      saveTransaction(writer -> {
        try {
          datasetIsoGenerator.generateDatasetMetadata(isoMetadata, metadataId, writer);
        } catch (IOException | XMLStreamException e) {
          log.warn("Unable to generate dataset metadata: {}", e.getMessage());
          log.trace("Stack trace:", e);
        }
        return null;
      }, insert);
    }
    sendTransaction(writer -> {
      try {
        datasetIsoGenerator.generateDatasetMetadata(isoMetadata, metadataId, writer);
        uuids.add(isoMetadata.getFileIdentifier());
      } catch (IOException | XMLStreamException e) {
        log.warn("Unable to generate dataset metadata: {}", e.getMessage());
        log.trace("Stack trace:", e);
      }
      return null;
    }, insert, isoMetadata);
    if (isoMetadata.getServices() != null) {
      isoMetadata.getServices().forEach(service -> {
        try {
          if (log.isTraceEnabled()) {
            saveTransaction(writer -> {
              try {
                serviceIsoGenerator.generateServiceMetadata(isoMetadata, service, writer);
              } catch (IOException | XMLStreamException e) {
                log.warn("Unable to generate service metadata: {}", e.getMessage());
                log.trace("Stack trace:", e);
              }
              return null;
            }, insert);
          }
          sendTransaction(writer -> {
            try {
              serviceIsoGenerator.generateServiceMetadata(isoMetadata, service, writer);
              uuids.add(service.getFileIdentifier());
            } catch (IOException | XMLStreamException e) {
              log.warn("Unable to generate service metadata: {}", e.getMessage());
              log.trace("Stack trace:", e);
            }
            return null;
          }, insert, service);
        } catch (URISyntaxException | IOException | InterruptedException | XMLStreamException e) {
          log.warn("Unable to generate service metadata: {}", e.getMessage());
          log.trace("Stack trace:", e);
        }
      });
    }
    metadataCollectionService.update(entity.getId(), entity);
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
  public void removeMetadata(String metadataId) throws URISyntaxException, IOException, InterruptedException, XMLStreamException {
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
      req.header("Authorization", "Basic " + encoded)
        .header("Content-Type", "application/xml");

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
        throw new IOException("No records deleted for metadata with ID " + metadataId + ". This is probably due to " +
          "a wrong or non existing metadata ID.");
      } else {
        log.debug("Deleted {} records for metadata with ID {}", deletedRecords, metadataId);
      }
    }
  }
}
