package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.springframework.format.annotation.NumberFormat;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="TransactionVS")
public class TransactionVS  implements Serializable {

    private static Logger log = Logger.getLogger(TransactionVS.class);

    public static final long serialVersionUID = 1L;

    public enum Type { USER_INPUT, USER_OUTPUT, SYSTEM_INPUT, SYSTEM_OUTPUT, USER_ALLOCATION;}

    public enum State { OK, REPEATED, CANCELLED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Column(name="subject") private String subject;

    @Column(name="currency") private String currency;

    @NumberFormat(style= NumberFormat.Style.CURRENCY) private BigDecimal amount = null;
    @OneToOne private MessageSMIME messageSMIME;

    @OneToOne private MessageSMIME cancellationSMIME;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionParent") private TransactionVS transactionParent;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fromUserVS") private UserVS fromUserVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;


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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

}
