package de.terrestris.mde.mde_backend.event.sse.validation;

import de.terrestris.mde.mde_backend.model.dto.sse.ValidationMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ValidationEventPublisher {
  private final ApplicationEventPublisher publisher;

  public ValidationEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  public void publishEvent(ValidationMessage message) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof JwtAuthenticationToken)) {
     throw new RuntimeException("Could not publish event, user probably not logged in");
    }

    String keycloakId = ((JwtAuthenticationToken) authentication).getToken().getClaimAsString("sub");

    publisher.publishEvent(new ValidationEvent(this, message, keycloakId));
  }
}
