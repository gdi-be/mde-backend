package de.terrestris.mde.mde_backend.service;

import static de.terrestris.bkgtestsuite.core.model.TestResult.MessageType.TEXT;
import static de.terrestris.bkgtestsuite.core.model.TestResult.ResultStatus.*;

import de.terrestris.bkgtestsuite.core.model.TestParameters;
import de.terrestris.bkgtestsuite.core.model.TestResult;
import de.terrestris.bkgtestsuite.core.spi.TestRunner;
import de.terrestris.bkgtestsuite.te.TeTestRunner;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.enumeration.ValidationStatus;
import de.terrestris.mde.mde_backend.event.sse.validation.ValidationEventPublisher;
import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.dto.sse.ValidationMessage;
import de.terrestris.mde.mde_backend.thread.TrackedTask;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Log4j2
public class ValidatorService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Value("classpath:/bkg-scripts-metadata/src/scripts/config.json")
  private Resource inspireConfig;

  @Value("classpath:/bkg-scripts-metadata/src/config-iso/config.json")
  private Resource isoConfig;

  @Value("classpath:/bkg-scripts-metadata/src/**/*")
  private List<Resource> metadataResources;

  @Autowired private MetadataCollectionRepository metadataCollectionRepository;

  @Autowired private IsoGenerator generator;

  @Autowired private ValidationEventPublisher eventPublisher;

  private TestRunner isoRunner;

  private TestRunner inspireRunner;

  private final List<Map<String, String>> inspireClasses =
      List.of(
          Map.of("id", "ISO-Schemavalidierung"),
          Map.of("id", "GDI-DE_INSPIRE_verpflichtend"),
          Map.of("id", "GDI-DE_INSPIRE_konditional"),
          Map.of("id", "GDI-DE_konditional"),
          Map.of("id", "GDI-DE_optional"),
          Map.of("id", "OpenData_konditional"));

  private final List<Map<String, String>> isoClasses =
      List.of(
          Map.of("id", "ISO-Schemavalidierung"),
          Map.of("id", "GDI-DE_verpflichtend"),
          Map.of("id", "GDI-DE_konditional"),
          Map.of("id", "GDI-DE_optional"),
          Map.of("id", "OpenData_konditional"));

  @PostConstruct
  public void loadIsoPackage() throws IOException {
    var excluded = List.of("config.json", "main.ctl", "inspire_only.ctl");
    var list = extractResources(metadataResources, file -> file.toString().contains("src/scripts"));
    list = list.stream().filter(f -> !excluded.contains(f.getName())).collect(Collectors.toList());
    isoRunner =
        new TeTestRunner(
            isoConfig.getInputStream(), list, new File("/tmp/bkg-scripts-metadata/src/resources"));
  }

  @PostConstruct
  public void loadInspirePackage() throws IOException {
    var excluded = List.of("config.json", "main2.ctl", "iso_only.ctl");
    var list = extractResources(metadataResources, file -> file.toString().contains("src/scripts"));
    list = list.stream().filter(f -> !excluded.contains(f.getName())).collect(Collectors.toList());
    inspireRunner =
        new TeTestRunner(
            inspireConfig.getInputStream(),
            list,
            new File("/tmp/bkg-scripts-metadata/src/resources"));
  }

  private static List<File> extractResources(List<Resource> resources, Predicate<File> tester)
      throws IOException {
    var list = new ArrayList<File>();
    var tmp = new File("/tmp/");
    for (Resource resource : resources) {
      var path = ((ClassPathResource) resource).getPath();
      var file = new File(tmp, path);
      if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
        log.warn("Unable to create script directory: {}", file.getParentFile());
      }
      IOUtils.copy(resource.getInputStream(), Files.newOutputStream(file.toPath()));
      if (tester.test(file)) {
        list.add(file);
      }
    }
    return list;
  }

  private void extractErrors(TestResult result, List<String> errors) {
    if (result.getStatus().equals(Failure)
        || result.getStatus().equals(TechnicalFailure)
        || result.getStatus().equals(InheritedFailure)) {
      errors.addAll(
          result.getMessages().stream()
              .filter(m -> m.getType().equals(TEXT))
              .map(TestResult.Message::getText)
              .toList());
    }
    for (var sub : result.getSubresults()) {
      extractErrors(sub, errors);
    }
  }

  private List<String> validate(boolean inspire, Path file, boolean dataset) {
    try {
      var runner = inspire ? inspireRunner : isoRunner;
      var classes = inspire ? inspireClasses : isoClasses;
      var errors = new ArrayList<String>();
      log.debug("Running GDI-DE tests...");
      runTest(
          file,
          runner,
          errors,
          classes,
          inspire ? "Ergebnisse GDI-DE für INSPIRE:" : "Ergebnisse GDI-DE:");
      return errors;
    } catch (Exception e) {
      log.error("Error when validating: {}", e.getMessage());
      log.trace("Stack trace:", e);
      return List.of("Fehler beim Validieren: " + e.getMessage());
    }
  }

  private void runTest(
      Path file,
      TestRunner runner,
      List<String> errors,
      List<Map<String, String>> classes,
      String title) {
    var params = new TestParameters();
    params.setParameters(Map.of("file", file.toString(), "conformanceClasses", classes));
    var id = runner.runTest(params);
    var result = runner.getTestResult(id);
    runner.deleteTestResult(id);
    errors.add(title);
    extractErrors(result, errors);
  }

  @PreAuthorize("isAuthenticated()")
  public List<String> validateMetadata(String metadataId) throws XMLStreamException, IOException {
    var metadataCollection = metadataCollectionRepository.findByMetadataId(metadataId);
    if (metadataCollection.isEmpty()) {
      log.info("Metadata with ID {} is not available.", metadataId);
      return null;
    }
    var isoMetadata = metadataCollection.get().getIsoMetadata();
    var inspire = !isoMetadata.getMetadataProfile().equals(MetadataProfile.ISO);
    var files = generator.generateMetadata(metadataId);
    var errors = new ArrayList<String>();
    files.forEach(f -> errors.addAll(validate(inspire, f, files.getFirst() == f)));
    FileUtils.deleteDirectory(files.getFirst().getParent().toFile());
    return errors;
  }

  @PreAuthorize("isAuthenticated()")
  public TrackedTask createValidateMetadataTask(String metadataId) {
    SecurityContext context = SecurityContextHolder.getContext();

    return new TrackedTask(
        metadataId,
        () -> {
          SecurityContextHolder.setContext(context);
          try {
            eventPublisher.publishEvent(
                new ValidationMessage(metadataId, "Validation started", ValidationStatus.RUNNING));

            List<String> result = this.validateMetadata(metadataId);

            eventPublisher.publishEvent(
                new ValidationMessage(
                    metadataId, String.join("|", result), ValidationStatus.FINISHED));
          } catch (IOException | XMLStreamException | RuntimeException e) {
            log.error(
                "Error while validating metadata with id {}: \n {}", metadataId, e.getMessage());
            log.trace("Full stack trace: ", e);

            eventPublisher.publishEvent(
                new ValidationMessage(
                    metadataId, "Error while validation", ValidationStatus.FAILED));
          } finally {
            SecurityContextHolder.clearContext();
          }
        });
  }
}
