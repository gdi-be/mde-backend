package de.terrestris.mde.mde_backend.model;

import jakarta.persistence.*;
import java.math.BigInteger;
import lombok.*;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_deletion")
@Data
public class ServiceDeletion {

  @Column(unique = true, nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private BigInteger id;

  @Column @Setter private String metadataId;

  @Column @Setter private String fileIdentifier;
}
