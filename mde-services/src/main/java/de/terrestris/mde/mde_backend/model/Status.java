package de.terrestris.mde.mde_backend.model;

public enum Status {

  // dataset is not yet published
  NEW,
  // dataset is published, but being edited
  IN_EDIT,
  // dataset is published and not being edited
  PUBLISHED
}
