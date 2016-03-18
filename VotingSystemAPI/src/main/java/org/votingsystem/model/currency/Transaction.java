package org.votingsystem.model.currency;

import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.User;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="Transaction")
@NamedQueries({
        @NamedQuery(name = "countTransByToUserIsNullAndTypeAndDateCreatedBetween", query =
        "SELECT COUNT(t) FROM Transaction t WHERE t.dateCreated between :dateFrom and :dateTo AND t.toUser is null AND t.type =:type"),
        @NamedQuery(name = "countTransByToUserIsNotNullAndTypeAndDateCreatedBetween", query =
        "SELECT COUNT(t) FROM Transaction t WHERE t.dateCreated between :dateFrom and :dateTo AND t.toUser is not null AND t.type =:type"),
        @NamedQuery(name="countTransByTypeAndDateCreatedBetween", query=
        "SELECT COUNT(t) FROM Transaction t WHERE t.type =:type AND t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name="findSystemTransactionList", query=
                "SELECT t FROM Transaction t WHERE (t.state =:state and t.transactionParent is not null " +
                        "and t.dateCreated between :dateFrom and :dateTo) OR (t.state =:state and t.transactionParent is null " +
                        "and t.type in :typeList)"),
        @NamedQuery(name="findSystemTransactionFromList", query=
                "SELECT t FROM Transaction t WHERE t.transactionParent is null and t.dateCreated between :dateFrom and :dateTo and t.type not in :typeList"),
        @NamedQuery(name="countTransByTransactionParent", query=
                "SELECT COUNT(t) FROM Transaction t WHERE t.transactionParent =:transactionParent"),
        @NamedQuery(name="findUserTransFromByFromUserAndStateAndDateCreatedAndInList", query=
                "SELECT t FROM Transaction t WHERE (t.fromUser =:fromUser and t.state =:state " +
                        "and t.transactionParent is not null and t.dateCreated between :dateFrom and :dateTo) " +
                        "OR (t.fromUser =:fromUser and t.state =:state and t.transactionParent is null " +
                        "and  t.dateCreated between :dateFrom and :dateTo and t.type in (:inList))"),
        @NamedQuery(name="findTransByToUserAndStateAndDateCreatedBetween", query= "SELECT t FROM Transaction t " +
                "WHERE t.toUser =:toUser and t.state =:state and t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name="findTransByFromUserAndTransactionParentNullAndDateCreatedBetween", query= "SELECT t FROM Transaction t " +
        "WHERE t.fromUser =:fromUser and t.transactionParent is null and t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name="findTransByToUserAndDateCreatedBetween", query= "SELECT t FROM Transaction t  WHERE " +
                "t.toUser =:toUser and t.dateCreated between :dateFrom and :dateTo")
})
public class Transaction implements Serializable {

    private static Logger log = Logger.getLogger(Transaction.class.getName());
    
    public static final long serialVersionUID = 1L;

    public enum Source {FROM, TO}

    public enum Type {
        FROM_BANK, FROM_USER, CURRENCY_PERIOD_INIT, CURRENCY_PERIOD_INIT_TIME_LIMITED, CURRENCY_REQUEST, CURRENCY_SEND,
        CURRENCY_CHANGE, CANCELLATION, TRANSACTION_INFO; }

    public enum State { OK, REPEATED, CANCELED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Column(name="subject") private String subject;

    @Column(name="currency", nullable=false) private String currencyCode;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tag", nullable=false) private TagVS tag;

    @Column(name="amount") private BigDecimal amount = null;
    @OneToOne private CMSMessage cmsMessage;

    @OneToOne private CMSMessage cancellationCMS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionParent") private Transaction transactionParent;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fromUser") private User fromUser;

    //This is to set the data of the Bank client the transaction comes from
    @Column(name="fromUserIBAN") private String fromUserIBAN;
    @Column(name="fromUserName") private String fromUserName;

    @Column(name="toUserIBAN") private String toUserIBAN;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUser") private User toUser;
    @OneToOne @JoinColumn(name="currencyBatch")  private CurrencyBatch currencyBatch;
    @Column(name="isTimeLimited") private Boolean isTimeLimited;
    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private Map<CurrencyAccount, BigDecimal> accountFromMovements;
    @Transient private Long userId;
    @Transient private List<String> toUserList;

    public Transaction() {}

    public Transaction(User fromUser, User toUser, BigDecimal amount, String currencyCode, String subject,
                       CMSMessage cmsMessage, Type type, State state, TagVS tag) {
        this.amount = amount;
        this.fromUser = fromUser;
        this.fromUserIBAN = fromUser.getIBAN();
        this.toUser = toUser;
        this.toUserIBAN = toUser.getIBAN();
        this.cmsMessage = cmsMessage;
        this.subject = subject;
        this.currencyCode = currencyCode;
        this.type = type;
        this.state = state;
        this.tag = tag;
    }

    public static Transaction CURRENCY_SEND(CurrencyBatch batch, User toUser, Date validTo,
                                            CMSMessage cmsMessage, TagVS tagVS) {
        Transaction transaction = BASIC(toUser, Transaction.Type.CURRENCY_SEND, null, batch.getBatchAmount(),
                batch.getCurrencyCode(), batch.getSubject(), validTo, cmsMessage, batch.getTagVS());
        transaction.setToUserIBAN(batch.getToUser().getIBAN());
        transaction.setCurrencyBatch(batch);
        transaction.setState(Transaction.State.OK);
        transaction.setTag(tagVS);
        transaction.setIsTimeLimited(batch.getTimeLimited());
        return transaction;
    }

    public static Transaction CURRENCY_CHANGE(CurrencyBatch batch, Date validTo, CMSMessage cmsMessage, TagVS tagVS) {
        Transaction transaction = new Transaction();
        transaction.setType(Type.CURRENCY_CHANGE);
        transaction.setAmount(batch.getBatchAmount());
        transaction.setCurrencyCode(batch.getCurrencyCode());
        transaction.setSubject(batch.getSubject());
        transaction.setValidTo(validTo);
        transaction.setCmsMessage(cmsMessage);
        transaction.setTag(tagVS);
        transaction.setCurrencyBatch(batch);
        transaction.setState(Transaction.State.OK);
        transaction.setIsTimeLimited(batch.getTimeLimited());
        return transaction;
    }

    public static Transaction USER(User user, User toUser, Type type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
                                   BigDecimal amount, String currencyCode, String subject, Date validTo, CMSMessage cmsMessage, TagVS tag) {
        Transaction transaction = BASIC(toUser, type, accountFromMovements, amount, currencyCode, subject,
                validTo, cmsMessage, tag);
        transaction.setFromUser(user);
        transaction.setFromUserIBAN(user.getIBAN());
        return transaction;
    }

    public static Transaction BASIC(User toUser, Type type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
                                    BigDecimal amount, String currencyCode, String subject, Date validTo, CMSMessage cmsMessage, TagVS tag) {
        Transaction transaction = new Transaction();
        transaction.setToUser(toUser);
        transaction.setToUserIBAN(toUser.getIBAN());
        transaction.setType(type);
        transaction.setAccountFromMovements(accountFromMovements);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setSubject(subject);
        transaction.setValidTo(validTo);
        transaction.setCmsMessage(cmsMessage);
        transaction.setTag(tag);
        transaction.setState(Transaction.State.OK);
        return transaction;
    }

    public static Transaction FROM_BANK(Bank bank, String bankClientIBAN, String bankClientName, User toUser,
                                        BigDecimal amount, String currencyCode, String subject, Date validTo, CMSMessage cmsMessage, TagVS tag) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(bank);
        transaction.setFromUserIBAN(bankClientIBAN);
        transaction.setFromUserName(bankClientName);
        transaction.setToUser(toUser);
        transaction.setToUserIBAN(toUser.getIBAN());
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setSubject(subject);
        transaction.setValidTo(validTo);
        transaction.setCmsMessage(cmsMessage);
        transaction.setTag(tag);
        transaction.setState(Transaction.State.OK);
        transaction.setType(Transaction.Type.FROM_BANK);
        return transaction;
    }

    public static Transaction CURRENCY_REQUEST(String subject, Map<CurrencyAccount, BigDecimal> accountFromMovements,
                                               CurrencyRequestDto requestDto) {
        Transaction transaction = new Transaction();
        transaction.setType(Transaction.Type.CURRENCY_REQUEST);
        transaction.setState(Transaction.State.OK);
        transaction.setAmount(requestDto.getTotalAmount());
        transaction.setCurrencyCode(requestDto.getCurrencyCode());
        transaction.setTag(requestDto.getTagVS());
        transaction.setSubject(subject);
        transaction.setCmsMessage(requestDto.getCmsMessage());
        transaction.setFromUser(requestDto.getCmsMessage().getUser());
        transaction.setAccountFromMovements(accountFromMovements);
        return transaction;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CMSMessage getCmsMessage() {
        return cmsMessage;
    }

    public void setCmsMessage(CMSMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    public CMSMessage getCancellationCMS() {
        return cancellationCMS;
    }

    public void setCancellationCMS(CMSMessage cancellationCMS) {
        this.cancellationCMS = cancellationCMS;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
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

    public Transaction getTransactionParent() {
        return transactionParent;
    }

    public void setTransactionParent(Transaction transactionParent) {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUser) {
        this.fromUserName = fromUser;
    }


    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public List<String> getToUserList() {
        return toUserList;
    }

    public void setToUserList(List<String> toUserList) {
        this.toUserList = toUserList;
    }

    public TagVS getTag() {
        return tag;
    }

    public void setTag(TagVS tag) {
        this.tag = tag;
    }

    public String getTagName() {
        if(tag == null) return null;
        else return tag.getName();
    }

    public CurrencyBatch getCurrencyBatch() {
        return currencyBatch;
    }

    public Transaction setCurrencyBatch(CurrencyBatch currencyBatch) {
        this.currencyBatch = currencyBatch;
        return this;
    }

    public static Transaction generateTriggeredTransaction(Transaction transactionParent, BigDecimal amount,
                                                           User toUser, String toUserIBAN) {
        Transaction result = new Transaction();
        result.amount = amount;
        result.cmsMessage = transactionParent.cmsMessage;
        result.fromUser = transactionParent.fromUser;
        result.fromUser = transactionParent.fromUser;
        result.fromUserIBAN = transactionParent.fromUserIBAN;
        result.state = transactionParent.state;
        result.validTo = transactionParent.validTo;
        result.subject = transactionParent.subject;
        result.currencyCode = transactionParent.currencyCode;
        result.type = transactionParent.type;
        result.tag = transactionParent.tag;
        result.transactionParent = transactionParent;
        result.toUser = toUser;
        result.toUserIBAN = toUserIBAN;
        return result;
    }

    public Map<CurrencyAccount, BigDecimal> getAccountFromMovements() {
        return accountFromMovements;
    }

    public void setAccountFromMovements(Map<CurrencyAccount, BigDecimal> accountFromMovements) {
        this.accountFromMovements = accountFromMovements;
    }

    public void addAccountFromMovement(CurrencyAccount currencyAccount, BigDecimal amount) {
        if(accountFromMovements == null)  accountFromMovements = new HashMap<CurrencyAccount, BigDecimal>();
        accountFromMovements.put(currencyAccount, amount);
    }

    @PrePersist
    public void prePersist() {
        Date date = new Date();
        setDateCreated(date);
        setLastUpdated(date);
        if(this.validTo != null) isTimeLimited = Boolean.TRUE;
        else isTimeLimited = Boolean.FALSE;
    }

    @PostPersist
    public void postPersist() { }


    @PreUpdate
    public void preUpdate() {
        setLastUpdated(new Date());
    }

}