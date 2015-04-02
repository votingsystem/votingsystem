package org.votingsystem.web.currency.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CertificateVSBean {

    private static final Logger log = Logger.getLogger(CertificateVSBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject SignatureBean signatureBean;
    @Inject SystemBean systemBean;
    @Inject ConfigVS config;
    @Inject MessagesBean messages;

    /*
     * Método para poder añadir certificados de confianza.
     * El procedimiento para añadir una autoridad certificadora consiste en
     * añadir el certificado en formato pem en el directorio ./WEB-INF/votingsystem
     */
    public Map addCertificateAuthority(MessageSMIME messageSMIMEReq) throws Exception {
        CertificateVSRequest request = new CertificateVSRequest(messageSMIMEReq).newCARequest();
        if(!signatureBean.isUserAdmin(request.signer.getNif())) throw new ValidationExceptionVS(
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
            certificateVS = new CertificateVS(CertUtils.isSelfSigned(x509NewCACert), CertificateVS.Type.CERTIFICATE_AUTHORITY,
                    CertificateVS.State.OK, request.info, x509NewCACert.getEncoded(), x509NewCACert.getSerialNumber().longValue(),
                    x509NewCACert.getNotBefore(), x509NewCACert.getNotAfter());
            dao.persist(certificateVS);
        } else {
            if(certificateVS.getType() != CertificateVS.Type.CERTIFICATE_AUTHORITY) {
                certificateVS.setType(CertificateVS.Type.CERTIFICATE_AUTHORITY);
                certificateVS.setDescription(certificateVS.getDescription() + " ### " + request.info);
                dao.merge(certificateVS);
            } else throw new ValidationExceptionVS("newCACertRepeatedErrorMsg - serialNumber:" +
                        x509NewCACert.getSerialNumber().toString());

        }
        log.info("addCertificateAuthority - new CA - id:" + certificateVS.getId());
        signatureBean.addCertAuthority(certificateVS);
        Map result = new HashMap<>();
        result.put("statusCode", ResponseVS.SC_OK);
        result.put("message", messages.get("certUpdatedToCAMsg", x509NewCACert.getSerialNumber().toString()));
        result.put("certURL", config.getContextURL() + "/certificateVS/" + x509NewCACert.getSerialNumber().toString());
        return result;
    }

    private class CertificateVSRequest {
        String certChainPEM, info, description;
        CertificateVS.State certState;
        TypeVS operation;
        UserVS signer;
        JsonNode dataJSON;
        Long serialNumber;

        public CertificateVSRequest() { }

        public CertificateVSRequest(MessageSMIME messageSMIME) throws Exception {
            signer = messageSMIME.getUserVS();
            dataJSON = new ObjectMapper().readTree(messageSMIME.getSMIME().getSignedContent());
            if (dataJSON.get("operation") == null) throw new ValidationExceptionVS("missing param 'operation'");
            operation = TypeVS.valueOf(dataJSON.get("operation").asText());
        }

        public CertificateVSRequest newCARequest() throws ValidationExceptionVS {
            if(TypeVS.CERT_CA_NEW != operation) throw new ValidationExceptionVS(
                    "operation expected: 'CERT_CA_NEW' - operation found: " + operation.toString());
            if(dataJSON.get("info") == null ) throw new ValidationExceptionVS("missing param 'info'");
            info = dataJSON.get("info").asText();
            if(dataJSON.get("certChainPEM") == null ) throw new ValidationExceptionVS("missing param 'certChainPEM'");
            certChainPEM = dataJSON.get("certChainPEM").asText();
            return this;
        }

        public CertificateVSRequest editCertRequest() throws ValidationExceptionVS {
            if(TypeVS.CERT_EDIT != operation) throw new ValidationExceptionVS(
                    "operation expected: 'CERT_CA_NEW' - operation found: " + operation.toString());
            if(dataJSON.get("changeCertToState") == null ) throw new ValidationExceptionVS("missing param 'changeCertToState'");
            certState = CertificateVS.State.valueOf(dataJSON.get("changeCertToState").asText());
            if(dataJSON.get("serialNumber") == null ) throw new ValidationExceptionVS("missing param 'serialNumber'");
            serialNumber = dataJSON.get("serialNumber").asLong();
            if(dataJSON.get("description") != null ) this.description = dataJSON.get("description").asText();
            return this;
        }
    }

    public CertificateVS editCert(MessageSMIME messageSMIMEReq) throws Exception {
        CertificateVSRequest request = new CertificateVSRequest(messageSMIMEReq).editCertRequest();
        if(!signatureBean.isUserAdmin(request.signer.getNif())) throw new ValidationExceptionVS(
                "userWithoutPrivilegesErrorMsg - operation: " + TypeVS.CERT_EDIT.toString() + " - user: " +
                        request.signer.getId());
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber").setParameter("serialNumber", request.serialNumber);
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null || CertificateVS.State.OK != certificateVS.getState()) throw new ValidationExceptionVS(
                "activeCertificateNotFoundErrorMsg - serialNumber: " + request.serialNumber);
        certificateVS.updateDescription(request.description);
        dao.merge(certificateVS.setMessageSMIME(messageSMIMEReq).setState(request.certState));
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

    public Map getCertificateVSDataMap(CertificateVS certificate) throws Exception {
        X509Certificate x509Cert = CertUtils.loadCertificate(certificate.getContent());
        Map result = new HashMap<>();
        //SerialNumber as String to avoid Javascript problem handling such big numbers
        result.put("serialNumber", x509Cert.getSerialNumber().toString());
        result.put("isRoot", CertUtils.isSelfSigned(x509Cert));
        result.put("description", certificate.getDescription());
        result.put("pemCert", new String(CertUtils.getPEMEncoded (x509Cert), "UTF-8"));
        result.put("type", certificate.getType().toString());
        result.put("state", certificate.getState().toString());
        result.put("subjectDN", x509Cert.getSubjectDN().toString());
        result.put("issuerDN", x509Cert.getIssuerDN().toString());
        result.put("sigAlgName", x509Cert.getSigAlgName());
        result.put("notBefore", DateUtils.getDateStr(x509Cert.getNotBefore()));
        result.put("notAfter", DateUtils.getDateStr(x509Cert.getNotAfter()));
        if(certificate.getAuthorityCertificateVS() != null) result.put("issuerSerialNumber", 
                certificate.getAuthorityCertificateVS().getSerialNumber().toString());
        return result;
    }

}
