package de.terrestris.mde.mde_backend.specification;

import de.terrestris.mde.mde_backend.model.MetadataCollection;
import de.terrestris.mde.mde_backend.model.dto.QueryConfig;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MetadataCollectionSpecification {

  public static Specification<MetadataCollection> searchMetadata(QueryConfig config, String myKeycloakId) {
    return (Root<MetadataCollection> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

      List<Predicate> predicates = new ArrayList<>();

      // Text-Filter
      if (config.getSearchTerm() != null && !config.getSearchTerm().isEmpty()) {
        predicates.add(cb.like(
          cb.lower(root.get("title")),
          "%" + config.getSearchTerm().toLowerCase() + "%"
        ));
      }

      // Valid-Filter
      if (config.getIsValid() != null) {
        predicates.add(cb.equal(root.get("valid"), config.getIsValid()));
      }

      // Assigned-Filter
      if (config.getIsAssignedToMe() != null) {
        if (config.getIsAssignedToMe()) {
          predicates.add(cb.equal(root.get("assignedUserId"), myKeycloakId));
        } else {
          predicates.add(cb.notEqual(root.get("assignedUserId"), myKeycloakId));
        }
      }

      // Team-Member-Filter (reused in sortPriority)
      Expression<Boolean> isTeamMember = cb.isNotNull(cb.function(
        "array_position",
        Integer.class,
        root.get("teamMemberIds"),
        cb.literal(myKeycloakId)
      ));
      if (config.getIsTeamMember() != null) {
        if (config.getIsTeamMember()) {
          predicates.add(cb.isTrue(isTeamMember));
        } else {
          predicates.add(cb.isFalse(isTeamMember));
        }
      }

      // Role-Filter
      if (config.getAssignedRoles() != null && !config.getAssignedRoles().isEmpty()) {
        predicates.add(root.get("responsibleRole").in(config.getAssignedRoles()));
      }

      Expression<Object> sortPriority = cb.selectCase()
        .when(cb.equal(root.get("assignedUserId"), myKeycloakId), 0)
        .when(cb.isTrue(isTeamMember), 1)
        .otherwise(2);

      query.orderBy(
        // first sort by priority (assigned to me, team member, not assigned)
        cb.asc(sortPriority),
        // sub sort by modified date if assigned to me
        cb.desc(cb.selectCase()
          .when(cb.equal(root.get("assignedUserId"), myKeycloakId), root.get("modified"))
          .otherwise(cb.nullLiteral(Instant.class))
        ),
        // ... otherwise sort by title
        cb.asc(root.get("title"))
      );

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
