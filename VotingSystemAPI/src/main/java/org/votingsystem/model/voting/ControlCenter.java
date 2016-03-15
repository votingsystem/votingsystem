package org.votingsystem.model.voting;

import org.votingsystem.model.Actor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("ControlCenter")
public class ControlCenter extends Actor implements Serializable {

    public static final long serialVersionUID = 1L;

    public ControlCenter() {}

    public ControlCenter(Actor actor) throws Exception {
        setName(actor.getName());
        setX509Certificate(actor.getX509Certificate());
        setControlCenter(actor.getControlCenter());
        setEnvironmentVS(actor.getEnvironmentVS());
        setServerURL(actor.getServerURL());
        setState(actor.getState());
        setId(actor.getId());
        setTimeStampCert(actor.getTimeStampCert());
        setTrustAnchors(actor.getTrustAnchors());
    }

    @Override public Type getType() {
        return Type.CONTROL_CENTER;
    }

    public String getVoteServiceURL() {
        return getServerURL() + "/rest/vote";
    }

}
