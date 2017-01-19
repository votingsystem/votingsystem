package org.votingsystem.model.currency;

import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.CurrencyCode;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="TRANSACTION")
@NamedQueries({
        @NamedQuery(name = Transaction.COUNT_BY_TO_USER_NULL_AND_TYPE_AND_DATE_CREATED_BETWEEN, query =
        "SELECT COUNT(t) FROM Transaction t WHERE t.dateCreated between :dateFrom and :dateTo AND t.toUser is null AND t.type =:type"),
        @NamedQuery(name = Transaction.COUNT_BY_TO_USER_NOT_NULL_AND_TYPE_AND_DATE_CREATED_BETWEEN, query =
        "SELECT COUNT(t) FROM Transaction t WHERE t.dateCreated between :dateFrom and :dateTo AND t.toUser is not null AND t.type =:type"),
        @NamedQuery(name= Transaction.COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN, query=
        "SELECT COUNT(t) FROM Transaction t WHERE t.type =:type AND t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name= Transaction.FIND_SYSTEM_TRANSACTION, query=
                "SELECT t FROM Transaction t WHERE (t.state =:state and t.transactionParent is not null " +
                        "and t.dateCreated between :dateFrom and :dateTo) OR (t.state =:state and t.transactionParent is null " +
                        "and t.type in :typeList)"),
        @NamedQuery(name=Transaction.FIND_SYSTEM_TRANSACTION_FROM_LIST, query=
                "SELECT t FROM Transaction t WHERE t.transactionParent is null and t.dateCreated between :dateFrom and :dateTo and t.type not in :typeList"),
        @NamedQuery(name=Transaction.COUNT_BY_TRANSACTION_PARENT, query=
                "SELECT COUNT(t) FROM Transaction t WHERE t.transactionParent =:transactionParent"),
        @NamedQuery(name=Transaction.FIND_USER_TRANS_FROM_BY_FROM_USER_AND_STATE_AND_DATE_CREATED_AND_IN_LIST, query=
                "SELECT t FROM Transaction t WHERE (t.fromUser =:fromUser and t.state =:state " +
                        "and t.transactionParent is not null and t.dateCreated between :dateFrom and :dateTo) " +
                        "OR (t.fromUser =:fromUser and t.state =:state and t.transactionParent is null " +
                        "and  t.dateCreated between :dateFrom and :dateTo and t.type in (:inList))"),
        @NamedQuery(name=Transaction.FIND_TRANS_BY_TO_USER_AND_STATE_AND_DATE_CREATED_BETWEEN, query= "SELECT t FROM Transaction t " +
                "WHERE t.toUser =:toUser and t.state =:state and t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name=Transaction.FIND_TRANS_BY_FROM_USER_AND_TRANS_PARENT_NULL_AND_DATE_CREATED_BETWEEN,
                query= "SELECT t FROM Transaction t WHERE t.fromUser =:fromUser and t.transactionParent is null and t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name=Transaction.FIND_TRANS_BY_TO_USER_AND_DATE_CREATED_BETWEEN,
                query= "SELECT t FROM Transaction t  WHERE t.toUser =:toUser and t.dateCreated between :dateFrom and :dateTo")
})
public class Transaction extends EntityBase implements Serializable {

    private static Logger log = Logger.getLogger(Transaction.class.getName());
    
    public static final long serialVersionUID = 1L;


    public static final String COUNT_BY_TO_USER_NULL_AND_TYPE_AND_DATE_CREATED_BETWEEN =
            "Transaction.countTransByToUserIsNullAndTypeAndDateCreatedBetween";
    public static final String COUNT_BY_TO_USER_NOT_NULL_AND_TYPE_AND_DATE_CREATED_BETWEEN =
            "Transaction.countByToUserNotNullAndTypeAndDateCreatedBetween";
    public static final String COUNT_BY_TYPE_AND_DATE_CREATED_BETWEEN =
            "Transaction.countByTypeAndDateCreatedBetween";
    public static final String FIND_SYSTEM_TRANSACTION     = "Transaction.findSystemTransaction";
    public static final String FIND_SYSTEM_TRANSACTION_FROM_LIST     = "Transaction.findSystemTransactionFromList";
    public static final String COUNT_BY_TRANSACTION_PARENT = "Transaction.countByTransactionParent";
    public static final String FIND_USER_TRANS_FROM_BY_FROM_USER_AND_STATE_AND_DATE_CREATED_AND_IN_LIST =
            "Transaction.findUserTransFromByFromUserAndStateAndDateCreatedAndInList";
    public static final String FIND_TRANS_BY_TO_USER_AND_STATE_AND_DATE_CREATED_BETWEEN =
            "Transaction.findTransByToUserAndStateAndDateCreatedBetween";
    public static final String FIND_TRANS_BY_FROM_USER_AND_TRANS_PARENT_NULL_AND_DATE_CREATED_BETWEEN =
            "Transaction.findTransByFromUserAndTransactionParentNullAndDateCreatedBetween";
    public static final String FIND_TRANS_BY_TO_USER_AND_DATE_CREATED_BETWEEN =
            "Transaction.findTransByToUserAndDateCreatedBetween";


    public enum Source {FROM, TO}

    public enum Type {
        FROM_BANK, FROM_USER, CURRENCY_PERIOD_INIT, CURRENCY_PERIOD_INIT_TIME_LIMITED, CURRENCY_REQUEST, CURRENCY_SEND,
        CURRENCY_CHANGE, CANCELLATION, TRANSACTION_INFO; }

    public enum State { OK, REPEATED, CANCELED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Column(name="SUBJECT") private String subject;

    @Column(name="CURRENCY", nullable=false) @Enumerated(EnumType.STRING)
    private CurrencyCode currencyCode;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="TAG", nullable=false)
    private Tag tag;

    @Column(name="AMOUNT", nullable=false)
    private BigDecimal amount = null;

    @OneToOne @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="TRANSACTION_PARENT")
    private Transaction transactionParent;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="TRANSACTION_FROM_USER")
    private User fromUser;

    //This is to set the data of the Bank client the transaction comes from
    @Column(name="FROM_USER_IBAN")
    private String fromUserIBAN;

    @Column(name="FROM_USER_NAME")
    private String fromUserName;

    @Column(name="TO_USER_IBAN")
    private String toUserIBAN;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="TO_USER")
    private User toUser;

    @OneToOne @JoinColumn(name="CURRENCY_BATCH")
    private CurrencyBatch currencyBatch;

    @Column(name="IS_TIME_LIMITED")
    private Boolean isTimeLimited;

    @Column(name="TYPE", nullable=false) @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING)
    private State state;

    @Column(name="VALID_TO", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime validTo;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    @Transient private Map<CurrencyAccount, BigDecimal> accountFromMovements;
    @Transient private Long userId;
    @Transient private List<String> toUserList;

    public Transaction() {}

    public Transaction(User fromUser, User toUser, BigDecimal amount, CurrencyCode currencyCode, String subject,
                       SignedDocument signedDocument, Type type, State state, Tag tag) {
        this.amount = amount;
        this.fromUser = fromUser;
        this.fromUserIBAN = fromUser.getIBAN();
        this.toUser = toUser;
        this.toUserIBAN = toUser.getIBAN();
        this.signedDocument = signedDocument;
        this.subject = subject;
        this.currencyCode = currencyCode;
        this.type = type;
        this.state = state;
        this.tag = tag;
    }

    public static Transaction CURRENCY_SEND(CurrencyBatch batch, User toUser, LocalDateTime validTo,
                                            SignedDocument signedDocument, Tag tag) {
        Transaction transaction = BASIC(toUser, Transaction.Type.CURRENCY_SEND, null, batch.getBatchAmount(),
                batch.getCurrencyCode(), batch.getSubject(), validTo, signedDocument, batch.getTag());
        transaction.setToUserIBAN(batch.getToUser().getIBAN());
        transaction.setCurrencyBatch(batch);
        transaction.setState(Transaction.State.OK);
        transaction.setSignedDocument(signedDocument);
        transaction.setTag(tag);
        transaction.setIsTimeLimited(batch.getTimeLimited());
        return transaction;
    }

    public static Transaction CURRENCY_CHANGE(CurrencyBatch batch, LocalDateTime validTo, SignedDocument signedDocument,
                                              Tag tag) {
        Transaction transaction = new Transaction();
        transaction.setType(Type.CURRENCY_CHANGE);
        transaction.setAmount(batch.getBatchAmount());
        transaction.setCurrencyCode(batch.getCurrencyCode());
        transaction.setSubject(batch.getSubject());
        transaction.setValidTo(validTo);
        transaction.setSignedDocument(signedDocument);
        transaction.setTag(tag);
        transaction.setCurrencyBatch(batch);
        transaction.setState(Transaction.State.OK);
        transaction.setIsTimeLimited(batch.getTimeLimited());
        return transaction;
    }

    public static Transaction USER(User user, User toUser, Type type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
           BigDecimal amount, CurrencyCode currencyCode, String subject, LocalDateTime validTo,
           SignedDocument signedDocument, Tag tag) {
        Transaction transaction = BASIC(toUser, type, accountFromMovements, amount, currencyCode, subject,
                validTo, signedDocument, tag);
        transaction.setFromUser(user);
        transaction.setFromUserIBAN(user.getIBAN());
        return transaction;
    }

    public static Transaction BASIC(User toUser, Type type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
            BigDecimal amount, CurrencyCode currencyCode, String subject, LocalDateTime validTo,
            SignedDocument signedDocument, Tag tag) {
        Transaction transaction = new Transaction();
        transaction.setToUser(toUser);
        transaction.setToUserIBAN(toUser.getIBAN());
        transaction.setType(type);
        transaction.setAccountFromMovements(accountFromMovements);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setSubject(subject);
        transaction.setValidTo(validTo);
        transaction.setSignedDocument(signedDocument);
        transaction.setTag(tag);
        transaction.setState(Transaction.State.OK);
        return transaction;
    }

    public static Transaction FROM_BANK(Bank bank, String bankClientIBAN, String bankClientName, User toUser,
            BigDecimal amount, CurrencyCode currencyCode, String subject, LocalDateTime validTo,
            SignedDocument signedDocument, Tag tag) {
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
        transaction.setSignedDocument(signedDocument);
        transaction.setTag(tag);
        transaction.setState(Transaction.State.OK);
        transaction.setType(Transaction.Type.FROM_BANK);
        return transaction;
    }

    public static Transaction CURRENCY_REQUEST(String subject, Map<CurrencyAccount, BigDecimal> accountFromMovements,
            CurrencyRequestDto requestDto, User fromUser) {
        Transaction transaction = new Transaction();
        transaction.setType(Transaction.Type.CURRENCY_REQUEST);
        transaction.setState(Transaction.State.OK);
        transaction.setAmount(requestDto.getTotalAmount());
        transaction.setCurrencyCode(requestDto.getCurrencyCode());
        transaction.setTag(requestDto.getTag());
        transaction.setSubject(subject);
        transaction.setSignedDocument(requestDto.getSignedDocument());
        transaction.setFromUser(fromUser);
        transaction.setAccountFromMovements(accountFromMovements);
        return transaction;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public Transaction setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
        return this;
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

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
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

    public Tag getTag() {
        return tag;
    }

    public Transaction setTag(Tag tag) {
        this.tag = tag;
        return this;
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
        result.signedDocument = transactionParent.signedDocument;
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


    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public Transaction setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
        return this;
    }

    @PrePersist
    public void prePersist() {
        if(this.validTo != null) isTimeLimited = Boolean.TRUE;
        else isTimeLimited = Boolean.FALSE;
    }

}