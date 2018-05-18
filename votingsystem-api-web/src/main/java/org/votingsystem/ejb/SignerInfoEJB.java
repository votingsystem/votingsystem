package org.votingsystem.ejb;

import eu.europa.esig.dss.client.crl.OnlineCRLSource;
import eu.europa.esig.dss.client.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.validation.CertificateValidator;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import eu.europa.esig.dss.validation.reports.wrapper.DiagnosticData;
import eu.europa.esig.dss.x509.CertificateSource;
import eu.europa.esig.dss.x509.CertificateToken;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.throwable.CertificateValidationException;
import org.votingsystem.throwable.SignerValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class SignerInfoEJB implements SignerInfoService {

    private static final Logger log = Logger.getLogger(SignerInfoEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private Config config;

    @Override
    public User checkSigner(X509Certificate certificate, User.Type userType, String entityId, Certificate certificateCA)
            throws SignerValidationException, CertificateValidationException {
        try {
            return checkSigner(User.FROM_CERT(certificate, userType), userType, entityId, certificateCA);
        } catch (InstantiationException | IllegalAccessException | CertificateEncodingException e) {
            throw new SignerValidationException(e.getMessage());
        }
    }

    @Override
    public User checkSigner(User signer, User.Type userType, String entityId)
            throws SignerValidationException, CertificateValidationException {
        try {
            Certificate certificateCA = verifyCertificate(signer.getX509Certificate());
            return checkSigner(User.FROM_CERT(signer.getX509Certificate(), userType), userType, entityId, certificateCA);
        } catch (InstantiationException | IllegalAccessException | CertificateEncodingException e) {
            throw new SignerValidationException(e.getMessage());
        }
    }

    @Override
    public User checkSigner(X509Certificate certificate, User.Type userType, String entityId)
            throws SignerValidationException, CertificateValidationException {
        try {
            Certificate certificateCA = verifyCertificate(certificate);
            return checkSigner(User.FROM_CERT(certificate, userType), userType, entityId, certificateCA);
        } catch (InstantiationException | IllegalAccessException | CertificateEncodingException e) {
            throw new SignerValidationException(e.getMessage());
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public User checkSigner(User signer, User.Type signerType, String entityId, Certificate certificateCA)
            throws SignerValidationException, CertificateValidationException {
        X509Certificate x509CertSigner = signer.getX509Certificate();
        if(certificateCA == null) {
            throw new CertificateValidationException(Messages.currentInstance().get("signerCertWithoutCAErrorMsg"));
        }
        List<User> dbSignerList = null;
        Certificate signerCertificate = null;
        try {
            List<Certificate> certificates = em.createNamedQuery(Certificate.FIND_BY_SERIALNUMBER_AND_AUTHORITY)
                    .setParameter("serialNumber", x509CertSigner.getSerialNumber().longValue())
                    .setParameter("authorityCertificate", certificateCA).getResultList();
            switch (signerType) {
                case TIMESTAMP_SERVER:
                case BANK:
                case ENTITY:
                case IDENTITY_SERVER:
                    signer.setEntityId(entityId).setType(signerType);
                    dbSignerList = em.createQuery("select s from User s where s.entityId=:entityId")
                            .setParameter("entityId", entityId).getResultList();
                    break;
                case ANON_ELECTOR:
                    if(certificates.isEmpty()) {
                        CertVoteExtensionDto certExtensionDto = CertificateUtils.getCertExtensionData(CertVoteExtensionDto.class,
                                x509CertSigner, Constants.VOTE_OID);
                        Certificate certificate = Certificate.VOTE(certExtensionDto.getRevocationHash(),
                                x509CertSigner, certificateCA).setCertVoteExtension(certExtensionDto);
                        em.persist(certificate);
                        signer.setCertificate(certificate).setCertificateCA(certificateCA)
                                .setValidElector(true);
                    } else signer.setValidElector(false);
                    return signer;
                default:
                    if(signer.getNumId() == null) {
                        String certHash = CertificateUtils.getHash(x509CertSigner);
                        dbSignerList = em.createQuery("select s from User s where s.UUID=:UUID")
                                .setParameter("UUID", certHash).getResultList();
                        if(dbSignerList.isEmpty())
                            signer.setType(User.Type.ENTITY).setUUID(certHash);
                    } else {
                        dbSignerList = em.createQuery("select s from User s where s.numId=:numId and s.documentType=:typeId")
                                .setParameter("numId", signer.getNumId())
                                .setParameter("typeId", signer.getDocumentType()).getResultList();
                    }
                    break;
            }
            if(!dbSignerList.isEmpty()) {
                signer = dbSignerList.iterator().next();
            } else {
                em.persist(signer);
            }
            if(certificates.isEmpty()) {
                signerCertificate = Certificate.SIGNER(signer, x509CertSigner).setAuthorityCertificate(certificateCA);
                em.persist(signerCertificate);
                log.severe("Added new signer id: " + signer.getId() + " - certificate id: " + signerCertificate.getId() +
                        " - certificate subjectDN: " + signerCertificate.getSubjectDN() +
                        " - issuer subjectDN: " + signerCertificate.getAuthorityCertificate().getSubjectDN());
            } else {
                signerCertificate = certificates.iterator().next();
            }
            signer.setX509Certificate(x509CertSigner).setCertificate(signerCertificate).setCertificateCA(certificateCA);
            log.log(Level.FINE, "NumIdAndType: " + signer.getNumIdAndType() + " - signerType: " + signerType +
                    " - entityId: " + entityId + " - cert Authority: " + certificateCA.getSubjectDN());
            return signer;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateException ex) {
            String msg = Messages.currentInstance().get("signerCertErrorMsg", ex.getMessage());
            log.log(Level.SEVERE, msg + ((x509CertSigner != null) ? " - " + x509CertSigner.toString() : ""), ex);
            throw new SignerValidationException(msg);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new SignerValidationException(ex.getMessage());
        }
    }

    /**
     * Method that returns the Certificate Authority of the certToCheck
     * @param certToCheck
     * @return
     * @throws Exception
     */
    public Certificate verifyCertificate(X509Certificate certToCheck) throws CertificateValidationException {
        PKIXCertPathValidatorResult validatorResult = CertificateUtils.verifyCertificate(
                config.getTrustedCertAnchors(), false, Arrays.asList(certToCheck));
        X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        return config.getCACertificate(certCaResult.getSerialNumber().longValue());
    }

    public CertificateReports getCertificateStatus(CertificateToken token, CertificateSource trustedCertSource) {
        //CertificateToken token = DSSUtils.loadCertificate(new File("src/main/resources/keystore/ec.europa.eu.1.cer"));
        CertificateVerifier cv = new CommonCertificateVerifier();
        // We can inject several sources. eg: OCSP, CRL, AIA, trusted lists
        // Capability to download resources from AIA
        //cv.setDataLoader(new CommonsDataLoader());
        // Capability to request OCSP Responders
        cv.setOcspSource(new OnlineOCSPSource());
        // Capability to download CRL
        cv.setCrlSource(new OnlineCRLSource());
        // We now add trust anchors (trusted list, keystore,...)
        cv.setTrustedCertSource(trustedCertSource);
        // We also can add missing certificates
        //cv.setAdjunctCertSource(adjunctCertSource);
        CertificateValidator validator = CertificateValidator.fromCertificate(token);
        validator.setCertificateVerifier(cv);
        CertificateReports certificateReports = validator.validate();
        // We have 3 reports
        // The diagnostic data which contains all used and static data
        DiagnosticData diagnosticData = certificateReports.getDiagnosticData();
        // The detailed report which is the result of the process of the diagnostic data and the validation policy
        //DetailedReport detailedReport = certificateReports.getDetailedReport();

        // The simple report is a summary of the detailed report or diagnostic data (more user-friendly)
        //SimpleCertificateReport simpleReport = certificateReports.getSimpleReport();
        return certificateReports;
    }

}