package de.terrestris.mde.mde_backend.schedule;

import de.terrestris.mde.mde_backend.event.usercleanup.UserCleanupEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("${usercleanup.enabled:false}")
public class UserCleanupScheduler {

  @Autowired private final UserCleanupEventPublisher userCleanupEventPublisher;

  // Default: every weekday at 0 am
  @Scheduled(cron = "${usercleanup.cleanup-cron:0 0 0 ? * MON-FRI}")
  public void publishUserCleanup() {
    userCleanupEventPublisher.publish();
  }
}
