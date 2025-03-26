package de.terrestris.mde.mde_backend.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Objects;

@Getter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Indexed
public abstract class BaseMetadata implements Serializable {

    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private BigInteger id;

    @CreationTimestamp
    @Column(updatable = false)
    @Setter
    @Schema(
      description = "The timestamp of creation.",
      accessMode = Schema.AccessMode.READ_ONLY
    )
    private OffsetDateTime created;

    @UpdateTimestamp
    @Column
    @Setter
    @Schema(
      description = "The timestamp of the last modification.",
      accessMode = Schema.AccessMode.READ_ONLY
    )
    private OffsetDateTime modified;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BaseMetadata that = (BaseMetadata) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
