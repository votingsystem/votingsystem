package org.votingsystem.model;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

@Entity @Table(name="FieldValueEventVS")
public class FieldValueEventVS {

    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne private SignatureVS signatureVS;
    @Column(name="value", length=1000) private String value;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fieldEventVS", nullable=false) private FieldEventVS fieldEventVS;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;
    
    public FieldValueEventVS() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFieldEventVS(FieldEventVS fieldEventVS) {
        this.fieldEventVS = fieldEventVS;
    }

    public FieldEventVS getFieldEventVS() {
        return fieldEventVS;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SignatureVS getSignatureVS() {
        return signatureVS;
    }

    public void setSignatureVS(SignatureVS signatureVS) {
        this.signatureVS = signatureVS;
    }
    
}
