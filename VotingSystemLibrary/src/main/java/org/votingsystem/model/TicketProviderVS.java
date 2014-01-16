package org.votingsystem.model;

import org.apache.log4j.Logger;
import org.votingsystem.model.ActorVS;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="TicketProviderVS")
@DiscriminatorValue("TicketProviderVS")
public class TicketProviderVS extends ActorVS implements Serializable {

    private static Logger log = Logger.getLogger(TicketProviderVS.class);

    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=IDENTITY)
    @Column(name="id", unique=true, nullable=false) private Long id;

    @Temporal(TemporalType.TIMESTAMP) @Column(name="dateCreated", length=23) private Date dateCreated;
    @Temporal(TemporalType.TIMESTAMP) @Column(name="lastUpdated", length=23) private Date lastUpdated;

    public static final String TAG = "TicketProviderVS";

    public String getTicketRequestServiceURL() {
        return getServerURL() + "/ticket/request";
    }

    public String getTicketDepositServiceURL() {
        return getServerURL() + "/ticket/deposit";
    }

}
