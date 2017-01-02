package org.votingsystem.idprovider.ejb;

import eu.europa.esig.dss.x509.CertificateToken;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.crypto.*;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.DeviceDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.Device;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.voting.AnonVoteCertRequest;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.UserCSRRequest;
import org.votingsystem.ocsp.RootCertOCSPInfo;
import org.votingsystem.throwable.RequestRepeatedException;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class CertIssuerEJB {

    private static final Logger log = Logger.getLogger(CertIssuerEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigIdProvider config;
    @Inject SignerInfoService signerEJB;
    @EJB ElectionsEJB electionsEJB;

    private PrivateKey certIssuerPrivateKey;
    private X509Certificate certIssuerSigningCert;
    private RootCertOCSPInfo rootCertOCSPInfo;


    @PostConstruct
    public void initialize() {
        try {
            KeyStore keyStoreCertIssuer = KeyStore.getInstance("JKS");
            keyStoreCertIssuer.load(new FileInputStream(config.getApplicationDirPath() + "/sec/" +
                            config.getIssuerKeyStoreFileName()),
                    "local-demo".toCharArray());
            String keyAlias = keyStoreCertIssuer.aliases().nextElement();
            certIssuerSigningCert = (X509Certificate) keyStoreCertIssuer.getCertificate(keyAlias);
            Certificate certificate = config.loadAuthorityCertificate(new CertificateToken(certIssuerSigningCert));
            certIssuerPrivateKey = (PrivateKey) keyStoreCertIssuer.getKey(keyAlias,
                    config.getIssuerKeyPassword().toCharArray());
            rootCertOCSPInfo = new RootCertOCSPInfo(certificate, certIssuerSigningCert, certIssuerPrivateKey);
            signerEJB.checkSigner(config.getSigningCert(), User.Type.ENTITY, config.getEntityId());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public X509Certificate signUserCert(UserCSRRequest userCSR) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validTo = now.plusDays(365); //one year
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(userCSR.getContent());
        Certificate issuedCert = signCSR(userCSR.getUser(), csr, null, now, validTo,
                Certificate.Type.USER, null);
        userCSR.setSerialNumber(issuedCert.getSerialNumber());
        em.persist(userCSR.setState(UserCSRRequest.State.OK));
        log.info("issued Certificate id: " + issuedCert.getSerialNumber() + " for UserRequestCsr: " + userCSR.getId());
        return issuedCert.getX509Certificate();
    }

    public java.security.KeyStore generateUserKeyStore(String givenname, String surname, String nif,
                                                       char[] password) throws Exception {
        log.info("nif: " + nif + " - givenname: " + givenname + " - surname: " + surname);
        LocalDateTime validFrom = LocalDateTime.now();
        Date validFromDate = DateUtils.getUTCDate(validFrom);
        LocalDateTime validTo = validFrom.plusDays(365);
        Date validToDate = DateUtils.getUTCDate(validTo);
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certIssuerSigningCert,
                certIssuerPrivateKey, Constants.ROOT_CERT_ALIAS);
        String testUserDN;
        if(surname == null)
            testUserDN = format("GIVENNAME={0}, SERIALNUMBER={1}", givenname, nif);
        else
            testUserDN = format("GIVENNAME={0}, SURNAME={1} , SERIALNUMBER={2}", givenname, surname, nif);
        KeyStore keyStore = KeyStoreUtils.generateUserKeyStore(validFromDate, validToDate, password, Constants.USER_CERT_ALIAS,
                rootCAPrivateCredential, testUserDN, config.getOcspServerURL());
        X509Certificate issuedCert = (X509Certificate) keyStore.getCertificate(Constants.USER_CERT_ALIAS);
        signerEJB.checkSigner(issuedCert, User.Type.USER, null);
        return keyStore;
    }


    public java.security.KeyStore generateTimeStampServerKeyStore(String givenName, String keyAlias,
                                                                  char[] password) throws Exception {
        log.info("givenName: " + givenName);
        LocalDateTime validFrom = LocalDateTime.now();
        Date validFromDate = DateUtils.getUTCDate(validFrom);
        LocalDateTime validTo = validFrom.plusDays(365);
        Date validToDate = DateUtils.getUTCDate(validTo);
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certIssuerSigningCert,
                certIssuerPrivateKey, Constants.ROOT_CERT_ALIAS);
        String userDN = format("GIVENNAME={0}", givenName);
        return KeyStoreUtils.generateTimeStampServerKeyStore(validFromDate, validToDate, password, keyAlias,
                rootCAPrivateCredential, userDN, config.getOcspServerURL());
    }

    public java.security.KeyStore generateSystemEntityKeyStore(String givenName, String keyAlias,
                                                               char[] password) throws Exception {
        log.info("givenName: " + givenName);
        LocalDateTime validFrom = LocalDateTime.now();
        Date validFromDate = DateUtils.getUTCDate(validFrom);
        LocalDateTime validTo = validFrom.plusDays(365);
        Date validToDate = DateUtils.getUTCDate(validTo);
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certIssuerSigningCert,
                certIssuerPrivateKey, Constants.ROOT_CERT_ALIAS);
        String userDN = format("GIVENNAME={0}", givenName);
        return KeyStoreUtils.generateUserKeyStore(validFromDate, validToDate, password, keyAlias,
                rootCAPrivateCredential, userDN, config.getOcspServerURL());
    }

    @TransactionAttribute(REQUIRES_NEW)
    public Certificate signCSR(User user, PKCS10CertificationRequest csr, String organizationalUnit, LocalDateTime dateBegin,
               LocalDateTime dateFinish, Certificate.Type certificateType, String revocationHashBase64) throws Exception {
        X509Certificate issuedCert = CertUtils.signCSR(csr, organizationalUnit, certIssuerPrivateKey,
                certIssuerSigningCert, dateBegin, dateFinish, config.getOcspServerURL());
        Certificate result = null;
        switch (certificateType) {
            case VOTE:
                result = Certificate.VOTE(revocationHashBase64, issuedCert, config.getCACertificate(
                        certIssuerSigningCert.getSerialNumber().longValue()));
                break;
            case USER:
                result = Certificate.SIGNER(user, issuedCert);
                break;
            default:
                log.severe("unknown certificateType: " + certificateType);
        }
        if(result != null) {
            em.persist(result);
            log.info("Issued new Certificate - " + result.getSerialNumber());
        }
        return result;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public CsrResponse processAnonymousCertificateRequest(SignedDocument signedDocument, byte[] csr) throws Exception {
        IdentityRequestDto request =  signedDocument.getSignedContent(IdentityRequestDto.class);
        try {
            switch (request.getType()) {
                case ANON_VOTE_CERT_REQUEST:
                    return issueAnonVoteCert(request, signedDocument, csr);
                default:
                    throw new ValidationException(
                            "ERROR processing anonymous certificate request - unexpected operation type: " + request.getType());
            }
        } catch (RequestRepeatedException ex) {
            em.merge(signedDocument.setSignedDocumentType(SignedDocumentType.ANON_VOTE_CERT_REQUEST_REPEATED));
            log.severe("RequestRepeatedException: " + ex.getMessage() + " - SignedDocument id: " + signedDocument.getId());
            return new CsrResponse(ResponseDto.SC_ERROR, ex.getMessage());
        }
    }

    private CsrResponse issueAnonVoteCert(IdentityRequestDto idRequest, SignedDocument signedDocument, byte[] csrBytes)
            throws Exception {
        User signer = signedDocument.getSignatures().iterator().next().getSigner();
        LocalDateTime timeStampDate = signedDocument.getSignatures().iterator().next().getSignatureDate();

        if(OperationType.ANON_VOTE_CERT_REQUEST != idRequest.getType())
            throw new ValidationException("Expected operation 'ANON_VOTE_CERT_REQUEST' found: " + idRequest.getType());
        if(idRequest.getUUID() == null)
            throw new ValidationException("missing param election UUID");
        if(idRequest.getIndentityServiceEntity() == null)
            throw new ValidationException("missing indentity service entity");
        if(!idRequest.getIndentityServiceEntity().getId().equals(config.getEntityId()))
            throw new ValidationException("Exepected indentity service: " + config.getEntityId() + " found " +
                    idRequest.getIndentityServiceEntity().getId());
        if(idRequest.getCallbackServiceEntityId() == null)
            throw new ValidationException("missing callback service entity");
        if(idRequest.getRevocationHashBase64() == null)
            throw new ValidationException("missing revocation hash");

        Election election = electionsEJB.getElection(idRequest.getUUID(), idRequest.getCallbackServiceEntityId().getId());
        if(!election.isActive(timeStampDate)) {
            throw new ValidationException("Election in state '" + election.getState() + "' - timeStampDate: " + timeStampDate +
                    " - election dates: [" + election.getDateBegin() + " - " + election.getDateFinish() + "]");
        }
        List<AnonVoteCertRequest> anonVoteCertRequests = em.createQuery(
                "select a from AnonVoteCertRequest a where a.revocationHashBase64 =:revocationHashBase64 " +
                        "and a.election.uuid=:electionUUID").setParameter("electionUUID", idRequest.getUUID())
                .setParameter("revocationHashBase64", idRequest.getRevocationHashBase64()).getResultList();
        if (!anonVoteCertRequests.isEmpty()) {
            throw new ValidationException("ERROR - RevocationHash:" +
                    idRequest.getRevocationHashBase64() + " - already exists on electionUUID: " + idRequest.getUUID());
        }
        anonVoteCertRequests = em.createQuery(
                "select a from AnonVoteCertRequest a where a.user =:user and a.election=:election and a.state=:state")
                .setParameter("user", signer).setParameter("election", election)
                .setParameter("state", AnonVoteCertRequest.State.OK).getResultList();
        if (!anonVoteCertRequests.isEmpty()) {
            throw new RequestRepeatedException(Messages.currentInstance().get("voteRequestRepeatedErrorMsg",
                    signer.getNumIdAndType(),election.getSubject()));
        }

        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrBytes);
        CertVoteExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertVoteExtensionDto.class, csr,
                Constants.VOTE_OID);
        if (!certExtensionDto.getElectionUUID().equals(election.getUUID())) {
            throw new ValidationException("validateCSRVote - expected event UUID: " + election.getUUID() + " - found:" +
                    certExtensionDto.getElectionUUID());
        }
        if (!certExtensionDto.getVotingServiceEntity().equals(election.getEntityId())) {
            throw new ValidationException("validateCSRVote - expected voting service id: " + election.getEntityId() +
                    " - found:" + certExtensionDto.getVotingServiceEntity());
        }
        if (!certExtensionDto.getIdentityServiceEntity().equals(config.getEntityId())) {
            throw new ValidationException("validateCSRVote - expected identity service id: " + config.getEntityId() +
                    " - found:" + certExtensionDto.getIdentityServiceEntity());
        }
        String revocationHashBase64 = certExtensionDto.getRevocationHashBase64();
        //String revocationHashBase64 = UUID.randomUUID().toString();

        LocalDateTime validFrom = election.getDateBegin();
        LocalDateTime validTo = election.getDateFinish();


        CsrResponse csrResponse = new CsrResponse(CertUtils.getPublicKey(csr), null, revocationHashBase64);
        PKCS10CertificationRequest pkcs10CertReq = PEMUtils.fromPEMToPKCS10CertificationRequest(csrBytes);
        Certificate issuedCert =  signCSR(null, pkcs10CertReq, config.getEntityId(),
                validFrom, validTo, Certificate.Type.VOTE, csrResponse.getRevocationHashBase64());
        csrResponse.setIssuedCert(PEMUtils.getPEMEncoded(issuedCert.getX509Certificate()));

        AnonVoteCertRequest anonVoteCertRequest = new AnonVoteCertRequest(signer, signedDocument,
                AnonVoteCertRequest.State.OK, revocationHashBase64, election);
        em.persist(anonVoteCertRequest);
        return csrResponse;
    }

    public RootCertOCSPInfo getRootCertOCSPInfo() {
        return rootCertOCSPInfo;
    }

    /*  C=ES, ST=State or Province, L=locality name, O=organization name, OU=org unit, CN=common name,
    emailAddress=user@votingsystem.org, SERIALNUMBER=1234, SN=surname, GN=given name, GN=name given */
    public UserCSRRequest saveUserCSR(byte[] csrBytes) throws Exception {
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrBytes);
        User user = User.getUser(csr.getSubject());
        CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class, csr, Constants.DEVICE_OID);
        DeviceDto deviceDto = new DeviceDto(user, certExtensionDto);
        Device device = checkDeviceFromCSR(deviceDto);
        Query query = em.createQuery("select r from UserCSRRequest r where r.device.deviceId =:device and " +
                "r.user.numId =:numId and r.user.documentType=:typeId and r.state =:state")
                .setParameter("device", device.getDeviceId())
                .setParameter("numId", user.getNumId()).setParameter("typeId", user.getDocumentType())
                .setParameter("state", UserCSRRequest.State.PENDING);
        List<UserCSRRequest> previousRequestList = query.getResultList();
        for(UserCSRRequest prevRequest: previousRequestList) {
            prevRequest.setState(UserCSRRequest.State.CANCELED);
        }
        UserCSRRequest csrRequest = new UserCSRRequest(UserCSRRequest.State.PENDING, csrBytes, device);
        em.persist(new UserCSRRequest(UserCSRRequest.State.PENDING, csrBytes, device));
        log.info("csrRequest id:" + csrRequest.getId() + " - cert subject: " + csr.getSubject());
        return csrRequest;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public Device checkDeviceFromCSR(DeviceDto dto) throws ValidationException {
        log.info(format("checkDevice - givenname: {0} - surname: {1} - nif:{2} - phone: {3}" +
                        " - email: {4} - deviceId: {5} - deviceType: {6}", dto.getName(), dto.getSurname(), dto.getNumId(),
                dto.getPhone(), dto.getEmail(), dto.getDeviceId(), dto.getDeviceType()));
        if(dto.getNumId() == null) throw new ValidationException("missing 'getNumId'");
        if(dto.getDeviceId() == null) throw new ValidationException("missing 'deviceId'");
        //TODO for now this only works with Spanish NIF
        String validatedNIF = org.votingsystem.util.NifUtils.validate(dto.getNumId());
        List<User> userList = em.createQuery("select u from User u where u.numId =:numId")
                .setParameter("numId", validatedNIF).getResultList();
        User user;
        if(userList.isEmpty()) {
            user = new User(User.Type.USER, dto.getName(), dto.getSurname(), dto.getEmail(), dto.getPhone())
                    .setNumIdAndType(validatedNIF, IdDocument.NIF);
            em.persist(user);
        } else user = userList.iterator().next();
        Device device = new Device(user, dto.getDeviceId(), dto.getEmail(), dto.getPhone(), dto.getDeviceType());
        em.persist(device.setState(Device.State.PENDING));
        return device;
    }

}
