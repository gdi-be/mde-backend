package de.terrestris.mde.mde_backend.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@JsonDeserialize(as = Comment.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class Comment {

  // mde-client related id
  private UUID id;

  private String text;

  private String date;

  private String userId;

  private String userName;

  public Comment() {
    this.id = UUID.randomUUID();
  }

  public Comment(String text, String userId, String userName) {
    this.id = UUID.randomUUID();
    this.text = text;
    this.userId = userId;
    this.userName = userName;
    this.date = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
  }
}
