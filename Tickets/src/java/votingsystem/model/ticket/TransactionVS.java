package org.votingsystem.model.ticket;

import org.apache.log4j.Logger;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;

import javax.persistence.*;
import java.io.Serializable;
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

    public enum Type { USER_INPUT, USER_OUTPUT, SYSTEM_INPUT, SYSTEM_OUTPUT;}

    public enum State { OK, REPEATED, CANCELLED;}

    @Id @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Column(name="IBAN") private String IBAN;

    @OneToOne private MessageSMIME messageSMIME;

    @OneToOne private MessageSMIME cancellationSMIME;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fromUserVS") private UserVS fromUserVS;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;
}
