package de.terrestris.mde.mde_backend.model.dto.sse;

import de.terrestris.mde.mde_backend.enumeration.ValidationStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ValidationMessage extends SseMessage {

  private String metadataId;

  private ValidationStatus status;

  public ValidationMessage(String metadataId, String message, ValidationStatus status) {
    super(message);

    this.status = status;
    this.metadataId = metadataId;
  }
}
