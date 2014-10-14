package org.votingsystem.vicket.model;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.TagVS;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="UserVSAccount")
public class UserVSAccount implements Serializable {

    private static Logger log = Logger.getLogger(UserVSAccount.class);

    public enum State {ACTIVE, SUSPENDED, CANCELLED}

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
    @JoinColumn(name="userVS") private UserVS userVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tag") private TagVS tag;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
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

    public UserVSAccount.State getState() {
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
            AlertVS alert = new AlertVS("UserVSAccount_id_" + this.id + "_negativeBalance_" + this.balance.toString());
            ContextVS.getInstance().alert(alert);
        }
    }

}
