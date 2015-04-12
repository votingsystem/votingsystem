package org.votingsystem.web.accesscontrol.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.ActorVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.ControlCenterVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ControlCenterBean {

    private static final Logger log = Logger.getLogger(ControlCenterBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject SignatureBean signatureBean;
    private ControlCenterVS controlCenter;

    public void init() throws Exception {
        log.info("init");
        try {
            Query query = dao.getEM().createQuery("select c from ControlCenterVS c where c.state =:state")
                    .setParameter("state", ActorVS.State.OK);
            controlCenter = dao.getSingleResult(ControlCenterVS.class, query);
            if(controlCenter == null) checkControlCenter(config.getProperty("vs.controlCenterURL"));
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

    @Asynchronous
    public void checkControlCenter(String serverURL) throws Exception {
        try {
            log.info("checkControlCenter - serverURL:" + serverURL);
            CertificateVS controlCenterCert = null;
            serverURL = StringUtils.checkURL(serverURL);
            Query query = dao.getEM().createQuery("select c from ControlCenterVS c where c.serverURL =:serverURL")
                    .setParameter("serverURL", serverURL);
            ControlCenterVS controlCenterDB = dao.getSingleResult(ControlCenterVS.class, query);
            if(controlCenterDB != null) {
                query = dao.getEM().createQuery("select c from CertificateVS c where c.actorVS =:actorVS " +
                        "and c.state =:state").setParameter("actorVS", controlCenterDB)
                        .setParameter("state", CertificateVS.State.OK);
                controlCenterCert = dao.getSingleResult(CertificateVS.class, query);
                if(controlCenterCert != null) return ;
            }
            ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(serverURL), ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                ActorVS actorVS = ((ActorVSDto)responseVS.getDto(ActorVSDto.class)).getActorVS();
                if (ActorVS.Type.CONTROL_CENTER != actorVS.getType()) throw new ExceptionVS(
                        "ERROR - actorNotControlCenterMsg serverURL: " + serverURL);
                if(!actorVS.getServerURL().equals(serverURL)) throw new ExceptionVS(
                        "ERROR - serverURLMismatch expected URL: " + serverURL + " - found: " + actorVS.getServerURL());
                X509Certificate x509Cert = CertUtils.fromPEMToX509CertCollection(
                        actorVS.getCertChainPEM().getBytes()).iterator().next();
                signatureBean.verifyCertificate(x509Cert);
                if(controlCenterDB == null) {
                    controlCenterDB = dao.persist((ControlCenterVS) new ControlCenterVS(actorVS).setX509Certificate(
                            x509Cert).setState(ActorVS.State.OK));
                }
                controlCenterDB.setCertChainPEM(actorVS.getCertChainPEM());
                controlCenterCert = new CertificateVS(controlCenterDB, actorVS.getCertChainPEM().getBytes(),
                        x509Cert.getEncoded(), CertificateVS.State.OK, x509Cert.getSerialNumber().longValue(),
                        CertificateVS.Type.ACTOR_VS, x509Cert.getNotBefore(), x509Cert.getNotAfter());
                dao.persist(controlCenterCert);
                controlCenter = controlCenterDB;
            } else throw new ExceptionVS(responseVS.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}
