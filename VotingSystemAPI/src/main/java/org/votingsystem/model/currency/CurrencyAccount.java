package org.votingsystem.model.currency;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import org.votingsystem.util.EntityVS;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CurrencyAccount")
@NamedQueries({
        @NamedQuery(name = "findAccountByTypeAndUser", query =
                "SELECT account FROM CurrencyAccount account WHERE account.user =:user AND account.type =:type"),
        @NamedQuery(name = "findAccountByUser", query =
                "SELECT account FROM CurrencyAccount account WHERE account.user =:user"),
        @NamedQuery(name = "findAccountByUserIBANAndTagAndCurrencyCodeAndState", query =
                "SELECT account FROM CurrencyAccount account WHERE account.IBAN =:userIBAN and account.tag =:tag " +
                "and account.currencyCode =:currencyCode and account.state =:state"),
        @NamedQuery(name = "findAccountByUserAndState", query = "SELECT account FROM CurrencyAccount account WHERE " +
                "account.user =:user and account.state =:state"),
        @NamedQuery(name = "findAccountByUserIBANAndStateAndCurrencyAndTag", query = "SELECT account FROM CurrencyAccount account WHERE " +
                "account.user.IBAN =:userIBAN and account.state =:state and account.currencyCode =:currencyCode " +
                "and account.tag =:tag")


})
public class CurrencyAccount extends EntityVS implements Serializable {

    private static Logger log = Logger.getLogger(CurrencyAccount.class.getName());

    public enum State {ACTIVE, SUSPENDED, CANCELED}

    public enum Type {SYSTEM, EXTERNAL}

    public static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @Column(name="state") @Enumerated(EnumType.STRING) private State state = State.ACTIVE;
    @Column(name="type") @Enumerated(EnumType.STRING) private Type type = Type.SYSTEM;

    @Column(name="balance", nullable=false) private BigDecimal balance = null;

    @Column(name="currencyCode", nullable=false) private String currencyCode;
    @Column(name="IBAN", nullable=false) private String IBAN;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user") private User user;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tag") private TagVS tag;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public CurrencyAccount() {}

    public CurrencyAccount(User user, BigDecimal balance, String currencyCode, TagVS tag) {
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

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public TagVS getTag() {
        return tag;
    }

    public void setTag(TagVS tag) {
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
