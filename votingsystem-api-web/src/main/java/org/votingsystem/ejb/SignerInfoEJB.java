package org.votingsystem.ejb;

import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.User;
import org.votingsystem.throwable.CertificateRequestException;
import org.votingsystem.throwable.CertificateValidationException;
import org.votingsystem.throwable.InsufficientPrivilegesException;
import org.votingsystem.throwable.SignerValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.IdDocument;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
    public User checkIfAdmin(X509Certificate certToCheck) throws InsufficientPrivilegesException {
        log.severe("===== TODO");
        Set<User> adminCerts = null;
        try {
            for(User admin : adminCerts) {
                if(Arrays.equals(admin.getX509Certificate().getEncoded(), certToCheck.getEncoded())) {
                    return admin;
                }
            }
        } catch (Exception ex) {
            throw new InsufficientPrivilegesException(ex.getMessage(), ex);
        }
        throw new InsufficientPrivilegesException("InsufficientPrivilegesException - Cert: " +
                certToCheck.getSubjectDN() + " serial number: " + certToCheck.getSerialNumber());
    }

    public void loadCertInfo(User user, CertExtensionDto deviceData) throws CertificateRequestException {
        log.log(Level.FINE, "user id: " + user.getId() +  " - deviceData: " + deviceData);
        try {
            X509Certificate x509Cert = user.getX509Certificate();
            List<Certificate> certificateList = em.createQuery("SELECT c FROM Certificate c WHERE c.state =:state " +
                    "and c.serialNumber =:serialNumber and c.authorityCertificate =:authorityCertificate")
                    .setParameter("state", Certificate.State.OK)
                    .setParameter("serialNumber", x509Cert.getSerialNumber().longValue())
                    .setParameter("authorityCertificate", user.getCertificateCA()).getResultList();
            Device device = null;
            Certificate certificate = null;
            if(certificateList.isEmpty()) {
                certificate = Certificate.SIGNER(user, x509Cert);
                em.persist(certificate);
                if(deviceData != null) {
                    List<Device> deviceList = em.createNamedQuery(Device.FIND_BY_USER_AND_UUID).setParameter("user", user)
                            .setParameter("deviceUUID", deviceData.getUUID()).getResultList();
                    if(deviceList.isEmpty()) {
                        device = new Device(user, deviceData.getUUID(), deviceData.getEmail(),
                                deviceData.getMobilePhone(), deviceData.getDeviceName(), certificate);
                        em.persist(device);
                        log.log(Level.INFO, "Added new Device - device UUID: " + device.getUUID());
                    } else device.updateCertInfo(deviceData);
                }
                log.log(Level.INFO, "Added new Certificate - certificate id:" + certificate.getId());
            } else if(deviceData != null && deviceData.getUUID() != null) {
                List<Device> deviceList = em.createQuery("SELECT d FROM Device d WHERE d.UUID =:UUID and d.certificate =:certificate")
                        .setParameter("UUID", deviceData.getUUID()).setParameter("certificate", certificate).getResultList();
                if(deviceList.isEmpty()) {
                    device = new Device(user, deviceData.getUUID(), deviceData.getEmail(),
                            deviceData.getMobilePhone(), deviceData.getDeviceName(), certificate);
                    em.persist(device);
                    log.log(Level.INFO, "Added new Device - device UUID: " + device.getUUID());
                }
            }
            user.setCertificate(certificate);
            user.setDevice(device);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new CertificateRequestException(ex.getMessage(), ex);
        }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public User checkSigner(X509Certificate signerX509Cert, User.Type signerType, String entityId)
            throws SignerValidationException, CertificateValidationException {
        PKIXCertPathValidatorResult certValidatorResult = CertUtils.verifyCertificate(
                config.getTrustedCertAnchors(), false, Arrays.asList(signerX509Cert));
        X509Certificate x509CertCa = certValidatorResult.getTrustAnchor().getTrustedCert();
        Certificate certificateCA = config.getCACertificate(x509CertCa.getSerialNumber().longValue());
        if(certificateCA == null) {
            throw new CertificateValidationException(Messages.currentInstance().get("signerCertWithoutCAErrorMsg"));
        }
        User signer = null;
        List<User> dbSignerList = null;
        Certificate signerCertificate = null;
        try {
            CertExtensionDto deviceData = CertUtils.getCertExtensionData(CertExtensionDto.class, signerX509Cert, Constants.DEVICE_OID);
            signer = User.FROM_CERT(signerX509Cert, signerType);
            List<Certificate> certificates = em.createNamedQuery(Certificate.FIND_BY_SERIALNUMBER_AND_AUTHORITY)
                    .setParameter("serialNumber", signerX509Cert.getSerialNumber().longValue())
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
                                signerX509Cert, Constants.VOTE_OID);
                        Certificate certificate = Certificate.VOTE(certExtensionDto.getRevocationHashBase64(), signerX509Cert,
                                certificateCA).setCertVoteExtension(certExtensionDto);
                        em.persist(certificate);
                        signer.setCertificate(certificate).setCertificateCA(certificateCA)
                                .setValidElector(true);
                    } else signer.setValidElector(false);
                    return signer;
                default:
                    if(signer.getNumId() == null) {
                        if(deviceData != null && deviceData.getNumId() != null) {
                            dbSignerList = em.createQuery("select s from User s where s.numId=:numId and s.documentType=:typeId")
                                    .setParameter("numId", deviceData.getNumId())
                                    .setParameter("typeId", IdDocument.NIF).getResultList();
                        } else {
                            String certHash = CertificateUtils.getHash(signerX509Cert);
                            dbSignerList = em.createQuery("select s from User s where s.UUID=:UUID")
                                    .setParameter("UUID", certHash).getResultList();
                            if(dbSignerList.isEmpty())
                                signer.setType(User.Type.ENTITY).setUUID(certHash);
                        };
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
                signerCertificate = Certificate.SIGNER(signer, signerX509Cert)
                        .setAuthorityCertificate(certificateCA);
                em.persist(signerCertificate);
                log.severe("Added new signer id: " + signer.getId() + " - certificate id: " + signerCertificate.getId() +
                        " - certificate subjectDN: " + signerCertificate.getSubjectDN() +
                        " - issuer subjectDN: " + signerCertificate.getAuthorityCertificate().getSubjectDN());
            } else {
                signerCertificate = certificates.iterator().next();
            }
            signer.setX509Certificate(signerX509Cert).setCertificate(signerCertificate)
                    .setCertificateCA(certificateCA);
            log.log(Level.FINE, "user num. id and type:" + signer.getNumIdAndType() + " - cert Authority: " + certificateCA.getSubjectDN());
            return signer;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateException ex) {
            String msg = Messages.currentInstance().get("signerCertErrorMsg", ex.getMessage());
            log.log(Level.SEVERE, msg + ((signerX509Cert != null) ? " - " + signerX509Cert.toString() : ""), ex);
            throw new SignerValidationException(msg);
        } catch (CertificateValidationException ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new SignerValidationException(ex.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new SignerValidationException(ex.getMessage());
        }
    }

    public Certificate verifyCertificate(X509Certificate certToCheck) throws Exception {
        PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                config.getTrustedCertAnchors(), false, Arrays.asList(certToCheck));
        X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        return config.getCACertificate(certCaResult.getSerialNumber().longValue());
    }

}