package org.votingsystem.model.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="TAG")
@NamedQueries({
        @NamedQuery(name = Tag.FIND_BY_NAME, query = "SELECT tag FROM Tag tag WHERE tag.name =:name")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag extends EntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FIND_BY_NAME = "Tag.findByName";

    public static final String WILDTAG = "WILDTAG";

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Column(name="NAME", nullable=false, unique=true, length=50)
    private String name;

    @Column(name="FREQUENCY")
    private Long frequency;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;


    public Tag() { }

    public Tag(String name) {
        this.name = name;
    }

    public Tag(String name, Long frequency) {
        this.name = name;
        this.frequency = frequency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tag setFrequency(Long frequency) {
        this.frequency = frequency;
        return this;
    }

    public Long getFrequency() {
        return frequency;
    }

    @Override
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

}