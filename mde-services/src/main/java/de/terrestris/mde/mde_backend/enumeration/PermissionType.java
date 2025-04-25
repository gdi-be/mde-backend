package de.terrestris.mde.mde_backend.enumeration;

public enum PermissionType {
  ADMIN("ADMIN"),
  CREATE("CREATE"),
  DELETE("DELETE"),
  UPDATE("UPDATE"),
  READ("READ");

  private final String type;

  PermissionType(String type) {
    this.type = type;
  }
}
