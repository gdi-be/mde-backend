package de.terrestris.mde.mde_backend.model;

import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Type;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "iso_metadata")
@Data
public class IsoMetadata extends BaseMetadata {

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  private JsonIsoMetadata data;

  public IsoMetadata (String title, String metadataId) {
    super();
    setTitle(title);
    setMetadataId(metadataId);
    setData(new JsonIsoMetadata());
  }

}
