package org.votingsystem.model.currency;

import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;

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
@Table(name="TransactionVS")
@NamedQueries({
        @NamedQuery(name = "countTransByToUserVSIsNullAndTypeAndDateCreatedBetween", query =
        "SELECT COUNT(t) FROM TransactionVS t WHERE t.dateCreated between :dateFrom and :dateTo AND t.toUserVS is null AND t.type =:type"),
        @NamedQuery(name = "countTransByToUserVSIsNotNullAndTypeAndDateCreatedBetween", query =
        "SELECT COUNT(t) FROM TransactionVS t WHERE t.dateCreated between :dateFrom and :dateTo AND t.toUserVS is not null AND t.type =:type"),
        @NamedQuery(name="countTransByTypeAndDateCreatedBetween", query=
        "SELECT COUNT(t) FROM TransactionVS t WHERE t.type =:type AND t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name="findSystemTransactionVSList", query=
                "SELECT t FROM TransactionVS t WHERE (t.state =:state and t.transactionParent is not null " +
                        "and t.dateCreated between :dateFrom and :dateTo) OR (t.state =:state and t.transactionParent is null " +
                        "and t.type in :typeList)"),
        @NamedQuery(name="findSystemTransactionVSFromList", query=
                "SELECT t FROM TransactionVS t WHERE t.transactionParent is null and t.dateCreated between :dateFrom and :dateTo and t.type not in :typeList"),
        @NamedQuery(name="countTransByTransactionParent", query=
                "SELECT COUNT(t) FROM TransactionVS t WHERE t.transactionParent =:transactionParent"),
        @NamedQuery(name="findUserVSTransFromByFromUserAndStateAndDateCreatedAndInList", query=
                "SELECT t FROM TransactionVS t WHERE (t.fromUserVS =:fromUserVS and t.state =:state " +
                        "and t.transactionParent is not null and t.dateCreated between :dateFrom and :dateTo) " +
                        "OR (t.fromUserVS =:fromUserVS and t.state =:state and t.transactionParent is null " +
                        "and  t.dateCreated between :dateFrom and :dateTo and t.type in (:inList))"),
        @NamedQuery(name="findTransByToUserAndStateAndDateCreatedBetween", query= "SELECT t FROM TransactionVS t " +
                "WHERE t.toUserVS =:toUserVS and t.state =:state and t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name="findTransByFromUserAndTransactionParentNullAndDateCreatedBetween", query= "SELECT t FROM TransactionVS t " +
        "WHERE t.fromUserVS =:fromUserVS and t.transactionParent is null and t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name="findTransByToUserAndDateCreatedBetween", query= "SELECT t FROM TransactionVS t  WHERE " +
                "t.toUserVS =:toUserVS and t.dateCreated between :dateFrom and :dateTo")
})
public class TransactionVS implements Serializable {

    private static Logger log = Logger.getLogger(TransactionVS.class.getName());
    
    public static final long serialVersionUID = 1L;

    public enum Source {FROM, TO}

    public enum Type {FROM_BANKVS, FROM_USERVS, FROM_GROUP_TO_MEMBER_GROUP, FROM_GROUP_TO_ALL_MEMBERS,
        CURRENCY_PERIOD_INIT, CURRENCY_PERIOD_INIT_TIME_LIMITED, CURRENCY_REQUEST, CURRENCY_SEND, CURRENCY_CHANGE,
        CANCELLATION, TRANSACTIONVS_INFO; }

    public enum State { OK, REPEATED, CANCELED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Column(name="subject") private String subject;

    @Column(name="currency", nullable=false) private String currencyCode;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tag", nullable=false) private TagVS tag;

    @Column(name="amount") private BigDecimal amount = null;
    @OneToOne private MessageCMS messageCMS;

    @OneToOne private MessageCMS cancellationCMS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionParent") private TransactionVS transactionParent;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fromUserVS") private UserVS fromUserVS;

    //This is to set the data of the Bank client the transaction comes from
    @Column(name="fromUserIBAN") private String fromUserIBAN;
    @Column(name="fromUser") private String fromUser;

    @Column(name="toUserIBAN") private String toUserIBAN;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;
    @OneToOne @JoinColumn(name="currencyBatch")  private CurrencyBatch currencyBatch;
    @Column(name="isTimeLimited") private Boolean isTimeLimited;
    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    @Transient private Map<CurrencyAccount, BigDecimal> accountFromMovements;
    @Transient private Long userId;
    @Transient private List<String> toUserVSList;

    public TransactionVS() {}

    public TransactionVS(UserVS fromUserVS, UserVS toUserVS, BigDecimal amount, String currencyCode, String subject,
                         MessageCMS messageCMS, Type type, State state, TagVS tag) {
        this.amount = amount;
        this.fromUserVS = fromUserVS;
        this.fromUserIBAN = fromUserVS.getIBAN();
        this.toUserVS = toUserVS;
        this.toUserIBAN = toUserVS.getIBAN();
        this.messageCMS = messageCMS;
        this.subject = subject;
        this.currencyCode = currencyCode;
        this.type = type;
        this.state = state;
        this.tag = tag;
    }

    public static TransactionVS CURRENCY_SEND(CurrencyBatch batch, UserVS toUserVS, Date validTo,
                                              MessageCMS messageCMS, TagVS tagVS) {
        TransactionVS transactionVS = BASIC(toUserVS, TransactionVS.Type.CURRENCY_SEND, null, batch.getBatchAmount(),
                batch.getCurrencyCode(), batch.getSubject(), validTo, messageCMS, batch.getTagVS());
        transactionVS.setToUserIBAN(batch.getToUserVS().getIBAN());
        transactionVS.setCurrencyBatch(batch);
        transactionVS.setState(TransactionVS.State.OK);
        transactionVS.setTag(tagVS);
        transactionVS.setIsTimeLimited(batch.getTimeLimited());
        return transactionVS;
    }

    public static TransactionVS CURRENCY_CHANGE(CurrencyBatch batch, Date validTo, MessageCMS messageCMS, TagVS tagVS) {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setType(Type.CURRENCY_CHANGE);
        transactionVS.setAmount(batch.getBatchAmount());
        transactionVS.setCurrencyCode(batch.getCurrencyCode());
        transactionVS.setSubject(batch.getSubject());
        transactionVS.setValidTo(validTo);
        transactionVS.setMessageCMS(messageCMS);
        transactionVS.setTag(tagVS);
        transactionVS.setCurrencyBatch(batch);
        transactionVS.setState(TransactionVS.State.OK);
        transactionVS.setIsTimeLimited(batch.getTimeLimited());
        return transactionVS;
    }

    public static TransactionVS USERVS(UserVS userVS, UserVS toUserVS, Type type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
                                       BigDecimal amount, String currencyCode, String subject, Date validTo, MessageCMS messageCMS, TagVS tag) {
        TransactionVS transactionVS = BASIC(toUserVS, type, accountFromMovements, amount, currencyCode, subject,
                validTo, messageCMS, tag);
        transactionVS.setFromUserVS(userVS);
        transactionVS.setFromUserIBAN(userVS.getIBAN());
        return transactionVS;
    }

    public static TransactionVS BASIC(UserVS toUserVS, Type type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
                                      BigDecimal amount, String currencyCode, String subject, Date validTo, MessageCMS messageCMS, TagVS tag) {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setToUserVS(toUserVS);
        transactionVS.setToUserIBAN(toUserVS.getIBAN());
        transactionVS.setType(type);
        transactionVS.setAccountFromMovements(accountFromMovements);
        transactionVS.setAmount(amount);
        transactionVS.setCurrencyCode(currencyCode);
        transactionVS.setSubject(subject);
        transactionVS.setValidTo(validTo);
        transactionVS.setMessageCMS(messageCMS);
        transactionVS.setTag(tag);
        transactionVS.setState(TransactionVS.State.OK);
        return transactionVS;
    }

    public static TransactionVS FROM_BANKVS(BankVS bankVS, String bankClientIBAN, String bankClientName, UserVS toUser,
                                            BigDecimal amount, String currencyCode, String subject, Date validTo, MessageCMS messageCMS, TagVS tag) {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setFromUserVS(bankVS);
        transactionVS.setFromUserIBAN(bankClientIBAN);
        transactionVS.setFromUser(bankClientName);
        transactionVS.setToUserVS(toUser);
        transactionVS.setToUserIBAN(toUser.getIBAN());
        transactionVS.setAmount(amount);
        transactionVS.setCurrencyCode(currencyCode);
        transactionVS.setSubject(subject);
        transactionVS.setValidTo(validTo);
        transactionVS.setMessageCMS(messageCMS);
        transactionVS.setTag(tag);
        transactionVS.setState(TransactionVS.State.OK);
        transactionVS.setType(TransactionVS.Type.FROM_BANKVS);
        return transactionVS;
    }

    public static TransactionVS CURRENCY_REQUEST(String subject, Map<CurrencyAccount, BigDecimal> accountFromMovements,
             CurrencyRequestDto requestDto) {
        TransactionVS transaction = new TransactionVS();
        transaction.setType(TransactionVS.Type.CURRENCY_REQUEST);
        transaction.setState(TransactionVS.State.OK);
        transaction.setAmount(requestDto.getTotalAmount());
        transaction.setCurrencyCode(requestDto.getCurrencyCode());
        transaction.setTag(requestDto.getTagVS());
        transaction.setSubject(subject);
        transaction.setMessageCMS(requestDto.getMessageCMS());
        transaction.setFromUserVS(requestDto.getMessageCMS().getUserVS());
        transaction.setAccountFromMovements(accountFromMovements);
        return transaction;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MessageCMS getMessageCMS() {
        return messageCMS;
    }

    public void setMessageCMS(MessageCMS messageCMS) {
        this.messageCMS = messageCMS;
    }

    public MessageCMS getCancellationCMS() {
        return cancellationCMS;
    }

    public void setCancellationCMS(MessageCMS cancellationCMS) {
        this.cancellationCMS = cancellationCMS;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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


    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public List<String> getToUserVSList() {
        return toUserVSList;
    }

    public void setToUserVSList(List<String> toUserVSList) {
        this.toUserVSList = toUserVSList;
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

    public TransactionVS setCurrencyBatch(CurrencyBatch currencyBatch) {
        this.currencyBatch = currencyBatch;
        return this;
    }

    public static TransactionVS generateTriggeredTransaction(TransactionVS transactionParent, BigDecimal amount,
             UserVS toUser, String toUserIBAN) {
        TransactionVS result = new TransactionVS();
        result.amount = amount;
        result.messageCMS = transactionParent.messageCMS;
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