package de.terrestris.mde.mde_backend.model.dto.sse;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HeartbeatMessage extends SseMessage {
  public HeartbeatMessage(String message) {
    super(message);
  }
}
