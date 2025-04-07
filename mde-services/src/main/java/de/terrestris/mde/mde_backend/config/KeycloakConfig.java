package de.terrestris.mde.mde_backend.config;
import de.terrestris.mde.mde_backend.properties.KeycloakProperties;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import lombok.extern.log4j.Log4j2;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;

@Configuration
@Log4j2
public class KeycloakConfig {

  @Autowired
  private KeycloakProperties keycloakProperties;

  @Bean
  public Keycloak keycloakAdminClient() {
    Client restClient = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
      .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
      .build();

    log.info("Creating Keycloak client with properties: {}", keycloakProperties.toString());

    return KeycloakBuilder.builder()
      .serverUrl(keycloakProperties.getServerUrl())
      .realm(keycloakProperties.getRealm())
      .clientSecret(keycloakProperties.getClientSecret())
      .clientId(keycloakProperties.getClientId())
      .grantType(CLIENT_CREDENTIALS)
      .resteasyClient(restClient)
      .build();
  }

  @Bean
  public RealmResource getRealm(@Autowired Keycloak kc) {
    return kc.realm(keycloakProperties.getRealm());
  }

}
