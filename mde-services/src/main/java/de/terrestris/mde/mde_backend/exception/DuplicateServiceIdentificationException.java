package de.terrestris.mde.mde_backend.exception;

public class DuplicateServiceIdentificationException extends RuntimeException {
  public DuplicateServiceIdentificationException(String serviceIdentification) {
    super(
        "A service with the serviceIdentification '" + serviceIdentification + "' already exists.");
  }
}
