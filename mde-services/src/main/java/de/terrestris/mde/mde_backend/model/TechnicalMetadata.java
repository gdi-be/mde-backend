package de.terrestris.mde.mde_backend.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "technical_metadata")
@Data
public class TechnicalMetadata extends BaseMetadata {

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  private String data;

}
