package de.terrestris.mde.mde_backend.event.usercleanup;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class UserCleanupEventPublisher {

  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  public void publish() {
    try {
      log.trace("Publishing a user cleanup event.");

      applicationEventPublisher.publishEvent(new UserCleanupEvent(this));

      log.trace("Successfully published a user cleanup event.");
    } catch (Exception e) {
      log.error("Error while publishing a user cleanup event: {}", e.getMessage());
      log.trace("Full stack trace: ", e);
    }
  }
}
