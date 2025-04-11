package de.terrestris.mde.mde_backend.event.sse;

import de.terrestris.mde.mde_backend.model.dto.sse.SseMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class SseEvent extends ApplicationEvent {
  private final SseMessage message;

  public SseEvent(Object source, SseMessage message) {
    super(source);
    this.message = message;
  }
}
