package org.votingsystem.web.ejb;

import org.votingsystem.dto.MessageDto;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CertificateVSBean {

    private static final Logger log = Logger.getLogger(CertificateVSBean.class.getName());

    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject ConfigVS config;

    /*
     * Método para poder añadir certificados de confianza.
     * El procedimiento para añadir una autoridad certificadora consiste en
     * añadir el certificado en formato pem en el directorio ./WEB-INF/votingsystem
     */
    public MessageDto addCertificateAuthority(MessageSMIME messageSMIME) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CertificateVSRequest request = messageSMIME.getSignedContent(CertificateVSRequest.class);
        request.validateNewCARequest();
        if(!signatureBean.isAdmin(request.signer.getNif())) throw new ValidationExceptionVS(
                "userWithoutPrivilegesErrorMsg - operation: " + TypeVS.CERT_CA_NEW.toString() + " - user: " +
                request.signer.getId());
        Collection<X509Certificate> certX509CertCollection = CertUtils.fromPEMToX509CertCollection(
                request.certChainPEM.getBytes());
        if(certX509CertCollection.isEmpty()) throw new ValidationExceptionVS(
                "operation: CERT_CA_NEW - nullCertificateErrorMsg");
        X509Certificate x509NewCACert = certX509CertCollection.iterator().next();
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber").setParameter("serialNumber",
                x509NewCACert.getSerialNumber().longValue());
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null) {
            certificateVS = dao.persist(CertificateVS.AUTHORITY(x509NewCACert, request.info));
        }
        log.info("addCertificateAuthority - new CA - id:" + certificateVS.getId());
        signatureBean.addCertAuthority(certificateVS);
        return new MessageDto(ResponseVS.SC_OK,  messages.get("certUpdatedToCAMsg", x509NewCACert.getSerialNumber().toString()),
                config.getContextURL() + "/rest/certificateVS/serialNumber/" + x509NewCACert.getSerialNumber().toString());
    }

    public X509Certificate getVoteCert(byte[] pemCertCollection) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(pemCertCollection);
        for (X509Certificate certificate : certificates) {
            if (certificate.getSubjectDN().toString().contains("SERIALNUMBER=hashCertVoteHex:")) {
                return certificate;
            }
        }
        return null;
    }

    private class CertificateVSRequest {
        String certChainPEM, info, description;
        CertificateVS.State changeCertToState;
        TypeVS operation;
        UserVS signer;
        Long serialNumber;

        public CertificateVSRequest() { }

        public CertificateVSRequest validateNewCARequest() throws ValidationExceptionVS {
            if(TypeVS.CERT_CA_NEW != operation) throw new ValidationExceptionVS(
                    "operation expected: 'CERT_CA_NEW' - operation found: " + operation.toString());
            if(info == null) throw new ValidationExceptionVS("missing param 'info'");
            if(certChainPEM == null) throw new ValidationExceptionVS("missing param 'certChainPEM'");
            return this;
        }

        public CertificateVSRequest validatEditCertRequest() throws ValidationExceptionVS {
            if(TypeVS.CERT_EDIT != operation) throw new ValidationExceptionVS(
                    "operation expected: 'CERT_CA_NEW' - operation found: " + operation.toString());
            if(changeCertToState == null) throw new ValidationExceptionVS("missing param 'changeCertToState'");
            if(serialNumber == null) throw new ValidationExceptionVS("missing param 'serialNumber'");

            return this;
        }
    }

    public CertificateVS editCert(MessageSMIME messageSMIME) throws Exception {
        CertificateVSRequest request = messageSMIME.getSignedContent(CertificateVSRequest.class);
        request.validatEditCertRequest();
        if(!signatureBean.isAdmin(request.signer.getNif())) throw new ValidationExceptionVS(
                "userWithoutPrivilegesErrorMsg - operation: " + TypeVS.CERT_EDIT.toString() + " - user: " +
                        request.signer.getId());
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber").setParameter("serialNumber", request.serialNumber);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null || CertificateVS.State.OK != certificateVS.getState()) throw new ValidationExceptionVS(
                "activeCertificateNotFoundErrorMsg - serialNumber: " + request.serialNumber);
        certificateVS.updateDescription(request.description);
        dao.merge(certificateVS.setMessageSMIME(messageSMIME).setState(request.changeCertToState));
        return certificateVS;
    }
    
    private void cancelCert(long serialNumberCert) {
        log.info ("cancelCert - serialNumberCert:" + serialNumberCert);
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber").setParameter("serialNumber", serialNumberCert);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS != null) {
            log.info ("cancelCert - certificateVS.id: " + certificateVS.getId());
            if(CertificateVS.State.OK == certificateVS.getState()) {
                certificateVS.setCancelDate(new Date());
                dao.merge(certificateVS.setState(CertificateVS.State.CANCELED));
                log.info("certificateVS.id:" + certificateVS.getId() + " cancelled");
            } else log.info("certificateVS.id: " + certificateVS.getId() + " was already cancelled");
        } else log.info("not found certificateVS with serialNumber: " + serialNumberCert);
    }

}
