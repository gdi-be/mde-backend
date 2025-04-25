package de.terrestris.mde.mde_backend.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Component
public class KeycloakProperties {

  private String serverUrl;

  private String clientSecret;

  private String realm;

  private String clientId;
}
