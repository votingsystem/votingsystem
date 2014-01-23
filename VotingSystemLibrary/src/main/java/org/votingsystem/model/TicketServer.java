package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
@Entity
@Table(name="TicketServer")
@DiscriminatorValue("TicketServer")
public class TicketServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public TicketServer() {}

    public TicketServer(ActorVS actorVS) {
        setName(actorVS.getName());
        setX509Certificate(actorVS.getX509Certificate());
        setControlCenters(actorVS.getControlCenters());
        setEnvironmentVS(actorVS.getEnvironmentVS());
        setServerURL(actorVS.getServerURL());
        setState(actorVS.getState());
        setId(actorVS.getId());
        setTimeStampCert(actorVS.getTimeStampCert());
        setTrustAnchors(actorVS.getTrustAnchors());
    }

    @Override public Type getType() {
        return Type.TICKETS;
    }

    @Transient public String getDepositURL() {
        return getServerURL() + "/transaction/deposit";
    }

    public String getTicketRequestServiceURL() {
        return getServerURL() + "/ticket/request";
    }

    public String getTicketDepositServiceURL() {
        return getServerURL() + "/ticket/deposit";
    }
}
