package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.jpa.MetadataCollectionRepository;
import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.dto.UserDetails;
import de.terrestris.mde.mde_backend.thread.TrackedTask;
import de.terrestris.mde.mde_backend.thread.TrackingExecutorService;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;

/**
 * Service responsible for cleaning up stale Keycloak user references from {@link
 * MetadataCollection} entities.
 *
 * <p>On each run it fetches the current list of users from Keycloak and iterates over all {@link
 * MetadataCollection} records in batches of {@link #COLLECTIONS_BATCH_SIZE}. Any reference to a
 * user id that no longer exists in Keycloak (owner, assigned user, or team member) is removed from
 * the collection and persisted. A structured summary is written to the log at the end of each run.
 *
 * <p>The cleanup is submitted to the {@link TrackingExecutorService} thread pool so it runs
 * asynchronously and does not block the calling thread.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class UserCleanupService {

  public static final Integer COLLECTIONS_BATCH_SIZE = 20;

  @Autowired private KeycloakService keycloakService;

  @Autowired private TrackingExecutorService trackingExecutorService;

  @Autowired private MetadataCollectionRepository metadataCollectionRepository;

  /**
   * Holds all user-reference changes that were applied to a single {@link MetadataCollection}
   * during one cleanup run.
   *
   * @param collectionId the database id of the affected {@link MetadataCollection}
   * @param metadataUuid the metadata uuid of the affected {@link MetadataCollection}
   * @param removedOwnerId the owner id that was removed, or {@code null} if the owner was still
   *     present in Keycloak
   * @param removedAssignedUserId the assigned-user id that was removed, or {@code null} if the
   *     assigned user was still present in Keycloak
   * @param removedTeamMemberIds list of team-member ids that were removed; empty if no team members
   *     had to be removed
   */
  record CollectionCleanupRecord(
      BigInteger collectionId,
      String metadataUuid,
      String removedOwnerId,
      String removedAssignedUserId,
      List<String> removedTeamMemberIds) {

    /**
     * Returns {@code true} if at least one user reference was removed from the collection.
     *
     * @return {@code true} when the record represents actual changes, {@code false} otherwise
     */
    boolean hasChanges() {
      return removedOwnerId != null
          || removedAssignedUserId != null
          || !removedTeamMemberIds.isEmpty();
    }
  }

  /**
   * Submits a user-cleanup task to the background thread pool.
   *
   * <p>The task:
   *
   * <ol>
   *   <li>Creates a synthetic Spring {@link SecurityContext} with {@code ROLE_ADMIN} so that
   *       secured service methods can be called without an active HTTP request.
   *   <li>Retrieves the full list of users currently registered in Keycloak.
   *   <li>Iterates over all {@link MetadataCollection} records in pages of {@link
   *       #COLLECTIONS_BATCH_SIZE} and removes any user id that is no longer present in Keycloak.
   *   <li>Writes a structured summary to the log via {@link #logSummary}.
   * </ol>
   *
   * <p>Errors during execution are caught and logged; the security context is always cleared in a
   * {@code finally} block to prevent context leaks.
   */
  public void runUserCleanup() {
    // Submit cleanup task to thread pool to run in background
    TrackedTask cleanupTask =
        new TrackedTask(
            "user-cleanup-" + System.currentTimeMillis(),
            () -> {
              try {
                // Create a synthetic security context with ADMIN role for system-level operations.
                SecurityContext context = new SecurityContextImpl();
                UsernamePasswordAuthenticationToken adminAuth =
                    new UsernamePasswordAuthenticationToken(
                        "system", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                context.setAuthentication(adminAuth);
                SecurityContextHolder.setContext(context);

                List<UserDetails> keycloakUsers = keycloakService.getAllUsers();
                log.trace("Retrieved {} users from Keycloak", keycloakUsers.size());

                if (keycloakUsers.isEmpty()) {
                  throw new Exception("No users found in Keycloak.");
                }

                List<CollectionCleanupRecord> changedRecords = new ArrayList<>();

                Page<MetadataCollection> collectionsPage =
                    metadataCollectionRepository.findAll(PageRequest.of(0, COLLECTIONS_BATCH_SIZE));
                long totalCollections = collectionsPage.getTotalElements();

                log.trace(
                    "Retrieved {} metadata collections – removing all Keycloak user ids "
                        + "that are no longer present",
                    totalCollections);

                collectionsPage
                    .getContent()
                    .forEach(mc -> removeUser(mc, keycloakUsers).ifPresent(changedRecords::add));

                while (collectionsPage.hasNext()) {
                  collectionsPage =
                      metadataCollectionRepository.findAll(collectionsPage.nextPageable());
                  collectionsPage
                      .getContent()
                      .forEach(mc -> removeUser(mc, keycloakUsers).ifPresent(changedRecords::add));
                }

                logSummary(totalCollections, changedRecords);
              } catch (Exception e) {
                log.error("Error during user cleanup: {}", e.getMessage());
                log.trace("Full stack trace:", e);
              } finally {
                SecurityContextHolder.clearContext();
              }
            });

    trackingExecutorService.submit(cleanupTask);

    log.trace("User cleanup task submitted to thread pool");
  }

  /**
   * Writes a human-readable cleanup summary to the log at {@code INFO} level.
   *
   * <p>The summary contains:
   *
   * <ul>
   *   <li>Total number of metadata collections that were checked.
   *   <li>Number of collections that were actually modified.
   *   <li>Per-collection breakdown of which user ids were removed (owner, assigned user, team
   *       members).
   * </ul>
   *
   * @param totalChecked total number of {@link MetadataCollection} records that were inspected
   * @param changed list of {@link CollectionCleanupRecord} entries for every collection that had at
   *     least one user reference removed
   */
  private void logSummary(long totalChecked, List<CollectionCleanupRecord> changed) {
    log.info("=== User Cleanup Summary ===");
    log.info("Metadata collections checked : {}", totalChecked);
    log.info("Metadata collections modified: {}", changed.size());

    if (changed.isEmpty()) {
      log.info("No collections were modified.");
    } else {
      log.info("Modified collections:");
      for (CollectionCleanupRecord record : changed) {
        StringBuilder sb = new StringBuilder();
        sb.append("  MetadataCollection ID ")
            .append(record.collectionId())
            .append(" (")
            .append(record.metadataUuid())
            .append(")")
            .append(":");
        if (record.removedOwnerId() != null) {
          sb.append("\n    - owner removed          : ").append(record.removedOwnerId());
        }
        if (record.removedAssignedUserId() != null) {
          sb.append("\n    - assigned user removed  : ").append(record.removedAssignedUserId());
        }
        if (!record.removedTeamMemberIds().isEmpty()) {
          sb.append("\n    - team members removed   : ").append(record.removedTeamMemberIds());
        }
        log.info(sb.toString());
      }
    }
    log.info("============================");
  }

  /**
   * Checks a single {@link MetadataCollection} for stale Keycloak user references and removes any
   * id that is no longer present in the provided list of active Keycloak users.
   *
   * <p>The following fields are checked:
   *
   * <ul>
   *   <li>{@code ownerId} – set to {@code null} if the owner no longer exists.
   *   <li>{@code assignedUserId} – set to {@code null} if the assigned user no longer exists.
   *   <li>{@code teamMemberIds} – all ids of former Keycloak users are removed from the set.
   * </ul>
   *
   * <p>If any change was made the collection is persisted immediately and a {@link
   * CollectionCleanupRecord} capturing the removed ids is returned. If no ids were stale, {@link
   * Optional#empty()} is returned and no database write occurs.
   *
   * @param metadataCollection the {@link MetadataCollection} to inspect and potentially update
   * @param keycloakUsers the current list of all users registered in Keycloak
   * @return an {@link Optional} containing a {@link CollectionCleanupRecord} with the removed ids
   *     if any changes were made, or {@link Optional#empty()} if the collection was clean
   */
  private Optional<CollectionCleanupRecord> removeUser(
      MetadataCollection metadataCollection, List<UserDetails> keycloakUsers) {

    log.trace(
        "Checking MetadataCollection ID {} ({})",
        metadataCollection.getId(),
        metadataCollection.getMetadataId());

    String ownerId = metadataCollection.getOwnerId();
    String assignedUserId = metadataCollection.getAssignedUserId();
    Set<String> teamMemberIds = metadataCollection.getTeamMemberIds();

    String removedOwnerId = null;
    String removedAssignedUserId = null;
    List<String> removedTeamMemberIds = new ArrayList<>();

    // Check if ownerId exists in Keycloak
    if (ownerId != null && keycloakUsers.stream().noneMatch(u -> u.getId().equals(ownerId))) {
      removedOwnerId = ownerId;
      metadataCollection.setOwnerId(null);
    }

    // Check if assignedUserId exists in Keycloak
    if (assignedUserId != null
        && keycloakUsers.stream().noneMatch(u -> u.getId().equals(assignedUserId))) {
      removedAssignedUserId = assignedUserId;
      metadataCollection.setAssignedUserId(null);
    }

    // Check teamMemberIds
    if (teamMemberIds != null) {
      teamMemberIds.forEach(
          teamMemberId -> {
            if (keycloakUsers.stream().noneMatch(u -> u.getId().equals(teamMemberId))) {
              removedTeamMemberIds.add(teamMemberId);
            }
          });
      teamMemberIds.removeIf(removedTeamMemberIds::contains);
    }

    CollectionCleanupRecord record =
        new CollectionCleanupRecord(
            metadataCollection.getId(),
            metadataCollection.getMetadataId(),
            removedOwnerId,
            removedAssignedUserId,
            removedTeamMemberIds);

    if (record.hasChanges()) {
      metadataCollectionRepository.save(metadataCollection);
      return Optional.of(record);
    }

    return Optional.empty();
  }
}
