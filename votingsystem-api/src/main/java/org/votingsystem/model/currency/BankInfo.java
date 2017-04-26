package org.votingsystem.model.currency;

import org.iban4j.CountryCode;
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
@Table(name="BANK_INFO",
        uniqueConstraints= @UniqueConstraint(columnNames = {"BANK_CODE", "COUNTRY_CODE"}))
@NamedQueries({
        @NamedQuery(name = BankInfo.FIND_BY_BANK, query = "SELECT b FROM BankInfo b WHERE b.bank =:bank")
})
public class BankInfo extends EntityBase implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String FIND_BY_BANK = "BankInfo.findBankInfoByBank";


    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @OneToOne
    private Bank bank;

    @Column(name="BANK_CODE", nullable=false)
    private String bankCode;

    @Column(name="COUNTRY_CODE", nullable=false) @Enumerated(EnumType.STRING)
    private CountryCode countryCode;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public BankInfo() {}

    public BankInfo(Bank bank, String bankCode, CountryCode countryCode) {
        this.bank = bank;
        this.bankCode = bankCode;
        this.countryCode = countryCode;
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

    @Override
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    @Override
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public CountryCode getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(CountryCode countryCode) {
        this.countryCode = countryCode;
    }
}
