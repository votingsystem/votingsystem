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
@Table(name="VicketServer")
@DiscriminatorValue("VicketServer")
public class VicketServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public VicketServer() {}

    public VicketServer(ActorVS actorVS) {
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
        return Type.VICKETS;
    }

    @Transient public String getDepositURL() {
        return getServerURL() + "/transaction/deposit";
    }

    public String getVicketRequestServiceURL() {
        return getServerURL() + "/model/request";
    }

    public String getVicketDepositServiceURL() {
        return getServerURL() + "/model/deposit";
    }

    @Transient public String getUserProceduresPageURL() {
        return getServerURL() + "/app/user?menu=user";
    }

    @Transient public String getAdminProceduresPageURL() {
        return getServerURL() + "/app/admin?menu=admin";
    }

}
