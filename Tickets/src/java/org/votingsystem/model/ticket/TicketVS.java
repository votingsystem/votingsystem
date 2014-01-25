package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.springframework.format.annotation.NumberFormat;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.MessageSMIME;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="TicketVS")
public class TicketVS implements Serializable  {

    private static Logger log = Logger.getLogger(TicketVS.class);

    public static final long serialVersionUID = 1L;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CurrencyVS getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyVS currency) {
        this.currency = currency;
    }

    public enum State { OK, REJECTED, CANCELLED, EXPENDED, LAPSED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;
    @NumberFormat(style= NumberFormat.Style.CURRENCY) private BigDecimal amount = null;
    @Column(name="currency", nullable=false) @Enumerated(EnumType.STRING) private CurrencyVS currency;

    @Column(name="hashCertVS") private String hashCertVS;
    @Column(name="originHashCertVS") private String originHashCertVS;
    @Column(name="ticketProviderURL") private String ticketProviderURL;
    @Column(name="reason") private String reason;

    @Column(name="serialNumber", unique=true, nullable=false) private Long serialNumber;
    @Column(name="content", nullable=false) @Lob private byte[] content;
    @Column(name="state", nullable=false) @Enumerated(EnumType.STRING) private State state;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="authorityCertificateVS") private CertificateVS authorityCertificateVS;

    @OneToOne private MessageSMIME cancelMessage;
    @OneToOne private MessageSMIME messageSMIME;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="validFrom", length=23) private Date validFrom;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="validTo", length=23) private Date validTo;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTicketProviderURL() {
        return ticketProviderURL;
    }

    public void setTicketProviderURL(String ticketProviderURL) {
        this.ticketProviderURL = ticketProviderURL;
    }

    public String getOriginHashCertVS() {
        return originHashCertVS;
    }

    public void setOriginHashCertVS(String originHashCertVS) {
        this.originHashCertVS = originHashCertVS;
    }

    public MessageSMIME getCancelMessage() {
        return cancelMessage;
    }

    public void setCancelMessage(MessageSMIME cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
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

    public CertificateVS getAuthorityCertificateVS() {
        return authorityCertificateVS;
    }

    public void setAuthorityCertificateVS(CertificateVS authorityCertificateVS) {
        this.authorityCertificateVS = authorityCertificateVS;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public static Map checkSubject(String subjectDN) {
        String currency = null;
        String amount = null;
        String ticketProviderURL = null;
        if (subjectDN.contains("CURRENCY:")) currency = subjectDN.split("CURRENCY:")[1].split(",")[0];
        if (subjectDN.contains("AMOUNT:")) amount = subjectDN.split("AMOUNT:")[1].split(",")[0];
        if (subjectDN.contains("ticketProviderURL:")) ticketProviderURL = subjectDN.split("ticketProviderURL:")[1].split(",")[0];
        Map resultMap = new HashMap();
        resultMap.put("currency", currency);
        resultMap.put("amount", amount);
        resultMap.put("ticketProviderURL", ticketProviderURL);
        return resultMap;
    }

}
