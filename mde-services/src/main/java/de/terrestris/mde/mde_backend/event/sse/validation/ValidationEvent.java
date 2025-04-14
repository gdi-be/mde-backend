package de.terrestris.mde.mde_backend.event.sse.validation;

import de.terrestris.mde.mde_backend.event.sse.SseEvent;
import de.terrestris.mde.mde_backend.model.dto.sse.SseMessage;
import lombok.Getter;

@Getter
public class ValidationEvent extends SseEvent {
  private final String keycloakId;

  public ValidationEvent(Object source, SseMessage message, String keycloakId) {
    super(source, message);

    this.keycloakId = keycloakId;
  }
}
