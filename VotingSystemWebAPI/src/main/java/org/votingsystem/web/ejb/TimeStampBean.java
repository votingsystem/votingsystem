package org.votingsystem.web.ejb;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TSPUtil;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.web.cdi.ConfigVS;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class TimeStampBean {

    private static Logger log = Logger.getLogger(TimeStampBean.class.getSimpleName());

    @Inject
    ConfigVS configVS;
    @Inject DAOBean dao;

    private SignerInformationVerifier timeStampSignerInfoVerifier;
    private byte[] signingCertPEMBytes;

    public TimeStampBean() { }

    public void init() {
        log.info("TimeStampBean");
        try {
            String serverURL = StringUtils.checkURL(configVS.getTimeStampServerURL());
            Query query = dao.getEM().createNamedQuery("findActorVSByServerURL").setParameter("serverURL", serverURL);
            ActorVS timeStampServer = dao.getSingleResult(ActorVS.class, query);
            X509Certificate x509TimeStampServerCert = null;
            CertificateVS timeStampServerCert = null;
            if(timeStampServer == null) {
                fetchTimeStampServerInfo(new ActorVS(serverURL));
                return;
            } else {
                query = dao.getEM().createNamedQuery("findCertByActorVSAndStateAndType").setParameter("actorVS", timeStampServer)
                        .setParameter("state", CertificateVS.State.OK).setParameter("type", CertificateVS.Type.TIMESTAMP_SERVER);
                timeStampServerCert = dao.getSingleResult(CertificateVS.class, query);
                if(timeStampServerCert != null) {
                    x509TimeStampServerCert = CertUtils.loadCertificate(timeStampServerCert.getContent());
                    if(new Date().before(x509TimeStampServerCert.getNotAfter())) {
                        signingCertPEMBytes = CertUtils.getPEMEncoded(x509TimeStampServerCert);
                    } else {
                        log.info("timeStampServerCert lapsed - not valid after:" + x509TimeStampServerCert.getNotAfter());
                        dao.getEM().merge(timeStampServerCert.setState(CertificateVS.State.LAPSED));
                    }
                }
                if(signingCertPEMBytes == null) {
                    fetchTimeStampServerInfo(timeStampServer);
                    return;
                }
            }
            if(x509TimeStampServerCert != null) {
                configVS.setX509TimeStampServerCert(x509TimeStampServerCert);
                timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                        ContextVS.PROVIDER).build(x509TimeStampServerCert);
                X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
                TSPUtil.validateCertificate(certHolder);
            } else throw new Exception("TimeStamp signing cert not found - serverURL: " + serverURL);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void updateTimeStampServer(ActorVS timeStampServer) throws Exception {
        log.info("updateTimeStampServer");
        if(timeStampServer.getId() == null) {
            dao.persist(timeStampServer);
        }
        X509Certificate x509TimeStampServerCert = CertUtils.fromPEMToX509CertCollection(
                timeStampServer.getCertChainPEM().getBytes()).iterator().next();
        if(new Date().after(x509TimeStampServerCert.getNotAfter())) {
            throw new ExceptionVS(timeStampServer.getServerURL() + " - signing cert is lapsed");
        }
        log.info("updated TimeStampServer '${timeStampServer.id}' signing cert");
        CertificateVS certificateVS = dao.persist(new CertificateVS(timeStampServer, timeStampServer.getCertChainPEM().getBytes(),
                x509TimeStampServerCert.getEncoded(), CertificateVS.State.OK, x509TimeStampServerCert.getSerialNumber().longValue(),
                CertificateVS.Type.TIMESTAMP_SERVER, x509TimeStampServerCert.getNotBefore(), x509TimeStampServerCert.getNotAfter()));
        log.info("Added TimeStampServer Cert: " + certificateVS.getId());
        signingCertPEMBytes = CertUtils.getPEMEncoded(x509TimeStampServerCert);
    }

    private void fetchTimeStampServerInfo(final ActorVS timeStampServer) {
        log.info("fetchTimeStampServerInfo");
        new Thread(() -> {
            ResponseVS responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                            timeStampServer.getServerURL()), ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    Map dataMap = new ObjectMapper().readValue(responseVS.getMessage(),
                            new TypeReference<HashMap<String,String>>(){});
                    ActorVS serverActorVS = ActorVS.parse(dataMap);
                    if(timeStampServer.getServerURL().equals(serverActorVS.getServerURL())) {
                        if(timeStampServer.getId() != null) {
                            timeStampServer.setCertChainPEM(serverActorVS.getCertChainPEM());
                            updateTimeStampServer(timeStampServer);
                        } else updateTimeStampServer(serverActorVS);
                    } else log.log(Level.SEVERE, "Expected server URL:" + timeStampServer.getServerURL()  + " - found " +
                            serverActorVS.getServerURL());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            } else log.log(Level.SEVERE, "ERROR fetching TimeStampServer data - serverURL: " +
                    timeStampServer.getServerURL());
        }).start();
    }

    public byte[] getSigningCertPEMBytes() {
        return signingCertPEMBytes;
    }

    public void validateToken(TimeStampToken tsToken) throws ExceptionVS, TSPException {
        if(tsToken == null) throw new ExceptionVS("documentWithoutTimeStampErrorMsg");
        if(timeStampSignerInfoVerifier == null) throw new ExceptionVS("TimeStamp service not initialized");
        X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
        tsToken.validate(timeStampSignerInfoVerifier);
    }

    public SignerInformationVerifier getTimeStampSignerInfoVerifier(){
        return timeStampSignerInfoVerifier;
    }


}
