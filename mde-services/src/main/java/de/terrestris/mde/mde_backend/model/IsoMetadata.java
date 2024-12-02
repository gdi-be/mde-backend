package de.terrestris.mde.mde_backend.model;

import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@DynamicUpdate
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "isometadata")
@Table(name = "iso_metadata")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class IsoMetadata extends BaseMetadata {

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  private List<JsonIsoMetadata> data;

}
