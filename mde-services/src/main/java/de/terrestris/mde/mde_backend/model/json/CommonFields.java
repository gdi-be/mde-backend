package de.terrestris.mde.mde_backend.model.json;

import java.io.Serializable;

public interface CommonFields extends Serializable {

  void setFileIdentifier(String identifier);

  String getFileIdentifier();

  void setTechnicalDescription(String technicalDescription);

  String getTechnicalDescription();

  void setContentDescription(String contentDescription);

  String getContentDescription();
}
