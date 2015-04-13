package org.votingsystem.model.currency;

import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="BankVSInfo")
@NamedQueries({
        @NamedQuery(name = "findBankVSInfoByBank", query = "SELECT b FROM BankVSInfo b WHERE b.bankVS =:bankVS")
})
public class BankVSInfo extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne  private BankVS bankVS;
    @Column(name="bankCode" ) private String bankCode;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public BankVSInfo() {}

    public BankVSInfo(BankVS bankVS, String bankCode) {
        this.bankVS = bankVS;
        this.bankCode = bankCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankVS getBankVS() {
        return bankVS;
    }

    public BankVSInfo setBankVS(BankVS bankVS) {
        this.bankVS = bankVS;
        return this;
    }

    public String getBankCode() {
        return bankCode;
    }

    public BankVSInfo setBankCode(String bankCode) {
        this.bankCode = bankCode;
        return this;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
