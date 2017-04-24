package org.votingsystem.ejb;

import org.votingsystem.crypto.CertUtils;
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
    public User checkSigner(X509Certificate certificate, User.Type userType, String entityId)
            throws SignerValidationException, CertificateValidationException {
        try {
            return checkSigner(User.FROM_CERT(certificate, userType), userType, entityId);
        } catch (InstantiationException | IllegalAccessException | CertificateEncodingException e) {
            throw new SignerValidationException(e.getMessage());
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public User checkSigner(User signer, User.Type signerType, String entityId) throws SignerValidationException,
            CertificateValidationException {
        X509Certificate x509CertSigner = signer.getX509Certificate();
        Certificate certificateCA = verifyCertificate(x509CertSigner);
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
                        CertVoteExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertVoteExtensionDto.class,
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
                signerCertificate = Certificate.SIGNER(signer, x509CertSigner).setAuthorityCertificate(certificateCA)
                        .setUUID(CertificateUtils.getHash(x509CertSigner));
                em.persist(signerCertificate);
                log.severe("Added new signer id: " + signer.getId() + " - certificate id: " + signerCertificate.getId() +
                        " - certificate subjectDN: " + signerCertificate.getSubjectDN() +
                        " - issuer subjectDN: " + signerCertificate.getAuthorityCertificate().getSubjectDN());
            } else {
                signerCertificate = certificates.iterator().next();
            }
            signer.setX509Certificate(x509CertSigner).setCertificate(signerCertificate)
                    .setCertificateCA(certificateCA);
            log.log(Level.FINE, "NumIdAndType: " + signer.getNumIdAndType() + " - cert Authority: " + certificateCA.getSubjectDN());
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
        PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                config.getTrustedCertAnchors(), false, Arrays.asList(certToCheck));
        X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        return config.getCACertificate(certCaResult.getSerialNumber().longValue());
    }

}