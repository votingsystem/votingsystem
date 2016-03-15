package org.votingsystem.web.ejb;


import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TSPUtil;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.dto.ActorDto;
import org.votingsystem.model.Actor;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.util.ConfigVS;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class TimeStampBean {

    private static Logger log = Logger.getLogger(TimeStampBean.class.getName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    /* Executor service for asynchronous processing */
    @Resource(name="comp/DefaultManagedExecutorService")
    private ManagedExecutorService executorService;

    private SignerInformationVerifier timeStampSignerInfoVerifier;
    private byte[] signingCertPEMBytes;
    private X509Certificate x509TimeStampServerCert;
    private String timeStampServiceURL;

    public TimeStampBean() { }

    public void init() {
        log.info("TimeStampBean - init");
        try {
            String serverURL = StringUtils.checkURL(config.getTimeStampServerURL());
            timeStampServiceURL = serverURL + "/timestamp";
            Query query = dao.getEM().createNamedQuery("findActorByServerURL").setParameter("serverURL", serverURL);
            Actor timeStampServer = dao.getSingleResult(Actor.class, query);
            CertificateVS timeStampServerCert = null;
            if(timeStampServer == null) {
                fetchTimeStampServerInfo(new Actor(serverURL));
                return;
            } else {
                query = dao.getEM().createNamedQuery("findCertByActorAndStateAndType").setParameter("actor", timeStampServer)
                        .setParameter("state", CertificateVS.State.OK).setParameter("type", CertificateVS.Type.TIMESTAMP_SERVER);
                timeStampServerCert = dao.getSingleResult(CertificateVS.class, query);
                if(timeStampServerCert != null) {
                    x509TimeStampServerCert = CertUtils.loadCertificate(timeStampServerCert.getContent());
                    if(new Date().before(x509TimeStampServerCert.getNotAfter())) {
                        signingCertPEMBytes = PEMUtils.getPEMEncoded(x509TimeStampServerCert);
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
                timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                        ContextVS.PROVIDER).build(x509TimeStampServerCert);
                X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
                TSPUtil.validateCertificate(certHolder);
            } else throw new Exception("TimeStamp signing cert not found - serverURL: " + serverURL);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void updateTimeStampServer(Actor timeStampServer) throws Exception {
        log.info("updateTimeStampServer");
        if(timeStampServer.getId() == null) {
            dao.persist(timeStampServer);
        }
        X509Certificate x509TimeStampServerCert = PEMUtils.fromPEMToX509CertCollection(
                timeStampServer.getCertChainPEM().getBytes()).iterator().next();
        if(new Date().after(x509TimeStampServerCert.getNotAfter())) {
            throw new ExceptionVS(timeStampServer.getServerURL() + " - signing cert is lapsed");
        }
        CertificateVS certificateVS = CertificateVS.ACTOR(timeStampServer, x509TimeStampServerCert);
        certificateVS.setType(CertificateVS.Type.TIMESTAMP_SERVER);
        certificateVS.setCertChainPEM(timeStampServer.getCertChainPEM().getBytes());
        dao.persist(certificateVS);
        log.info("updateTimeStampServer - new CertificateVS - id: " + certificateVS.getId());
        signingCertPEMBytes = PEMUtils.getPEMEncoded(x509TimeStampServerCert);
        timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                ContextVS.PROVIDER).build(x509TimeStampServerCert);
        X509CertificateHolder certHolder = timeStampSignerInfoVerifier.getAssociatedCertificate();
        TSPUtil.validateCertificate(certHolder);
        this.x509TimeStampServerCert = x509TimeStampServerCert;
    }


    private void fetchTimeStampServerInfo(final Actor timeStampServer) {
        log.info("fetchTimeStampServerInfo");
        executorService.submit(() -> {
            ResponseVS responseVS = HttpHelper.getInstance().getData(Actor.getServerInfoURL(
                    timeStampServer.getServerURL()), ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                try {
                    Actor serverActor = ((ActorDto)responseVS.getMessage(ActorDto.class)).getActor();
                    if(timeStampServer.getServerURL().equals(serverActor.getServerURL())) {
                        if(timeStampServer.getId() != null) {
                            timeStampServer.setCertChainPEM(serverActor.getCertChainPEM());
                            updateTimeStampServer(timeStampServer);
                        } else updateTimeStampServer(serverActor);
                        return;
                    } else log.log(Level.SEVERE, "Expected server URL:" + timeStampServer.getServerURL()  +
                            " - found " + serverActor.getServerURL());
                } catch (Exception ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            log.log(Level.SEVERE, "ERROR fetching TimeStampServer data - serverURL: " +
                    timeStampServer.getServerURL());
        });
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
