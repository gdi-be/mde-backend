package de.terrestris.mde.mde_backend.model;

import de.terrestris.mde.mde_backend.model.json.JsonTechnicalMetadata;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "technical_metadata")
@Data
public class TechnicalMetadata extends BaseMetadata {

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  private JsonTechnicalMetadata data;

}
