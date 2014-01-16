package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.springframework.format.annotation.NumberFormat;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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

    public enum State { OK, REJECTED, CANCELLED, EXPENDED, LAPSED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @NotNull
    @NumberFormat(style= NumberFormat.Style.CURRENCY) private BigDecimal amount = null;
    @Column(name="hashCertVS") private String hashCertVS;
    @Column(name="originHashCertVS") private String originHashCertVS;
    @Column(name="ticketProviderURL") private String ticketProviderURL;

    @OneToOne(mappedBy="ticketVS") private CertificateVS certificateVS;


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
}
