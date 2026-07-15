package de.terrestris.mde.mde_backend.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@Configuration
@ConfigurationProperties(prefix = "usercleanup")
public class UserCleanupSchedulerProperties {
  private Boolean enabled;
  private String cleanupCron;
}
