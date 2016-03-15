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
@Table(name="BankInfo")
@NamedQueries({
        @NamedQuery(name = "findBankInfoByBank", query = "SELECT b FROM BankInfo b WHERE b.bank =:bank")
})
public class BankInfo extends EntityVS implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @OneToOne  private Bank bank;
    @Column(name="bankCode" ) private String bankCode;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public BankInfo() {}

    public BankInfo(Bank bank, String bankCode) {
        this.bank = bank;
        this.bankCode = bankCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bank getBank() {
        return bank;
    }

    public BankInfo setBank(Bank bank) {
        this.bank = bank;
        return this;
    }

    public String getBankCode() {
        return bankCode;
    }

    public BankInfo setBankCode(String bankCode) {
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
