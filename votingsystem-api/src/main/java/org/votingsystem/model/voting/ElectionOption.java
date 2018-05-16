package org.votingsystem.model.voting;

import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

@Entity @Table(name="ELECTION_OPTION")
public class ElectionOption extends EntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(ElectionOption.class.getName());

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false) private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ELECTION", nullable=false) private Election election;
    @Column(name="CONTENT", length=10000, nullable=false) private String content;
    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;
    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public ElectionOption() {}

    public ElectionOption(Election election, ElectionOptionDto electionOptionDto) {
        this.content = electionOptionDto.getContent();
        this.election = election;
    }

    public ElectionOption(String content) {
        this.content = content;
    }

    public ElectionOption(Election election, String content) {
        this.election = election;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Election getElection() {
        return election;
    }

    public ElectionOption setElection(Election election) {
        this.election = election;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

}