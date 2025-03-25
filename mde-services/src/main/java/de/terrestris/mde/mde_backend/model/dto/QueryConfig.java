package de.terrestris.mde.mde_backend.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.terrestris.mde.mde_backend.enumeration.Role;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryConfig {

    private String searchTerm;

    private Boolean isAssignedToMe;

    private Boolean isTeamMember;

    private Boolean isValid;

    private List<Role> assignedRoles;

}
