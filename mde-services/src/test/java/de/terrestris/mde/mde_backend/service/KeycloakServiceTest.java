package de.terrestris.mde.mde_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.terrestris.mde.mde_backend.model.dto.UserDetails;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

  @Mock private RealmResource keycloakRealm;

  @Mock private UsersResource usersResource;

  @InjectMocks private KeycloakService keycloakService;

  @BeforeEach
  void setUp() {
    when(keycloakRealm.users()).thenReturn(usersResource);
  }

  @Test
  void testGetAllUsersEmpty() {
    when(usersResource.list(0, 100)).thenReturn(new ArrayList<>());

    List<UserDetails> result = keycloakService.getAllUsers();

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(usersResource).list(0, 100);
  }

  @Test
  void testGetAllUsersSinglePageLessThan100() {
    List<UserRepresentation> page1 = createUserRepresentations(50);
    when(usersResource.list(0, 100)).thenReturn(page1);

    List<UserDetails> result = keycloakService.getAllUsers();

    assertNotNull(result);
    assertEquals(50, result.size());
    verify(usersResource).list(0, 100);
    verify(usersResource, times(1)).list(anyInt(), anyInt());
  }

  @Test
  void testGetAllUsersExactly100() {
    List<UserRepresentation> page1 = createUserRepresentations(100);
    when(usersResource.list(0, 100)).thenReturn(page1);

    List<UserDetails> result = keycloakService.getAllUsers();

    assertNotNull(result);
    assertEquals(100, result.size());
    // Should detect full page and stop (no second call)
    verify(usersResource).list(0, 100);
  }

  @Test
  void testGetAllUsersMultiplePages() {
    List<UserRepresentation> page1 = createUserRepresentations(100);
    List<UserRepresentation> page2 = createUserRepresentations(50);

    when(usersResource.list(0, 100)).thenReturn(page1);
    when(usersResource.list(100, 100)).thenReturn(page2);

    List<UserDetails> result = keycloakService.getAllUsers();

    assertNotNull(result);
    assertEquals(150, result.size());
    verify(usersResource, times(2)).list(anyInt(), anyInt());
  }

  @Test
  void testGetAllUsersExactly200() {
    List<UserRepresentation> page1 = createUserRepresentations(100);
    List<UserRepresentation> page2 = createUserRepresentations(100);
    List<UserRepresentation> page3 = new ArrayList<>(); // empty page

    when(usersResource.list(0, 100)).thenReturn(page1);
    when(usersResource.list(100, 100)).thenReturn(page2);
    when(usersResource.list(200, 100)).thenReturn(page3);

    List<UserDetails> result = keycloakService.getAllUsers();

    assertNotNull(result);
    assertEquals(200, result.size());
    verify(usersResource).list(0, 100);
    verify(usersResource).list(100, 100);
    verify(usersResource).list(200, 100);
  }

  @Test
  void testGetAllUsersMapping() {
    UserRepresentation user1 = new UserRepresentation();
    user1.setId("user-123");
    user1.setFirstName("John");
    user1.setLastName("Doe");
    user1.setEmail("john@example.com");

    when(usersResource.list(0, 100)).thenReturn(List.of(user1));

    List<UserDetails> result = keycloakService.getAllUsers();

    assertEquals(1, result.size());
    UserDetails userDetails = result.getFirst();
    assertEquals("user-123", userDetails.getId());
    assertEquals("John", userDetails.getFirstName());
    assertEquals("Doe", userDetails.getLastName());
    assertEquals("john@example.com", userDetails.getEmail());
  }

  private List<UserRepresentation> createUserRepresentations(int count) {
    List<UserRepresentation> users = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      UserRepresentation user = new UserRepresentation();
      user.setId("user-" + i);
      user.setFirstName("First" + i);
      user.setLastName("Last" + i);
      user.setEmail("user" + i + "@example.com");
      users.add(user);
    }
    return users;
  }
}
