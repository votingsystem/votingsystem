package org.votingsystem.model.currency;

import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.model.EntityBase;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.converter.LocalDateTimeAttributeConverter;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.logging.Logger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CURRENCY_BATCH")
public class CurrencyBatch extends EntityBase implements Serializable {

    private static Logger log = Logger.getLogger(CurrencyBatch.class.getName());

    public static final long serialVersionUID = 1L;

    public enum State { OK, ERROR;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="ID", unique=true, nullable=false)
    private Long id;

    @Column(name="STATE", nullable=false) @Enumerated(EnumType.STRING)
    private State state;

    @Column(name="REASON")
    private String reason;

    @Column(name="TYPE") @Enumerated(EnumType.STRING)
    private CurrencyOperation type;

    @Column(name="CONTENT")
    private byte[] content;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    private User user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="TO_USER")
    private User toUser;

    @Column(name="BATCH_AMOUNT")
    private BigDecimal batchAmount;

    @OneToOne @JoinColumn(name="LEFT_OVER")
    private Currency leftOver;

    @OneToOne @JoinColumn(name="CURRENCY_CHANGE")
    private Currency currencyChange;

    @Column(name="CURRENCY_CODE") @Enumerated(EnumType.STRING)
    private CurrencyCode currencyCode;

    @OneToOne @JoinColumn(name="SIGNED_DOCUMENT_ID")
    private SignedDocument signedDocument;

    @Column(name="BATCH_UUID")
    private String batchUUID;

    @Column(name="SUBJECT")
    private String subject;

    @Column(name="DATE_CREATED", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime dateCreated;

    @Column(name="LAST_UPDATE", columnDefinition="TIMESTAMP")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastUpdated;

    @Transient private Set<Currency> validatedCurrencySet;

    public CurrencyBatch() {}

    public CurrencyBatch(CurrencyBatchDto batchDto) {
        setType(batchDto.getOperation());
        setSubject(batchDto.getSubject());
        setBatchAmount(batchDto.getBatchAmount());
        setCurrencyCode(batchDto.getCurrencyCode());
        setContent(batchDto.getContent());
        setBatchUUID(batchDto.getBatchUUID());
    }

    public CurrencyBatch(byte[] content) throws IOException {
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public User getToUser() {
        return toUser;
    }

    public CurrencyBatch setToUser(User toUser) {
        this.toUser = toUser;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public Currency getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(Currency leftOver) {
        this.leftOver = leftOver;
    }

    public Currency getCurrencyChange() {
        return currencyChange;
    }

    public void setCurrencyChange(Currency currencyChange) {
        this.currencyChange = currencyChange;
    }

    public CurrencyOperation getType() {
        return type;
    }

    public void setType(CurrencyOperation type) {
        this.type = type;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public CurrencyBatch setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
        return this;
    }

    public State getState() {
        return state;
    }

    public CurrencyBatch setState(State state) {
        this.state = state;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public CurrencyBatch setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Set<Currency> getValidatedCurrencySet() {
        return validatedCurrencySet;
    }

    public void setValidatedCurrencySet(Set<Currency> validatedCurrencySet) {
        this.validatedCurrencySet = validatedCurrencySet;
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

}
