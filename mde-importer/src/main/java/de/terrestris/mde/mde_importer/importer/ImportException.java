package de.terrestris.mde.mde_importer.importer;

public class ImportException extends RuntimeException {

  public ImportException(Throwable cause) {
    super("Unable to import", cause);
  }
}
