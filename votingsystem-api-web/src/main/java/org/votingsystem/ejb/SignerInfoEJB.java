package org.votingsystem.ejb;

import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.SignatureParams;
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

    @PersistenceContext private EntityManager em;
    @Inject private Config config;

    /**
     * @param signatureParams
     * @return
     * @throws CertificateEncodingException
     */
    @TransactionAttribute(REQUIRES_NEW)
    public User checkSigner(SignatureParams signatureParams) throws SignerValidationException {
        User requestSigner;
        List<User> userListDB = null;
        X509Certificate signerCert = signatureParams.getSigningCert();
        //CertExtensionDto deviceData = CertUtils.getCertExtensionData(CertExtensionDto.class, signerCert, Constants.DEVICE_OID);
        try {
            requestSigner = User.FROM_CERT(signerCert, signatureParams.getSignerType());
            List<Certificate> certificates = em.createNamedQuery(Certificate.FIND_BY_SERIALNUMBER_AND_AUTHORITY)
                    .setParameter("serialNumber", signerCert.getSerialNumber().longValue())
                    .setParameter("authorityCertificate", signatureParams.getCertificateCA()).getResultList();
            switch (signatureParams.getSignerType()) {
                case TIMESTAMP_SERVER:
                case BANK:
                case ENTITY:
                    requestSigner.setEntityId(signatureParams.getEntityId());
                    userListDB = em.createQuery("select s from User s where s.entityId=:entityId")
                            .setParameter("entityId", signatureParams.getEntityId()).getResultList();
                    break;
                case ANON_ELECTOR:
                    if(certificates.isEmpty()) {
                        CertVoteExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertVoteExtensionDto.class,
                                signerCert, Constants.VOTE_OID);
                        Certificate certificate = Certificate.VOTE(certExtensionDto.getRevocationHashBase64(), signerCert,
                                signatureParams.getCertificateCA()).setCertVoteExtension(certExtensionDto);
                        em.persist(certificate);
                        requestSigner.setCertificate(certificate).setCertificateCA(signatureParams.getCertificateCA())
                                .setValidElector(true);
                    } else requestSigner.setValidElector(false);
                    return requestSigner;
                default:
                    if(requestSigner.getNumId() == null || requestSigner.getDocumentType() == null)
                        throw new SignerValidationException("Missing user identification data");
                    userListDB = em.createQuery("select s from User s where s.numId=:numId and s.documentType=:typeId")
                            .setParameter("numId", requestSigner.getNumId())
                            .setParameter("typeId", requestSigner.getDocumentType()).getResultList();
                    break;
            }
            if(!userListDB.isEmpty()) {
                requestSigner = userListDB.iterator().next();
            } else {
                if(signatureParams.getCertificateCA() == null) {
                    String msg = Messages.currentInstance().get("signerCertWithoutCAErrorMsg");
                    log.log(Level.SEVERE, msg + " - " + signerCert.toString());
                    throw new CertificateValidationException(msg);
                }
                em.persist(requestSigner);
            }
            Certificate certificate = null;
            if(certificates.isEmpty()) {
                certificate = Certificate.SIGNER(requestSigner, signerCert)
                        .setAuthorityCertificate(signatureParams.getCertificateCA());
                em.persist(certificate);
                log.severe("Added new signer id: " + requestSigner.getId() + " - certificate id: " + certificate.getId() +
                        " - certificate subjectDN: " + certificate.getSubjectDN() +
                        " - issuer subjectDN: " + certificate.getAuthorityCertificate().getSubjectDN());
            } else {
                certificate = certificates.iterator().next();
            }
            requestSigner.setX509Certificate(signerCert).setCertificate(certificate);
            return requestSigner;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateException ex) {
            String msg = Messages.currentInstance().get("signerCertErrorMsg", ex.getMessage());
            log.log(Level.SEVERE, msg + ((signerCert != null) ? " - " + signerCert.toString() : ""), ex);
            throw new SignerValidationException(msg);
        } catch (CertificateValidationException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new SignerValidationException(ex.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new SignerValidationException(ex.getMessage());
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public User checkSigner(X509Certificate x509Certificate, User.Type userType, String entityId)
            throws SignerValidationException, CertificateValidationException {
        PKIXCertPathValidatorResult certValidatorResult = CertUtils.verifyCertificate(
                config.getTrustedCertAnchors(), false, Arrays.asList(x509Certificate));
        X509Certificate certCaResult = certValidatorResult.getTrustAnchor().getTrustedCert();
        Certificate caCertificate = config.getCACertificate(certCaResult.getSerialNumber().longValue());
        return checkSigner(new SignatureParams(entityId, userType, x509Certificate).setCertificateCA(caCertificate));
    }

}
