package de.terrestris.mde.mde_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.dto.UserDetails;
import de.terrestris.mde.mde_backend.thread.TrackingExecutorService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCleanupServiceTest {

  @Mock private MetadataCollectionRepository metadataCollectionRepository;

  @Mock private TrackingExecutorService trackingExecutorService;

  @InjectMocks private UserCleanupService userCleanupService;

  private List<UserDetails> validUsers;

  @BeforeEach
  void setUp() {
    UserDetails user1 = new UserDetails();
    user1.setId("valid-user-1");
    user1.setFirstName("Alice");

    UserDetails user2 = new UserDetails();
    user2.setId("valid-user-2");
    user2.setFirstName("Bob");

    validUsers = List.of(user1, user2);
  }

  @Test
  void testRemoveUserNoChanges() {
    MetadataCollection mc =
        createMetadataCollection(
            "valid-user-1", "valid-user-2", Set.of("valid-user-1", "valid-user-2"));

    Optional<UserCleanupService.CollectionCleanupRecord> result = invokeRemoveUser(mc, validUsers);

    assertTrue(result.isEmpty(), "No changes should be made when all users are valid");
    verify(metadataCollectionRepository, never()).save(any());
  }

  @Test
  void testRemoveUserOwnerRemoved() {
    MetadataCollection mc =
        createMetadataCollection("stale-owner", "valid-user-2", Set.of("valid-user-1"));

    Optional<UserCleanupService.CollectionCleanupRecord> result = invokeRemoveUser(mc, validUsers);

    assertTrue(result.isPresent());
    UserCleanupService.CollectionCleanupRecord record = result.get();
    assertEquals("stale-owner", record.removedOwnerId());
    assertNull(record.removedAssignedUserId());
    assertTrue(record.removedTeamMemberIds().isEmpty());
    assertNull(mc.getOwnerId());
    verify(metadataCollectionRepository).save(mc);
  }

  @Test
  void testRemoveUserAssignedUserRemoved() {
    MetadataCollection mc =
        createMetadataCollection("valid-user-1", "stale-assigned", Set.of("valid-user-1"));

    Optional<UserCleanupService.CollectionCleanupRecord> result = invokeRemoveUser(mc, validUsers);

    assertTrue(result.isPresent());
    UserCleanupService.CollectionCleanupRecord record = result.get();
    assertNull(record.removedOwnerId());
    assertEquals("stale-assigned", record.removedAssignedUserId());
    assertTrue(record.removedTeamMemberIds().isEmpty());
    assertNull(mc.getAssignedUserId());
    verify(metadataCollectionRepository).save(mc);
  }

  @Test
  void testRemoveUserTeamMembersRemoved() {
    MetadataCollection mc =
        createMetadataCollection(
            "valid-user-1",
            "valid-user-2",
            Set.of("valid-user-1", "stale-member-1", "stale-member-2"));

    Optional<UserCleanupService.CollectionCleanupRecord> result = invokeRemoveUser(mc, validUsers);

    assertTrue(result.isPresent());
    UserCleanupService.CollectionCleanupRecord record = result.get();
    assertNull(record.removedOwnerId());
    assertNull(record.removedAssignedUserId());
    assertEquals(2, record.removedTeamMemberIds().size());
    assertTrue(
        record.removedTeamMemberIds().containsAll(List.of("stale-member-1", "stale-member-2")));
    assertEquals(1, mc.getTeamMemberIds().size());
    assertTrue(mc.getTeamMemberIds().contains("valid-user-1"));
    verify(metadataCollectionRepository).save(mc);
  }

  @Test
  void testRemoveUserMultipleChanges() {
    MetadataCollection mc =
        createMetadataCollection(
            "stale-owner",
            "stale-assigned",
            Set.of("stale-team-1", "valid-user-1", "stale-team-2"));

    Optional<UserCleanupService.CollectionCleanupRecord> result = invokeRemoveUser(mc, validUsers);

    assertTrue(result.isPresent());
    UserCleanupService.CollectionCleanupRecord record = result.get();
    assertEquals("stale-owner", record.removedOwnerId());
    assertEquals("stale-assigned", record.removedAssignedUserId());
    assertEquals(2, record.removedTeamMemberIds().size());
    assertTrue(record.hasChanges());
    verify(metadataCollectionRepository).save(mc);
  }

  @Test
  void testRemoveUserNullTeamMembers() {
    MetadataCollection mc = new MetadataCollection();
    mc.setOwnerId("valid-user-1");
    mc.setAssignedUserId("valid-user-2");
    mc.setTeamMemberIds(null);

    Optional<UserCleanupService.CollectionCleanupRecord> result = invokeRemoveUser(mc, validUsers);

    assertTrue(result.isEmpty());
    verify(metadataCollectionRepository, never()).save(any());
  }

  @Test
  void testRunUserCleanupSubmitsTask() {
    userCleanupService.runUserCleanup();

    verify(trackingExecutorService).submit(any(Runnable.class));
  }

  private MetadataCollection createMetadataCollection(
      String ownerId, String assignedUserId, Set<String> teamMemberIds) {
    MetadataCollection mc = new MetadataCollection();
    mc.setOwnerId(ownerId);
    mc.setAssignedUserId(assignedUserId);
    mc.setTeamMemberIds(new HashSet<>(teamMemberIds));
    return mc;
  }

  private Optional<UserCleanupService.CollectionCleanupRecord> invokeRemoveUser(
      MetadataCollection mc, List<UserDetails> users) {
    try {
      var method =
          UserCleanupService.class.getDeclaredMethod(
              "removeUser", MetadataCollection.class, List.class);
      method.setAccessible(true);
      @SuppressWarnings("unchecked")
      Optional<UserCleanupService.CollectionCleanupRecord> result =
          (Optional<UserCleanupService.CollectionCleanupRecord>)
              method.invoke(userCleanupService, mc, users);
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke removeUser method", e);
    }
  }
}
