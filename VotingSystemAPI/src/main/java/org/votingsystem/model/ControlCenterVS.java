package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="ControlCenterVS")
@DiscriminatorValue("ControlCenterVS")
public class ControlCenterVS extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public ControlCenterVS() {}

    public ControlCenterVS (ActorVS actorVS) throws Exception {
        setName(actorVS.getName());
        setX509Certificate(actorVS.getX509Certificate());
        setControlCenter(actorVS.getControlCenter());
        setEnvironmentVS(actorVS.getEnvironmentVS());
        setServerURL(actorVS.getServerURL());
        setState(actorVS.getState());
        setId(actorVS.getId());
        setTimeStampCert(actorVS.getTimeStampCert());
        setTrustAnchors(actorVS.getTrustAnchors());
    }

    @Override public Type getType() {
        return Type.CONTROL_CENTER;
    }

    public String getVoteServiceURL() {
        return getServerURL() + "/rest/voteVS";
    }

}
