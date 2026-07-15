package de.terrestris.mde.mde_backend.event.usercleanup;

import de.terrestris.mde.mde_backend.service.UserCleanupService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class UserCleanupEventListener implements ApplicationListener<UserCleanupEvent> {

  @Autowired private UserCleanupService userCleanupService;

  @Override
  public void onApplicationEvent(UserCleanupEvent event) {
    try {
      log.trace("Received a user cleanup event, starting cleanup task in background");

      userCleanupService.runUserCleanup();

      log.trace("Successfully started the cleanup task.");
    } catch (Exception e) {
      log.error("Error while starting the user cleanup task: {}", e.getMessage());
      log.trace("Full stack trace: ", e);
    }
  }
}
