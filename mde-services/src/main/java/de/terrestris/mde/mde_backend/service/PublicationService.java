package de.terrestris.mde.mde_backend.service;

import static de.terrestris.utils.xml.MetadataNamespaceUtils.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.nimbusds.jose.util.Base64;
import de.terrestris.mde.mde_backend.enumeration.Role;
import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.jpa.ServiceDeletionRepository;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.Status;
import de.terrestris.mde.mde_backend.model.dto.MetadataDeletionResponse;
import de.terrestris.mde.mde_backend.model.json.CommonFields;
import de.terrestris.mde.mde_backend.model.json.Service;
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
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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

  @Autowired protected MessageSource messageSource;

  @Autowired private ServiceDeletionRepository serviceDeletionRepository;

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
      Function<XMLStreamWriter, Void> generator, boolean insert, CommonFields object)
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
    var bs = out.toByteArray();
    var in = new ByteArrayInputStream(bs);
    if (log.isTraceEnabled()) {
      var tmp = Files.createTempFile(null, null);
      log.debug("Writing transaction to {}", tmp);
      IOUtils.copy(new ByteArrayInputStream(bs), Files.newOutputStream(tmp));
    }
    var publisher = HttpRequest.BodyPublishers.ofInputStream(() -> in);
    try (var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
      var builder = HttpRequest.newBuilder(new URI(cswServer));
      var req = builder.POST(publisher);
      var encoded = Base64.encode(String.format("%s:%s", cswUser, cswPassword)).toString();
      req.header("Authorization", "Basic " + encoded).header("Content-Type", "application/xml");
      log.debug("Sending request");
      var response = client.send(req.build(), HttpResponse.BodyHandlers.ofInputStream());
      log.debug("Sent request");
      var is = response.body();

      var bytes = IOUtils.toByteArray(is);
      if (log.isTraceEnabled()) {
        log.debug("Response: {}", new String(bytes, UTF_8));
      }

      if (!HttpStatus.valueOf(response.statusCode()).is2xxSuccessful()) {
        throw new IOException("Catalog server returned status code " + response.statusCode());
      }

      var reader = INPUT_FACTORY.createXMLStreamReader(new ByteArrayInputStream(bytes));

      var insertedRecords = 0;
      var updatedRecords = 0;

      while (reader.hasNext()) {
        reader.next();

        if (!reader.isStartElement()) {
          continue;
        }

        if (insert && reader.getLocalName().equals("totalInserted")) {
          insertedRecords = Integer.parseInt(reader.getElementText());
          log.debug("Inserted {} record(s)", insertedRecords);
        }

        if (!insert && reader.getLocalName().equals("totalUpdated")) {
          updatedRecords = Integer.parseInt(reader.getElementText());
          log.debug("Updated {} record(s)", updatedRecords);
        }

        if (reader.getLocalName().equals("identifier")) {
          var id = reader.getElementText();
          log.debug("Extracted file identifier {}", id);
          object.setFileIdentifier(id);
        }
      }

      if (insertedRecords == 0 && updatedRecords == 0) {
        log.error("No records inserted or updated");
        throw new IOException("No records inserted or updated");
      }
    }
  }

  private ArrayList<String> publishRecords(List<String> uuids)
      throws URISyntaxException, IOException, InterruptedException {

    var successfullyPublishedUuids = new ArrayList<String>();

    try (var client = HttpClient.newHttpClient()) {
      var builder = HttpRequest.newBuilder(new URI(meUrl));
      var req = builder.POST(HttpRequest.BodyPublishers.noBody());
      var response = client.send(req.build(), HttpResponse.BodyHandlers.discarding());
      var cookiesList = response.headers().allValues("set-cookie");
      var csrfCandidate =
          cookiesList.stream()
              .map(s -> s.split(";")[0])
              .filter(s -> s.startsWith("XSRF"))
              .findFirst();

      if (csrfCandidate.isEmpty()) {
        throw new IOException("XSRF token not found in response headers");
      }

      var csrf = csrfCandidate.get().split("=")[1];

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
        response = client.send(req.build(), HttpResponse.BodyHandlers.discarding());

        if (!HttpStatus.valueOf(response.statusCode()).is2xxSuccessful()) {
          log.error(
              "Could not publish record with ID {}. Status code: {}", uuid, response.statusCode());
          continue;
        }

        log.debug("Successfully published the record");
        log.debug("Response status: {}", response.statusCode());

        successfullyPublishedUuids.add(uuid);
      }
    }

    return successfullyPublishedUuids;
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR')")
  public void publishAllMetadata() {
    metadataCollectionRepository
        .findAll()
        .forEach(
            metadata -> {
              try {
                publishMetadata(metadata, true);
              } catch (XMLStreamException
                  | IOException
                  | URISyntaxException
                  | InterruptedException
                  | PublicationException e) {
                log.error(
                    "Unable to publish metadata with ID {}: {}",
                    metadata.getMetadataId(),
                    e.getMessage());
                log.trace("Stack trace:", e);
              }
            });
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR')")
  public ArrayList<String> publishMetadata(MetadataCollection metadata, boolean force)
      throws XMLStreamException,
          IOException,
          URISyntaxException,
          InterruptedException,
          PublicationException {
    var metadataId = metadata.getMetadataId();
    if (!force && (metadata.getApproved() == null || !metadata.getApproved())) {
      throw new PublicationException("Metadata with ID " + metadataId + " is not approved.");
    }

    if (!force
        && (metadata.getResponsibleRole() == null
            || !metadata.getResponsibleRole().equals(Role.MdeEditor))) {
      throw new PublicationException(
          "Metadata with ID "
              + metadataId
              + " is not assigned to "
              + "required role "
              + Role.MdeEditor
              + ".");
    }

    var isoMetadata = metadata.getIsoMetadata();
    if (!force) {
      isoMetadata.setDateTime(Instant.now());
    }
    var uuids = new ArrayList<String>();
    var insertIsoMetadata = isoMetadata.getFileIdentifier() == null || force;

    sendTransaction(
        writer -> {
          try {
            if (insertIsoMetadata) {
              isoMetadata.setFileIdentifier(UUID.randomUUID().toString());
            }
            datasetIsoGenerator.generateDatasetMetadata(isoMetadata, metadataId, writer);
            uuids.add(isoMetadata.getFileIdentifier());
          } catch (IOException | XMLStreamException e) {
            log.warn("Unable to generate dataset metadata: {}", e.getMessage());
            log.trace("Stack trace:", e);
          }
          return null;
        },
        insertIsoMetadata,
        isoMetadata);
    if (isoMetadata.getServices() != null) {
      isoMetadata
          .getServices()
          .forEach(
              service -> {
                var insertService = service.getFileIdentifier() == null || force;

                try {
                  sendTransaction(
                      writer -> {
                        try {
                          if (insertService) {
                            service.setFileIdentifier(UUID.randomUUID().toString());
                          }
                          serviceIsoGenerator.generateServiceMetadata(isoMetadata, service, writer);
                          uuids.add(service.getFileIdentifier());
                        } catch (IOException | XMLStreamException e) {
                          log.warn("Unable to generate service metadata: {}", e.getMessage());
                          log.trace("Stack trace:", e);
                        }
                        return null;
                      },
                      insertService,
                      service);
                } catch (URISyntaxException
                    | IOException
                    | InterruptedException
                    | XMLStreamException e) {
                  log.warn("Unable to publish the service metadata: {}", e.getMessage());
                  log.trace("Stack trace:", e);
                }
              });
    }

    metadata.setStatus(Status.PUBLISHED);

    // remove assignment and responsible role after publication
    metadata.setAssignedUserId(null);
    metadata.setResponsibleRole(null);

    metadataCollectionRepository.save(metadata);

    deleteOldServiceMetadata(metadata);

    return publishRecords(uuids);
  }

  private void deleteOldServiceMetadata(MetadataCollection metadataCollection) {
    var deletions = serviceDeletionRepository.findByMetadataId(metadataCollection.getMetadataId());
    for (var deletion : deletions) {
      try {
        removeMetadata(deletion.getFileIdentifier());
        log.debug(
            "Successfully deleted old service metadata with file identifier {}",
            deletion.getFileIdentifier());
      } catch (URISyntaxException | IOException | InterruptedException | XMLStreamException e) {
        log.error(
            "Error while deleting old service metadata with file identifier {}: {}",
            deletion.getFileIdentifier(),
            e.getMessage());
        log.trace("Stack trace:", e);
      }
    }
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR')")
  public ArrayList<String> publishMetadata(String metadataId)
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
    return publishMetadata(metadata, false);
  }

  @PreAuthorize("hasRole('ROLE_MDEADMINISTRATOR') or hasRole('ROLE_MDEEDITOR')")
  public MetadataDeletionResponse deleteMetadata(String metadataId, Authentication auth) {
    var metadataCollection =
        metadataCollectionRepository
            .findByMetadataId(metadataId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

    var userId = auth.getName();

    // Admin can always delete metadata collections
    if (!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MDEADMINISTRATOR"))) {
      var assignedUserId = metadataCollection.getAssignedUserId();
      var ownerId = metadataCollection.getOwnerId();
      var teamMemberIds = metadataCollection.getTeamMemberIds();

      // Editor can only delete metadata collections that are assigned to them
      if (assignedUserId != null && !assignedUserId.equals(userId)) {
        throw new ResponseStatusException(CONFLICT);
      }

      // Editor can only delete metadata collections that are owned by them or where they are team
      // members
      var isOwner = ownerId != null && ownerId.equals(userId);
      var isTeamMember = teamMemberIds != null && teamMemberIds.contains(userId);
      if (!isOwner && !isTeamMember) {
        throw new ResponseStatusException(CONFLICT);
      }
    }

    var response = new MetadataDeletionResponse();
    var catalogRecords = new ArrayList<String>();
    Set<String> identifiersToDelete = new HashSet<>();

    List<Service> services = metadataCollection.getIsoMetadata().getServices();
    if (services != null) {
      identifiersToDelete =
          services.stream().map(Service::getFileIdentifier).collect(Collectors.toSet());
    } else {
      log.warn("No services found for metadata collection with ID {}", metadataId);
    }

    String fileIdentifier = metadataCollection.getIsoMetadata().getFileIdentifier();
    if (fileIdentifier != null) {
      identifiersToDelete.add(fileIdentifier);
    }

    identifiersToDelete.forEach(
        identifier -> {
          try {
            removeMetadata(identifier);
            catalogRecords.add(identifier);
          } catch (Exception e) {
            log.error(
                "Error while removing catalog entry with id {}: \n {}", identifier, e.getMessage());
            log.trace("Full stack trace: ", e);
          }
        });

    metadataCollectionRepository.delete(metadataCollection);

    response.setDeletedCatalogRecords(catalogRecords);
    response.setDeletedMetadataCollection(metadataId);

    return response;
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
