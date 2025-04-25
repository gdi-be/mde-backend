package de.terrestris.mde.mde_backend.enumeration;

public enum MetadataType {
  ISO("ISO"),
  TECHNICAL("TECHNICAL"),
  CLIENT("CLIENT");

  private final String type;

  MetadataType(String type) {
    this.type = type;
  }
}
