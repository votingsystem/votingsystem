package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.votingsystem.model.MessageSMIME;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="TicketVSBatchRequest")
public class TicketVSBatchRequest implements Serializable  {

    private static Logger log = Logger.getLogger(TicketVSBatchRequest.class);

    public static final long serialVersionUID = 1L;


    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;


    @OneToOne private MessageSMIME messageSMIME;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

}
