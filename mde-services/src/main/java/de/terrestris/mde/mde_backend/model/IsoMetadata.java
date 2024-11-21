package de.terrestris.mde.mde_backend.model;

import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;

import java.math.BigInteger;
import java.util.List;

@Entity
@Table(name = "iso_metadata")
@Data
public class IsoMetadata {

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
  private List<JsonIsoMetadata> data;

}
