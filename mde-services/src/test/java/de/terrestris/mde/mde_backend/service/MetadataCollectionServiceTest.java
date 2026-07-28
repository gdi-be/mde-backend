package de.terrestris.mde.mde_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.json.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MetadataCollectionServiceTest {

  @Mock private MetadataCollectionRepository repository;

  @Mock private JwtAuthenticationToken authentication;

  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private MetadataCollectionService metadataCollectionService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(metadataCollectionService, "repository", repository);
    when(authentication.getTokenAttributes()).thenReturn(Map.of("sub", "user-id"));
    when(authentication.getAuthorities()).thenReturn(List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void testCloneClearsTemplateSpecificMetadata() throws Exception {
    String sourceId = "source-id";
    MetadataCollection source = new MetadataCollection(sourceId);

    Extent initialExtent = new Extent("EPSG:25833", 1.0, 2.0, 3.0, 4.0);
    source.getClientMetadata().setInitialExtent(initialExtent);
    source.getClientMetadata().setLayers(Map.of("atom-service", List.of(new Layer())));
    source.getClientMetadata().setRelatedTopics("related topics");
    source.getClientMetadata().setComments(List.of(new Comment()));

    DatabaseInfo databaseInfo =
        new DatabaseInfo("driver", "jdbc:postgresql://database", List.of("table"));
    List<Category> categories = List.of(new Category("title", "type", "url"));
    List<String> descriptions = List.of("description");
    source.getTechnicalMetadata().setDatabaseInfo(databaseInfo);
    source.getTechnicalMetadata().setCategories(categories);
    source.getTechnicalMetadata().setDescriptions(descriptions);
    source.getTechnicalMetadata().setDeliveredCrs("25833");
    source
        .getTechnicalMetadata()
        .setLayerInfos(List.of(new LayerInfo("atom-download", "workspace")));

    AtomicReference<MetadataCollection> cloneReference = new AtomicReference<>();
    when(repository.findByMetadataId(anyString()))
        .thenAnswer(
            invocation -> {
              String metadataId = invocation.getArgument(0);
              return sourceId.equals(metadataId)
                  ? Optional.of(source)
                  : Optional.ofNullable(cloneReference.get());
            });
    when(repository.save(any(MetadataCollection.class)))
        .thenAnswer(
            invocation -> {
              MetadataCollection metadataCollection = invocation.getArgument(0);
              cloneReference.set(metadataCollection);
              return metadataCollection;
            });

    metadataCollectionService.clone("Cloned title", sourceId);

    MetadataCollection clone = cloneReference.get();
    assertNull(clone.getClientMetadata().getLayers());
    assertNull(clone.getClientMetadata().getRelatedTopics());
    assertNull(clone.getClientMetadata().getComments());
    assertNull(clone.getTechnicalMetadata().getLayerInfos());
    assertNull(clone.getTechnicalMetadata().getDeliveredCrs());
    assertNull(clone.getTechnicalMetadata().getCategories());
    assertEquals(initialExtent, clone.getClientMetadata().getInitialExtent());
    assertEquals(databaseInfo, clone.getTechnicalMetadata().getDatabaseInfo());
    assertEquals(descriptions, clone.getTechnicalMetadata().getDescriptions());
  }
}
