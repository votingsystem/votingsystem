package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.apache.solr.analysis.HTMLStripCharFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.hibernate.search.annotations.*;
import org.springframework.format.annotation.NumberFormat;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.ticket.service.TransactionVSService;
import org.votingsystem.util.ApplicationContextHolder;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Indexed
@Entity
@Table(name="TransactionVS")
@AnalyzerDef(name="transactionVSAnalyzer",
        charFilters = { @CharFilterDef(factory = HTMLStripCharFilterFactory.class) },
        tokenizer =  @TokenizerDef(factory = StandardTokenizerFactory.class))
public class TransactionVS  implements Serializable {

    private static Logger log = Logger.getLogger(TransactionVS.class);

    public static final long serialVersionUID = 1L;

    public enum Type { TICKET_REQUEST, USER_ALLOCATION, USER_ALLOCATION_INPUT, TICKET_SEND, TICKET_CANCELLATION;}

    public enum State { OK, REPEATED, CANCELLED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false)
    @DocumentId
    private Long id;

    @Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)
    @Column(name="subject") private String subject;

    @Field(index = Index.NO, analyze=Analyze.NO, store=Store.YES)
    @Column(name="currency", nullable=false) @Enumerated(EnumType.STRING) private CurrencyVS currency;

    @Field(index = Index.NO, analyze=Analyze.NO, store=Store.YES)
    @NumberFormat(style= NumberFormat.Style.CURRENCY) private BigDecimal amount = null;
    @OneToOne private MessageSMIME messageSMIME;

    @OneToOne private MessageSMIME cancellationSMIME;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="transactionParent") private TransactionVS transactionParent;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fromUserVS") private UserVS fromUserVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @Field(index = Index.YES, analyze=Analyze.YES, store=Store.YES)
    @Column(name="type", nullable=false) @Enumerated(EnumType.STRING) private Type type;

    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;
    @Field(index = Index.NO, analyze= Analyze.NO, store = Store.YES)
    @DateBridge(resolution = Resolution.HOUR)
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

    public CurrencyVS getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyVS currency) {
        this.currency = currency;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void afterInsert() {
        if(Type.USER_ALLOCATION_INPUT != getType()) {
            ((TransactionVSService)ApplicationContextHolder.getBean("transactionVSService")).notifyListeners(this);
        }
    }

}
