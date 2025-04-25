package de.terrestris.mde.mde_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.terrestris.bkgtestsuite.core.model.TestParameters;
import de.terrestris.bkgtestsuite.core.model.TestResult;
import de.terrestris.bkgtestsuite.core.spi.TestRunner;
import de.terrestris.bkgtestsuite.etf.EtfTestRunner;
import de.terrestris.bkgtestsuite.te.TeTestRunner;
import de.terrestris.mde.mde_backend.enumeration.MetadataProfile;
import de.terrestris.mde.mde_backend.enumeration.ValidationStatus;
import de.terrestris.mde.mde_backend.event.sse.validation.ValidationEventPublisher;
import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.dto.sse.ValidationMessage;
import de.terrestris.mde.mde_backend.thread.TrackedTask;
import jakarta.annotation.PostConstruct;
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

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.terrestris.bkgtestsuite.core.model.TestResult.MessageType.TEXT;
import static de.terrestris.bkgtestsuite.core.model.TestResult.ResultStatus.*;

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

  @Autowired
  private MetadataCollectionRepository metadataCollectionRepository;

  @Autowired
  private IsoGenerator generator;

  @Autowired
  private ValidationEventPublisher eventPublisher;

  private TestRunner isoRunner;

  private TestRunner inspireRunner;

  private TestRunner etfServiceRunner;

  private TestRunner etfDatasetRunner;

  private TestRunner etfNetworkServicesRunner;

  private final List<Map<String, String>> inspireClasses = List.of(
    Map.of("id", "ISO-Schemavalidierung"),
      Map.of("id", "GDI-DE_INSPIRE_verpflichtend"),
      Map.of("id", "GDI-DE_INSPIRE_konditional"),
      Map.of("id", "GDI-DE_konditional"),
      Map.of("id", "GDI-DE_optional"),
      Map.of("id", "OpenData_konditional")
  );

  private final List<Map<String, String>> isoClasses = List.of(
    Map.of("id", "ISO-Schemavalidierung"),
    Map.of("id", "GDI-DE_verpflichtend"),
    Map.of("id", "GDI-DE_konditional"),
    Map.of("id", "GDI-DE_optional"),
    Map.of("id", "OpenData_konditional")
  );

  private final List<Map<String, String>> datasetClasses = List.of(
    Map.of("id", "EID59692c11-df86-49ad-be7f-94a1e1ddd8da"),
    Map.of("id", "EIDe4a95862-9cc9-436b-9fdd-a0115d342350"),
    Map.of("id", "EID2be1480a-fe42-40b2-9420-eb0e69385c80"),
    Map.of("id", "EID0b86f7a3-2947-4841-823d-6a00d8e06d70"),
    Map.of("id", "EID1067d6b2-3bb1-4e71-8ce1-a744c9bd5a3b"),
    Map.of("id", "EID7cceba68-e575-4429-9959-1b6b3d201b6d")
  );

  private final List<Map<String, String>> serviceClasses = List.of(
    Map.of("id", "EID59692c11-df86-49ad-be7f-94a1e1ddd8da"),
    Map.of("id", "EID8f869e23-c9e9-4e86-8dca-be30ff421229"),
    Map.of("id", "EID8db54d8a-8578-4959-b891-5394d9f53a28"),
    Map.of("id", "EID7514777a-6cb8-499c-acd5-912496dc84e9"),
    Map.of("id", "EIDa593a7ad-42d9-46d0-985d-9dff3e684428")
  );

  private final List<Map<String, String>> networkServiceClasses = List.of(
    Map.of("id", "EID59692c11-df86-49ad-be7f-94a1e1ddd8da"),
    Map.of("id", "EID8f869e23-c9e9-4e86-8dca-be30ff421229"),
    Map.of("id", "EID606587df-65a8-4b7b-9eee-e0d94daaa42a")
  );

  @PostConstruct
  public void loadIsoPackage() throws IOException {
    var excluded = List.of("config.json", "main.ctl", "inspire_only.ctl");
    var list = extractResources(metadataResources, file -> file.toString().contains("src/scripts"));
    list = list.stream().filter(f -> !excluded.contains(f.getName())).collect(Collectors.toList());
    isoRunner = new TeTestRunner(isoConfig.getInputStream(), list, new File("/tmp/bkg-scripts-metadata/src/resources"));
  }

  @PostConstruct
  public void loadInspirePackage() throws IOException {
    var excluded = List.of("config.json", "main2.ctl", "iso_only.ctl");
    var list = extractResources(metadataResources, file -> file.toString().contains("src/scripts"));
    list = list.stream().filter(f -> !excluded.contains(f.getName())).collect(Collectors.toList());
    inspireRunner = new TeTestRunner(inspireConfig.getInputStream(), list, new File("/tmp/bkg-scripts-metadata/src/resources"));
  }

  @PostConstruct
  public void loadEtfServicePackage() throws IOException, NoSuchAlgorithmException {
    var node = MAPPER.readTree(getClass().getResourceAsStream("/config-services.json"));
    etfServiceRunner = new EtfTestRunner(node, false);
  }

  @PostConstruct
  public void loadEtfDatasetPackage() throws IOException, NoSuchAlgorithmException {
    var node = MAPPER.readTree(getClass().getResourceAsStream("/config-dataset.json"));
    etfDatasetRunner = new EtfTestRunner(node, false);
  }

  @PostConstruct
  public void loadEtfNetworkServicesPackage() throws IOException, NoSuchAlgorithmException {
    var node = MAPPER.readTree(getClass().getResourceAsStream("/config-networkservices.json"));
    etfNetworkServicesRunner = new EtfTestRunner(node, false);
  }

  private static List<File> extractResources(List<Resource> resources, Predicate<File> tester) throws IOException {
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
    if (result.getStatus().equals(Failure) || result.getStatus().equals(TechnicalFailure) || result.getStatus().equals(InheritedFailure)) {
      errors.addAll(result.getMessages().stream().filter(m -> m.getType().equals(TEXT)).map(TestResult.Message::getText).toList());
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
      runTest(file, runner, errors, classes, inspire ? "Ergebnisse GDI-DE für INSPIRE:" : "Ergebnisse GDI-DE:");
      if (inspire) {
        if (dataset) {
          log.debug("Running INSPIRE dataset tests...");
          runTest(file, etfDatasetRunner, errors, datasetClasses, "Ergebnisse INSPIRE-Validator:");
        } else {
          log.debug("Running INSPIRE service tests...");
          runTest(file, etfServiceRunner, errors, serviceClasses, "Ergebnisse INSPIRE-Validator:");
          log.debug("Running INSPIRE network services tests...");
          runTest(file, etfNetworkServicesRunner, errors, networkServiceClasses, "Ergebnisse INSPIRE-Validator:");
        }
      }
      return errors;
    } catch (Exception e) {
      log.error("Error when validating: {}", e.getMessage());
      log.trace("Stack trace:", e);
      return List.of("Fehler beim Validieren: " + e.getMessage());
    }
  }

  private void runTest(Path file, TestRunner runner, List<String> errors, List<Map<String, String>> classes, String title) {
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

    return new TrackedTask(metadataId, () -> {
      SecurityContextHolder.setContext(context);
      try {
        eventPublisher.publishEvent(new ValidationMessage(metadataId,
          "Validation started", ValidationStatus.RUNNING));

        List<String> result = this.validateMetadata(metadataId);

        eventPublisher.publishEvent(new ValidationMessage(metadataId,
          String.join("|", result), ValidationStatus.FINISHED));
      } catch (IOException | XMLStreamException | RuntimeException e) {
        log.error("Error while validating metadata with id {}: \n {}", metadataId, e.getMessage());
        log.trace("Full stack trace: ", e);

        eventPublisher.publishEvent(new ValidationMessage(metadataId,
          "Error while validation", ValidationStatus.FAILED));
      } finally {
        SecurityContextHolder.clearContext();
      }
    });
  }
}
