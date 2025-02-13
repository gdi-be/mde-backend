package de.terrestris.mde.mde_backend.model;

import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "iso_metadata")
@Data
@Indexed
public class IsoMetadata extends BaseMetadata {

  @Formula("(data->>'title')")
  private String title;

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  @IndexedEmbedded
  private JsonIsoMetadata data;

  public IsoMetadata (String metadataId) {
    super();
    setMetadataId(metadataId);
    setData(new JsonIsoMetadata());
  }

}
