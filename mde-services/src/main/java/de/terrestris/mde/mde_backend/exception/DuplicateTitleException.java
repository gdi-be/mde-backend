package de.terrestris.mde.mde_backend.exception;

public class DuplicateTitleException extends RuntimeException {
  public DuplicateTitleException(String title) {
    super("A metadata entry with the title '" + title + "' already exists.");
  }
}
