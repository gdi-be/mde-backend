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

  private Boolean enabled;

  private String serverUrl;

  private String clientSecret;

  private String username;

  private String password;

  private String masterRealm;

  private String adminClientId;

  private String realm;

  private String clientId;

  private String principalAttribute;

  private Boolean disableHostnameVerification;

  private Boolean extractRolesFromResource = true;

  private Boolean extractRolesFromRealm = false;

}
