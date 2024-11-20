package de.terrestris.mde.mde_backend.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigInteger;

@Entity
@Table(name = "technical_metadata")
@Data
public class TechnicalMetadata {

  @Column(unique = true, nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private BigInteger id;

  @Column
  private String title;

  @Column
  private String metadataId;

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  private String data;

}
