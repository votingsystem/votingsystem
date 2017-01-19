package org.votingsystem.model.currency;

import org.votingsystem.model.EntityBase;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.CurrencyCode;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CURRENCY_ACCOUNT")
@NamedQueries({
        @NamedQuery(name = CurrencyAccount.FIND_BY_TYPE_AND_USER, query =
                "SELECT account FROM CurrencyAccount account WHERE account.user =:user AND account.type =:type"),
        @NamedQuery(name = CurrencyAccount.FIND_BY_USER, query =
                "SELECT account FROM CurrencyAccount account WHERE account.user =:user"),
        @NamedQuery(name = CurrencyAccount.FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE, query =
                "SELECT account FROM CurrencyAccount account WHERE account.IBAN =:userIBAN and account.tag =:tag " +
                "and account.currencyCode =:currencyCode and account.state =:state"),
        @NamedQuery(name = CurrencyAccount.FIND_BY_USER_AND_STATE, query = "SELECT account FROM CurrencyAccount account WHERE " +
                "account.user =:user and account.state =:state")
})
public class CurrencyAccount extends EntityBase implements Serializable {

    private static Logger log = Logger.getLogger(CurrencyAccount.class.getName());

    public static final String FIND_BY_TYPE_AND_USER = "CurrencyAccount.findByTypeAndUser";
    public static final String FIND_BY_USER = "CurrencyAccount.findByUser";
    public static final String FIND_BY_USER_IBAN_AND_TAG_AND_CURRENCY_CODE_AND_STATE =
            "CurrencyAccount.findByUserIBANAndTagAndCurrencyCodeAndState";
    public static final String FIND_BY_USER_AND_STATE = "CurrencyAccount.findByUserAndState";


    public enum State {ACTIVE, SUSPENDED, CANCELED}

    public enum Type {SYSTEM, EXTERNAL}

    public static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Column(name="STATE") @Enumerated(EnumType.STRING)
    private State state = State.ACTIVE;

    @Column(name="TYPE") @Enumerated(EnumType.STRING)
    private Type type = Type.SYSTEM;

    @Column(name="BALANCE", nullable=false)
    private BigDecimal balance = null;

    @Column(name="CURRENCY_CODE", nullable=false) @Enumerated(EnumType.STRING)
    private CurrencyCode currencyCode;

    @Column(name="IBAN", nullable=false)
    private String IBAN;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="USER_ID") private User user;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="TAG") private Tag tag;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    public CurrencyAccount() {}

    public CurrencyAccount(User user, BigDecimal balance, CurrencyCode currencyCode, Tag tag) {
        this.currencyCode = currencyCode;
        this.user = user;
        this.balance = balance;
        this.IBAN = user.getIBAN();
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public CurrencyAccount setBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void afterInsert() {
        if(balance.compareTo(BigDecimal.ZERO) < 0) {
            log.log(Level.SEVERE, "CurrencyAccount:" + id + "### NEGATIVE balance ###");
        }
    }

}