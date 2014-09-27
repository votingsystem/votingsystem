package org.votingsystem.vicket.model;

import org.apache.log4j.Logger;
import org.springframework.format.annotation.NumberFormat;
import org.votingsystem.model.*;
import org.votingsystem.vicket.service.TransactionVSService;
import org.votingsystem.vicket.util.ApplicationContextHolder;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="TransactionVS")
public class TransactionVS  implements Serializable {

    private static Logger log = Logger.getLogger(TransactionVS.class);

    public static final long serialVersionUID = 1L;

    public enum Type {FROM_BANKVS, FROM_USERVS, FROM_GROUP_TO_MEMBER_GROUP, FROM_GROUP_TO_MEMBER,
        FROM_GROUP_TO_ALL_MEMBERS, VICKET_INIT_PERIOD, VICKET_REQUEST, VICKET_SEND, VICKET_CANCELLATION, CANCELLATION;}

    public enum State { OK, REPEATED, CANCELLED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Column(name="subject") private String subject;

    @Column(name="currency", nullable=false) private String currencyCode;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tag", nullable=false) private VicketTagVS tag;

    @NumberFormat(style= NumberFormat.Style.CURRENCY) private BigDecimal amount = null;
    @OneToOne private MessageSMIME messageSMIME;

    @OneToOne private MessageSMIME cancellationSMIME;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionParent") private TransactionVS transactionParent;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fromUserVS") private UserVS fromUserVS;

    //This is for Transactions From BankVS, and not anonymous transactions
    @Column(name="fromUserIBAN") private String fromUserIBAN;
    @Column(name="fromUser") private String fromUser;

    @Column(name="toUserIBAN") private String toUserIBAN;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;

    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private Map<UserVSAccount, BigDecimal> accountFromMovements;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public MessageSMIME getCancellationSMIME() {
        return cancellationSMIME;
    }

    public void setCancellationSMIME(MessageSMIME cancellationSMIME) {
        this.cancellationSMIME = cancellationSMIME;
    }

    public UserVS getFromUserVS() {
        return fromUserVS;
    }

    public void setFromUserVS(UserVS fromUserVS) {
        this.fromUserVS = fromUserVS;
    }

    public UserVS getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVS toUserVS) {
        this.toUserVS = toUserVS;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionVS getTransactionParent() {
        return transactionParent;
    }

    public void setTransactionParent(TransactionVS transactionParent) {
        this.transactionParent = transactionParent;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getFromUserIBAN() {
        return fromUserIBAN;
    }

    public void setFromUserIBAN(String fromUserIBAN) {
        this.fromUserIBAN = fromUserIBAN;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public static TransactionVS generateTriggeredTransaction(TransactionVS transactionParent, BigDecimal amount,
             UserVS toUser, String toUserIBAN) {
        TransactionVS result = new TransactionVS();
        result.amount = amount;
        result.messageSMIME = transactionParent.messageSMIME;
        result.fromUserVS = transactionParent.fromUserVS;
        result.fromUser = transactionParent.fromUser;
        result.fromUserIBAN = transactionParent.fromUserIBAN;
        result.state = transactionParent.state;
        result.validTo = transactionParent.validTo;
        result.subject = transactionParent.subject;
        result.currencyCode = transactionParent.currencyCode;
        result.type = transactionParent.type;
        result.tag = transactionParent.tag;
        result.transactionParent = transactionParent;
        result.toUserVS = toUser;
        result.toUserIBAN = toUserIBAN;
        return result;
    }

    public void afterInsert() {
        ((TransactionVSService)ApplicationContextHolder.getBean("transactionVSService")).updateBalances(this);
    }

    public VicketTagVS getTag() {
        return tag;
    }

    public void setTag(VicketTagVS tag) {
        this.tag = tag;
    }

    public Map<UserVSAccount, BigDecimal> getAccountFromMovements() {
        return accountFromMovements;
    }

    public void setAccountFromMovements(Map<UserVSAccount, BigDecimal> accountFromMovements) {
        this.accountFromMovements = accountFromMovements;
    }

}
