package de.terrestris.mde.mde_backend.security;

import de.terrestris.mde.mde_backend.enumeration.Role;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Log4j2
@Component
public class MdePermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        List<Role> allowedRoles = List.of(Role.Administrator, Role.Editor, Role.QualityAssurance, Role.DataOwner);
        return allowedRoles.stream()
                .anyMatch(role -> authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role.toString()))
                );
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }


}