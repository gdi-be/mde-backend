package de.terrestris.mde.mde_backend.enumeration;

/**
 * KeyCloak realm roles.
 */
public enum Role {
    /**
     * Administrator role. (Administrator)
     */
    MdeAdministrator("MdeAdministrator"),
    /**
     * DataOwner role. (Datenhaltende Stelle)
     */
    MdeDataOwner("MdeDataOwner"),
    /**
     * Editor role. (Redakteur)
     */
    MdeEditor("MdeEditor"),
    /**
     * QualityAssurance role. (QS)
     */
    MdeQualityAssurance("MdeQualityAssurance");

    private final String type;

    Role(String type) {
        this.type = type;
    }
}
