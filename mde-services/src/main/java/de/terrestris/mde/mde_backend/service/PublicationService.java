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

  @Value("${csw.server}")
  private String cswServer;

  @Value("${csw.user}")
  private String cswUser;

  @Value("${csw.password}")
  private String cswPassword;

  @Autowired
  private DatasetIsoGenerator datasetIsoGenerator;

  @Autowired
  private ServiceIsoGenerator serviceIsoGenerator;

  @Autowired
  private IsoMetadataService metadataService;

  @PostConstruct
  public void completeCswUrl() {
    if (!cswServer.endsWith("/")) {
      cswServer = cswServer + "/";
    }
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

  public void publishMetadata(String metadataId) throws XMLStreamException, IOException, URISyntaxException, InterruptedException {
    var metadata = metadataService.findOneByMetadataId(metadataId);
    if (metadata.isEmpty()) {
      log.info("Metadata with ID {} is not available.", metadataId);
      return;
    }
    var entity = metadata.get();
    var data = entity.getData();
    if (log.isTraceEnabled()) {
      saveTransaction(writer -> {
        try {
          datasetIsoGenerator.generateDatasetMetadata(data, metadataId, writer);
        } catch (IOException | XMLStreamException e) {
          log.warn("Unable to generate dataset metadata: {}", e.getMessage());
          log.trace("Stack trace:", e);
        }
        return null;
      }, data.getFileIdentifier() == null);
    }
    sendTransaction(writer -> {
      try {
        datasetIsoGenerator.generateDatasetMetadata(data, metadataId, writer);
      } catch (IOException | XMLStreamException e) {
        log.warn("Unable to generate dataset metadata: {}", e.getMessage());
        log.trace("Stack trace:", e);
      }
      return null;
    }, data.getFileIdentifier() == null, data);
    if (data.getServices() != null) {
      data.getServices().forEach(service -> {
        try {
          if (log.isTraceEnabled()) {
            saveTransaction(writer -> {
              try {
                serviceIsoGenerator.generateServiceMetadata(data, service, writer);
              } catch (IOException | XMLStreamException e) {
                log.warn("Unable to generate service metadata: {}", e.getMessage());
                log.trace("Stack trace:", e);
              }
              return null;
            }, data.getFileIdentifier() == null);
          }
          sendTransaction(writer -> {
            try {
              serviceIsoGenerator.generateServiceMetadata(data, service, writer);
            } catch (IOException | XMLStreamException e) {
              log.warn("Unable to generate service metadata: {}", e.getMessage());
              log.trace("Stack trace:", e);
            }
            return null;
          }, data.getFileIdentifier() == null, service);
        } catch (URISyntaxException | IOException | InterruptedException | XMLStreamException e) {
          log.warn("Unable to generate service metadata: {}", e.getMessage());
          log.trace("Stack trace:", e);
        }
      });
    }
    metadataService.update(entity.getId(), entity);
  }

}
