package de.terrestris.mde.mde_backend.model;

import de.terrestris.mde.mde_backend.enumeration.Role;
import de.terrestris.mde.mde_backend.model.json.JsonClientMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonIsoMetadata;
import de.terrestris.mde.mde_backend.model.json.JsonTechnicalMetadata;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "metadata_collection")
@Data
public class MetadataCollection extends BaseMetadata {

  @Column @Setter private String metadataId;

  @Column @Setter private String clonedFromId;

  @Formula("(iso_metadata->>'title')")
  private String title;

  @Column @Setter private Boolean approved;

  @Column
  @Setter
  @Enumerated(EnumType.STRING)
  private Status status;

  @Column
  @Setter
  @FullTextField
  @KeywordField(name = "team_member_sort", sortable = Sortable.YES)
  private Set<String> teamMemberIds;

  @Column @Setter @KeywordField private String ownerId;

  @Column @Setter @KeywordField private String assignedUserId;

  @Column
  @Setter
  @KeywordField
  @Enumerated(EnumType.STRING)
  private Role responsibleRole;

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  @Setter
  private JsonClientMetadata clientMetadata;

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  @Setter
  @IndexedEmbedded
  private JsonIsoMetadata isoMetadata;

  @Column
  @Type(JsonBinaryType.class)
  @ToString.Exclude
  @Setter
  private JsonTechnicalMetadata technicalMetadata;

  public MetadataCollection(String metadataId) {
    super();
    setMetadataId(metadataId);
    setIsoMetadata(new JsonIsoMetadata());
    setClientMetadata(new JsonClientMetadata());
    setTechnicalMetadata(new JsonTechnicalMetadata());
  }
}
