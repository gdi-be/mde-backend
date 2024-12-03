package de.terrestris.mde.mde_backend.enumeration;

/**
 * KeyCloak realm roles.
 */
public enum Role {
    /**
     * Administrator role. (Administrator)
     */
    Administrator("Administrator"),
    /**
     * DataOwner role. (Datenhaltende Stelle)
     */
    DataOwner("DataOwner"),
    /**
     * Editor role. (Redakteur)
     */
    Editor("Editor"),
    /**
     * QualityAssurance role. (QS)
     */
    QualityAssurance("QualityAssurance");

    private final String type;

    Role(String type) {
        this.type = type;
    }
}