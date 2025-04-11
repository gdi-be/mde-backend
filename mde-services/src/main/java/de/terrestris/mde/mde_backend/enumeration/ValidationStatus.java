package de.terrestris.mde.mde_backend.enumeration;

public enum ValidationStatus {
  PENDING("PENDING"),
  RUNNING("RUNNING"),
  FINISHED("FINISHED"),
  FAILED("FAILED");

  private final String type;

  ValidationStatus(String type) {
    this.type = type;
  }
}
