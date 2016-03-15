package org.votingsystem.web.ejb;

import org.votingsystem.dto.MessageDto;
import org.votingsystem.model.CMSMessage;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.PEMUtils;
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
public class CertificateBean {

    private static final Logger log = Logger.getLogger(CertificateBean.class.getName());

    @Inject DAOBean dao;
    @Inject CMSBean cmsBean;
    @Inject ConfigVS config;

    /*
     * Método para poder añadir certificados de confianza.
     * El procedimiento para añadir una autoridad certificadora consiste en
     * añadir el certificado en formato pem en el directorio ./WEB-INF/votingsystem
     */
    public MessageDto addCertificateAuthority(CMSMessage cmsMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CertificateRequest request = cmsMessage.getSignedContent(CertificateRequest.class);
        request.validateNewCARequest();
        if(!cmsBean.isAdmin(request.signer.getNif())) throw new ValidationException(
                "userWithoutPrivilegesErrorMsg - operation: " + TypeVS.CERT_CA_NEW.toString() + " - user: " +
                request.signer.getId());
        Collection<X509Certificate> certX509CertCollection = PEMUtils.fromPEMToX509CertCollection(
                request.certChainPEM.getBytes());
        if(certX509CertCollection.isEmpty()) throw new ValidationException(
                "operation: CERT_CA_NEW - nullCertificateErrorMsg");
        X509Certificate x509NewCACert = certX509CertCollection.iterator().next();
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber").setParameter("serialNumber",
                x509NewCACert.getSerialNumber().longValue());
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if(certificate == null) {
            certificate = dao.persist(Certificate.AUTHORITY(x509NewCACert, request.info));
        }
        log.info("addCertificateAuthority - new CA - id:" + certificate.getId());
        cmsBean.addCertAuthority(certificate);
        return new MessageDto(ResponseVS.SC_OK,  messages.get("certUpdatedToCAMsg", x509NewCACert.getSerialNumber().toString()),
                config.getContextURL() + "/rest/certificate/serialNumber/" + x509NewCACert.getSerialNumber().toString());
    }

    public X509Certificate getVoteCert(byte[] pemCertCollection) throws Exception {
        Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(pemCertCollection);
        for (X509Certificate certificate : certificates) {
            if (certificate.getSubjectDN().toString().contains("SERIALNUMBER=hashCertVoteHex:")) {
                return certificate;
            }
        }
        return null;
    }

    private class CertificateRequest {
        String certChainPEM, info, description;
        Certificate.State changeCertToState;
        TypeVS operation;
        User signer;
        Long serialNumber;

        public CertificateRequest() { }

        public CertificateRequest validateNewCARequest() throws ValidationException {
            if(TypeVS.CERT_CA_NEW != operation) throw new ValidationException(
                    "operation expected: 'CERT_CA_NEW' - operation found: " + operation.toString());
            if(info == null) throw new ValidationException("missing param 'info'");
            if(certChainPEM == null) throw new ValidationException("missing param 'certChainPEM'");
            return this;
        }

        public CertificateRequest validatEditCertRequest() throws ValidationException {
            if(TypeVS.CERT_EDIT != operation) throw new ValidationException(
                    "operation expected: 'CERT_CA_NEW' - operation found: " + operation.toString());
            if(changeCertToState == null) throw new ValidationException("missing param 'changeCertToState'");
            if(serialNumber == null) throw new ValidationException("missing param 'serialNumber'");

            return this;
        }
    }

    public Certificate editCert(CMSMessage cmsMessage) throws Exception {
        CertificateRequest request = cmsMessage.getSignedContent(CertificateRequest.class);
        request.validatEditCertRequest();
        if(!cmsBean.isAdmin(request.signer.getNif())) throw new ValidationException(
                "userWithoutPrivilegesErrorMsg - operation: " + TypeVS.CERT_EDIT.toString() + " - user: " +
                        request.signer.getId());
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber").setParameter("serialNumber", request.serialNumber);
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if(certificate == null || Certificate.State.OK != certificate.getState()) throw new ValidationException(
                "activeCertificateNotFoundErrorMsg - serialNumber: " + request.serialNumber);
        certificate.updateDescription(request.description);
        dao.merge(certificate.setCmsMessage(cmsMessage).setState(request.changeCertToState));
        return certificate;
    }
    
    private void cancelCert(long serialNumberCert) {
        log.info ("cancelCert - serialNumberCert:" + serialNumberCert);
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber").setParameter("serialNumber", serialNumberCert);
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if(certificate != null) {
            log.info ("cancelCert - certificate.id: " + certificate.getId());
            if(Certificate.State.OK == certificate.getState()) {
                certificate.setCancelDate(new Date());
                dao.merge(certificate.setState(Certificate.State.CANCELED));
                log.info("certificate.id:" + certificate.getId() + " cancelled");
            } else log.info("certificate.id: " + certificate.getId() + " was already cancelled");
        } else log.info("not found certificate with serialNumber: " + serialNumberCert);
    }

}
