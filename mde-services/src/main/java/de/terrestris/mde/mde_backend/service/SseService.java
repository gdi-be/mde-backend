package de.terrestris.mde.mde_backend.service;

import de.terrestris.mde.mde_backend.model.dto.sse.HeartbeatMessage;
import de.terrestris.mde.mde_backend.model.dto.sse.SseMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class SseService {

  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  /**
   * Creates a new SseEmitter and associates it with the Keycloak ID of the authenticated user.
   *
   * @return The created SseEmitter.
   * @throws Exception If the authentication is not a JwtAuthenticationToken.
   */
  @PreAuthorize("isAuthenticated()")
  public SseEmitter createEmitter() throws Exception {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof JwtAuthenticationToken)) {
      throw new Exception("Authentication is not a JwtAuthenticationToken");
    }

    String keycloakId = ((JwtAuthenticationToken) authentication).getToken().getClaimAsString("sub");

    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

    if (emitters.get(keycloakId) != null) {
      log.debug("Emitter already exists for keycloak ID: {}", keycloakId);

      return emitters.get(keycloakId);
    }

    emitters.put(keycloakId, emitter);

    emitter.onCompletion(() -> {
      log.info("Emitter completed");
      emitters.remove(keycloakId);
    });
    emitter.onTimeout(() -> {
      log.info("Emitter timed out");
      emitters.remove(keycloakId);
    });
    emitter.onError(e -> {
      log.info("Error in emitter: {}", e.getMessage());
      emitters.remove(keycloakId);
    });

    return emitter;
  }

  /**
   * Sends a message to the emitter associated with the given keycloak ID.
   *
   * @param name       The name of the event.
   * @param keycloakId The keycloak ID of the user.
   * @param message    The message to send.
   */
  public void send(String name, String keycloakId, SseMessage message) {
    SseEmitter emitter = emitters.get(keycloakId);

    if (emitter == null) {
      log.warn("No emitter found for keycloak ID: {}", keycloakId);
      return;
    }

    try {
      emitter.send(SseEmitter.event()
        // TODO Check if id is needed, can most probably be omitted
        .name(name)
        .data(message)
      );
    } catch (IOException e) {
      emitter.completeWithError(e);
      emitters.remove(keycloakId);
    }
  }

  /**
   * Broadcasts a message to all emitters.
   *
   * @param name    The name of the event.
   * @param message The message to send.
   */
  public void broadcast(String name, SseMessage message) {
    emitters.forEach((keycloakId, emitter) -> {
      try {
        emitter.send(SseEmitter.event()
          .name(name)
          .data(message)
        );
      } catch (IOException e) {
        emitter.completeWithError(e);
        emitters.remove(keycloakId);
      }
    });
  }

  /**
   * Sends a heartbeat message to all emitters every 15 seconds to prevent
   * any timeout (even if set otherwise on the used emitter).
   */
  // TODO Make rate configurable via application.properties
  @Scheduled(fixedRate = 15000)
  public void sendHeartbeat() {
    broadcast("heartbeat", new HeartbeatMessage("Hello There"));
  }

}
