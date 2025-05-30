package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.terrestris.mde.mde_backend.enumeration.Role;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryConfig {

  private String searchTerm;

  private Boolean isAssignedToMe;

  private Boolean isTeamMember;

  private Boolean isApproved;

  private List<Role> assignedRoles;
}
