package de.terrestris.mde.mde_backend.event.usercleanup;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserCleanupEvent extends ApplicationEvent {
  public UserCleanupEvent(Object source) {
    super(source);
  }
}
