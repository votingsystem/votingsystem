package org.votingsystem.model.currency;

import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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
        @NamedQuery(name=Transaction.FIND_TRANS_BY_TYPE_LIST, query=
                "SELECT t FROM Transaction t WHERE t.dateCreated between :dateFrom and :dateTo and t.type in :typeList and t.state=:state"),
        @NamedQuery(name=Transaction.FIND_TRANS_BY_FROM_USER_AND_IN_LIST, query=
                "SELECT t FROM Transaction t WHERE t.fromUser =:fromUser and t.state =:state " +
                        "and t.dateCreated between :dateFrom and :dateTo and t.type in :inList"),
        @NamedQuery(name=Transaction.FIND_TRANS_BY_TO_USER_AND_STATE_AND_DATE_CREATED_BETWEEN, query= "SELECT t FROM Transaction t " +
                "WHERE t.toUser =:toUser and t.state =:state and t.dateCreated between :dateFrom and :dateTo"),
        @NamedQuery(name=Transaction.FIND_TRANS_BY_FROM_USER_AND_STATE,
                query= "SELECT t FROM Transaction t WHERE t.fromUser =:fromUser and t.dateCreated between :dateFrom and :dateTo and t.state=:state"),
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
    public static final String FIND_TRANS_BY_TYPE_LIST = "Transaction.findSystemTransactionByTypeList";
    public static final String FIND_TRANS_BY_FROM_USER_AND_IN_LIST = "Transaction.findTransByFromUserAndInList";
    public static final String FIND_TRANS_BY_TO_USER_AND_STATE_AND_DATE_CREATED_BETWEEN =
            "Transaction.findTransByToUserAndStateAndDateCreatedBetween";
    public static final String FIND_TRANS_BY_FROM_USER_AND_STATE =
            "Transaction.findTransByFromUserAndStateAndDateCreatedBetween";
    public static final String FIND_TRANS_BY_TO_USER_AND_DATE_CREATED_BETWEEN =
            "Transaction.findTransByToUserAndDateCreatedBetween";


    public enum Source {FROM, TO}

    public enum State { OK, REPEATED, CANCELED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Column(name="SUBJECT") private String subject;

    @Column(name="CURRENCY", nullable=false) @Enumerated(EnumType.STRING)
    private CurrencyCode currencyCode;

    @Column(name="AMOUNT", nullable=false)
    private BigDecimal amount = null;

    @OneToOne @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="FROM_USER")
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

    @Column(name="TYPE", nullable=false) @Enumerated(EnumType.STRING)
    private CurrencyOperation type;

    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING)
    private State state;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    @Transient private Map<CurrencyAccount, BigDecimal> accountFromMovements;


    public Transaction() {}

    public Transaction(User fromUser, User toUser, BigDecimal amount, CurrencyCode currencyCode, String subject,
                       SignedDocument signedDocument, CurrencyOperation type, State state) {
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
    }

    public static Transaction CURRENCY_SEND(CurrencyBatch batch, User toUser, SignedDocument signedDocument) {
        Transaction transaction = BASIC(toUser, CurrencyOperation.CURRENCY_SEND, null, batch.getBatchAmount(),
                batch.getCurrencyCode(), batch.getSubject(), signedDocument);
        transaction.setToUserIBAN(batch.getToUser().getIBAN());
        transaction.setCurrencyBatch(batch);
        transaction.setState(Transaction.State.OK);
        transaction.setSignedDocument(signedDocument);
        return transaction;
    }

    public static Transaction CURRENCY_CHANGE(CurrencyBatch batch, SignedDocument signedDocument) {
        Transaction transaction = new Transaction();
        transaction.setAmount(batch.getBatchAmount());
        transaction.setCurrencyCode(batch.getCurrencyCode());
        transaction.setSubject(batch.getSubject());
        transaction.setSignedDocument(signedDocument);
        transaction.setCurrencyBatch(batch);
        transaction.setState(Transaction.State.OK);
        return transaction;
    }

    public static Transaction USER(User user, User toUser, CurrencyOperation type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
           BigDecimal amount, CurrencyCode currencyCode, String subject, SignedDocument signedDocument) {
        Transaction transaction = BASIC(toUser, type, accountFromMovements, amount, currencyCode, subject, signedDocument);
        transaction.setFromUser(user);
        transaction.setFromUserIBAN(user.getIBAN());
        return transaction;
    }

    public static Transaction BASIC(User toUser, CurrencyOperation type, Map<CurrencyAccount, BigDecimal> accountFromMovements,
            BigDecimal amount, CurrencyCode currencyCode, String subject, SignedDocument signedDocument) {
        Transaction transaction = new Transaction();
        transaction.setToUser(toUser);
        transaction.setToUserIBAN(toUser.getIBAN());
        transaction.setType(type);
        transaction.setAccountFromMovements(accountFromMovements);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setSubject(subject);
        transaction.setSignedDocument(signedDocument);
        transaction.setState(Transaction.State.OK);
        return transaction;
    }

    public static Transaction FROM_BANK(Bank bank, String bankClientIBAN, String bankClientName, User toUser,
            BigDecimal amount, CurrencyCode currencyCode, String subject, SignedDocument signedDocument) {
        Transaction transaction = new Transaction();
        transaction.setFromUser(bank);
        transaction.setFromUserIBAN(bankClientIBAN);
        transaction.setFromUserName(bankClientName);
        transaction.setToUser(toUser);
        transaction.setToUserIBAN(toUser.getIBAN());
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setSubject(subject);
        transaction.setSignedDocument(signedDocument);
        transaction.setState(Transaction.State.OK);
        transaction.setType(CurrencyOperation.TRANSACTION_FROM_BANK);
        return transaction;
    }

    public static Transaction CURRENCY_REQUEST(String subject, Map<CurrencyAccount, BigDecimal> accountFromMovements,
            CurrencyRequestDto requestDto, User fromUser) {
        Transaction transaction = new Transaction();
        transaction.setType(CurrencyOperation.CURRENCY_REQUEST);
        transaction.setState(Transaction.State.OK);
        transaction.setAmount(requestDto.getTotalAmount());
        transaction.setCurrencyCode(requestDto.getCurrencyCode());
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


    public CurrencyOperation getType() {
        return type;
    }

    public void setType(CurrencyOperation type) {
        this.type = type;
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

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUser) {
        this.fromUserName = fromUser;
    }

    public CurrencyBatch getCurrencyBatch() {
        return currencyBatch;
    }

    public Transaction setCurrencyBatch(CurrencyBatch currencyBatch) {
        this.currencyBatch = currencyBatch;
        return this;
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

}