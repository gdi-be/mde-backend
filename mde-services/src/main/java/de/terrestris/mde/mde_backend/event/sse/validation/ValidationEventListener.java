package de.terrestris.mde.mde_backend.event.sse.validation;

import de.terrestris.mde.mde_backend.service.SseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ValidationEventListener {

  @Autowired
  private SseService sseService;

  @EventListener
  public void onValidationEvent(ValidationEvent event) {
    sseService.send("validation", event.getKeycloakId(), event.getMessage());
  }

}
