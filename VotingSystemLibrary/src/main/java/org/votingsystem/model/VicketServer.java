package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import javax.xml.bind.DatatypeConverter;
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
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

    public String getTransactionVSServiceURL() {
        return getServerURL() + "/transactionVS";
    }

    public String getVicketRequestServiceURL() {
        return getServerURL() + "/vicket/request";
    }

    public String getVicketStatusServiceURL(String hashCertVS) {
        return getServerURL() + "/vicket/status/" +DatatypeConverter.printHexBinary(hashCertVS.getBytes());
    }

    public String getVicketTransactionServiceURL() {
        return getServerURL() + "/transactionVS/vicket";
    }

    public String getUserProceduresPageURL() {
        return getServerURL() + "/app/user?menu=user";
    }

    public String getAdminProceduresPageURL() {
        return getServerURL() + "/app/admin?menu=admin";
    }

    public String getNewBankServiceURL() {
        return getServerURL() + "/userVS/newBankVS";
    }

    public String getSubscribeUserToGroupURL(Long groupId) {
        return getServerURL() + "/groupVS/" + String.valueOf(groupId) + "/subscribe";
    }

}
