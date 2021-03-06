package org.votingsystem.idprovider.ejb;

import eu.europa.esig.dss.x509.CertificateToken;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.crypto.*;
import org.votingsystem.dto.*;
import org.votingsystem.dto.currency.RegisterDto;
import org.votingsystem.dto.indentity.IdentityRequestDto;
import org.votingsystem.dto.indentity.SessionCertificationDto;
import org.votingsystem.dto.voting.CertVoteExtensionDto;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.model.*;
import org.votingsystem.model.voting.AnonVoteCertRequest;
import org.votingsystem.model.voting.Election;
import org.votingsystem.model.voting.UserCSRRequest;
import org.votingsystem.ocsp.RootCertOCSPInfo;
import org.votingsystem.throwable.RequestRepeatedException;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.votingsystem.xml.XML;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@DependsOn("SignatureServiceEJB")
public class CertIssuerEJB {

    private static final Logger log = Logger.getLogger(CertIssuerEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigIdProvider config;
    @Inject private CmsEJB cmsEJB;

    @Inject private SignerInfoService signerEJB;
    @EJB private SignatureServiceEJB signatureService;
    @EJB private ElectionsEJB electionsEJB;

    private Certificate certificate;
    private PrivateKey certIssuerPrivateKey;
    private X509Certificate certIssuerSigningCert;


    @PostConstruct
    public void initialize() {
        try {
            Properties properties = new Properties();
            File propertiesFile = new File(config.getApplicationDirPath() + "/sec/keystore.properties");
            properties.load(new FileInputStream(propertiesFile));

            String issuerKeyStoreFileName = (String) properties.get("issuerKeyStoreFileName");
            String issuerKeyPassword = (String) properties.get("issuerKeyPassword");

            KeyStore keyStoreCertIssuer = KeyStore.getInstance("JKS");
            keyStoreCertIssuer.load(new FileInputStream(config.getApplicationDirPath() + "/sec/" +
                    issuerKeyStoreFileName), issuerKeyPassword.toCharArray());
            String keyAlias = keyStoreCertIssuer.aliases().nextElement();
            certIssuerSigningCert = (X509Certificate) keyStoreCertIssuer.getCertificate(keyAlias);
            certificate = config.loadAuthorityCertificate(new CertificateToken(certIssuerSigningCert));
            certIssuerPrivateKey = (PrivateKey) keyStoreCertIssuer.getKey(keyAlias, issuerKeyPassword.toCharArray());
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    @TransactionAttribute(REQUIRES_NEW)
    public X509Certificate signUserCert(UserCSRRequest userCSR) throws Exception {
        Certificate issuedCert = signUserCert(userCSR.getUser(), userCSR.getContent());
        userCSR.setSerialNumber(issuedCert.getSerialNumber());
        em.persist(userCSR.setState(UserCSRRequest.State.OK));
        return issuedCert.getX509Certificate();
    }

    @TransactionAttribute(REQUIRES_NEW)
    public Certificate signUserCert(User user, byte userCSR[]) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validTo = now.plusYears(1); //one year
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(userCSR);
        Certificate issuedCert = signCSR(user, csr, null, now, validTo,
                Certificate.Type.USER, null);
        log.info("issued Certificate id: " + issuedCert.getSerialNumber() + " for user id: " + user.getId());
        return issuedCert;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public Certificate signUserCert(CMSDocument cmsMessage, RegisterDto registerDto) throws Exception {
        Certificate issuedCert = signUserCert(cmsMessage.getFirstSignature().getSigner(), registerDto.getCsr().getBytes());
        issuedCert.setSignedDocument(cmsMessage);
        return issuedCert;
    }

    public java.security.KeyStore generateUserKeyStore(String givenname, String surname, String nif,
                                                       char[] password) throws Exception {
        log.info("nif: " + nif + " - givenname: " + givenname + " - surname: " + surname);
        LocalDateTime validFrom = LocalDateTime.now();
        Date validFromDate = DateUtils.getUTCDate(validFrom);
        LocalDateTime validTo = validFrom.plusYears(1);
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
        signerEJB.checkSigner(issuedCert, User.Type.USER, null,
                config.getCACertificate(certIssuerSigningCert.getSerialNumber().longValue()));
        return keyStore;
    }


    public java.security.KeyStore generateTimeStampServerKeyStore(String givenName, String keyAlias,
                                                                  char[] password) throws Exception {
        log.info("givenName: " + givenName);
        LocalDateTime validFrom = LocalDateTime.now();
        Date validFromDate = DateUtils.getUTCDate(validFrom);
        LocalDateTime validTo = validFrom.plusYears(1);
        Date validToDate = DateUtils.getUTCDate(validTo);
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certIssuerSigningCert,
                certIssuerPrivateKey, Constants.ROOT_CERT_ALIAS);
        String userDN = format("GIVENNAME={0}", givenName);
        KeyStore keyStore = KeyStoreUtils.generateTimeStampServerKeyStore(validFromDate, validToDate, password, keyAlias,
                rootCAPrivateCredential, userDN, config.getOcspServerURL());
        X509Certificate issuedCert = (X509Certificate) keyStore.getCertificate(Constants.USER_CERT_ALIAS);
        signerEJB.checkSigner(issuedCert, User.Type.TIMESTAMP_SERVER, null,
                config.getCACertificate(certIssuerSigningCert.getSerialNumber().longValue()));
        return keyStore;
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
        KeyStore keyStore = KeyStoreUtils.generateUserKeyStore(validFromDate, validToDate, password, keyAlias,
                rootCAPrivateCredential, userDN, config.getOcspServerURL());
        X509Certificate issuedCert = (X509Certificate) keyStore.getCertificate(Constants.USER_CERT_ALIAS);
        signerEJB.checkSigner(issuedCert, User.Type.ENTITY, null,
                config.getCACertificate(certIssuerSigningCert.getSerialNumber().longValue()));
        return keyStore;
    }

    public java.security.KeyStore generateServerKeyStore(String commonName, String keyAlias,
                                                               char[] password) throws Exception {
        log.info("commonName: " + commonName);
        LocalDateTime validFrom = LocalDateTime.now();
        Date validFromDate = DateUtils.getUTCDate(validFrom);
        LocalDateTime validTo = validFrom.plusDays(365);
        Date validToDate = DateUtils.getUTCDate(validTo);
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certIssuerSigningCert,
                certIssuerPrivateKey, Constants.ROOT_CERT_ALIAS);
        String userDN = format("CN={0}", commonName);
        return KeyStoreUtils.generateUserKeyStore(validFromDate, validToDate, password, keyAlias,
                rootCAPrivateCredential, userDN, config.getOcspServerURL());
    }

    @TransactionAttribute(REQUIRES_NEW)
    public Certificate signCSR(User user, PKCS10CertificationRequest csr, String organizationalUnit, LocalDateTime dateBegin,
               LocalDateTime dateFinish, Certificate.Type certificateType, String revocationHash) throws Exception {
        X509Certificate issuedCert = CertificateUtils.signCSR(csr, organizationalUnit, certIssuerPrivateKey,
                certIssuerSigningCert, dateBegin, dateFinish, config.getOcspServerURL());
        Certificate result = null;
        switch (certificateType) {
            case VOTE:
                result = Certificate.VOTE(revocationHash, issuedCert, config.getCACertificate(
                        certIssuerSigningCert.getSerialNumber().longValue()));
                break;
            case USER:
                result = Certificate.ISSUED_USER_CERT(user, issuedCert, certificate);
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
            em.merge(signedDocument.setOperationType(OperationType.ANON_VOTE_CERT_REQUEST_REPEATED));
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
        if(idRequest.getRevocationHash() == null)
            throw new ValidationException("missing revocation hash");

        Election election = electionsEJB.getElection(idRequest.getUUID(), idRequest.getCallbackServiceEntityId().getId());
        if(!election.isActive(timeStampDate)) {
            throw new ValidationException("Election in state '" + election.getState() + "' - timeStampDate: " + timeStampDate +
                    " - election dates: [" + election.getDateBegin() + " - " + election.getDateFinish() + "]");
        }
        List<AnonVoteCertRequest> anonVoteCertRequests = em.createQuery(
                "select a from AnonVoteCertRequest a where a.revocationHash =:revocationHash " +
                        "and a.election.uuid=:electionUUID").setParameter("electionUUID", idRequest.getUUID())
                .setParameter("revocationHash", idRequest.getRevocationHash()).getResultList();
        if (!anonVoteCertRequests.isEmpty()) {
            throw new ValidationException("ERROR - RevocationHash:" +
                    idRequest.getRevocationHash() + " - already exists on electionUUID: " + idRequest.getUUID());
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
        CertVoteExtensionDto certExtensionDto = CertificateUtils.getCertExtensionData(CertVoteExtensionDto.class, csr,
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
        String revocationHash = certExtensionDto.getRevocationHash();

        LocalDateTime validFrom = election.getDateBegin();
        LocalDateTime validTo = election.getDateFinish();

        CsrResponse csrResponse = new CsrResponse(CertificateUtils.getPublicKey(csr), null, revocationHash);
        PKCS10CertificationRequest pkcs10CertReq = PEMUtils.fromPEMToPKCS10CertificationRequest(csrBytes);
        Certificate issuedCert =  signCSR(null, pkcs10CertReq, config.getEntityId(),
                validFrom, validTo, Certificate.Type.VOTE, csrResponse.getRevocationHash());
        csrResponse.setIssuedCert(PEMUtils.getPEMEncoded(issuedCert.getX509Certificate()));

        AnonVoteCertRequest anonVoteCertRequest = new AnonVoteCertRequest(signer, signedDocument,
                AnonVoteCertRequest.State.OK, revocationHash, election);
        em.persist(anonVoteCertRequest);
        return csrResponse;
    }

    public RootCertOCSPInfo getRootCertOCSPInfo() {
        RootCertOCSPInfo rootCertOCSPInfo = null;
        try {
            rootCertOCSPInfo = new RootCertOCSPInfo(certificate, certIssuerSigningCert, certIssuerPrivateKey);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return rootCertOCSPInfo;
    }

    /*  C=ES, ST=State or Province, L=locality name, O=organization name, OU=org unit, CN=common name,
    emailAddress=user@votingsystem.org, SERIALNUMBER=1234, SN=surname, GN=given name, GN=name given */
    public UserCSRRequest saveUserCSR(byte[] csrBytes) throws Exception {
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(csrBytes);
        User user = User.getUser(User.class, csr.getSubject());
        CertExtensionDto certExtensionDto = CertificateUtils.getCertExtensionData(CertExtensionDto.class, csr, Constants.DEVICE_OID);
        DeviceDto deviceDto = new DeviceDto(user, certExtensionDto);
        Device device = checkDeviceFromCSR(deviceDto);
        Query query = em.createQuery("select r from UserCSRRequest r where r.device.UUID =:UUID and " +
                "r.user.numId =:numId and r.user.documentType=:typeId and r.state =:state")
                .setParameter("UUID", device.getUUID())
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
                dto.getPhone(), dto.getEmail(), dto.getUUID(), dto.getDeviceType()));
        if(dto.getNumId() == null)
            throw new ValidationException("missing 'num id'");
        if(dto.getUUID() == null)
            throw new ValidationException("missing 'device UUID'");
        //TODO for now this only works with Spanish NIF
        String validatedNIF = NifUtils.validate(dto.getNumId());
        List<User> userList = em.createQuery("select u from User u where u.numId =:numId")
                .setParameter("numId", validatedNIF).getResultList();
        User user;
        if(userList.isEmpty()) {
            user = new User(User.Type.USER, dto.getName(), dto.getSurname(), dto.getEmail(), dto.getPhone())
                    .setNumIdAndType(validatedNIF, IdDocument.NIF);
            em.persist(user);
        } else user = userList.iterator().next();
        Device device = new Device(user, dto.getUUID(), dto.getEmail(), dto.getPhone(), dto.getDeviceType());
        em.persist(device.setState(Device.State.PENDING));
        return device;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public SignedDocument signSessionCSR(SignedDocument signedDocument) throws Exception {
        User signer = signedDocument.getFirstSignature().getSigner();
        X509Certificate signerCertificate = signedDocument.getFirstSignature().getSigningCert();
        SessionCertificationDto csrRequest = null;
        if(signedDocument instanceof CMSDocument)
            csrRequest = new JSON().getMapper().readValue(((CMSDocument)signedDocument).getCMS().getSignedContentStr(),
                    SessionCertificationDto.class);
        else
            csrRequest = signedDocument.getSignedContent(SessionCertificationDto.class);

        PKCS10CertificationRequest browserCSR = PEMUtils.fromPEMToPKCS10CertificationRequest(csrRequest.getBrowserCsr().getBytes());
        PKCS10CertificationRequest mobileCSR = PEMUtils.fromPEMToPKCS10CertificationRequest(csrRequest.getMobileCsr().getBytes());
        ZonedDateTime dateBegin = ZonedDateTime.now();
        ZonedDateTime dateFinish = dateBegin.plusDays(1).toLocalDate().atStartOfDay(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault());
        X509Certificate browserCert = CertificateUtils.signCSR(browserCSR, "browser-certificate", certIssuerPrivateKey,
                certIssuerSigningCert, dateBegin.toLocalDateTime(), dateFinish.toLocalDateTime(),
                config.getOcspServerURL());
        X509Certificate mobileCert = CertificateUtils.signCSR(mobileCSR, "mobile-certificate", certIssuerPrivateKey,
                certIssuerSigningCert, dateBegin.toLocalDateTime(), dateFinish.toLocalDateTime(),
                config.getOcspServerURL());

        Certificate browserCertificate = Certificate.SIGNER(signer, browserCert).setType(Certificate.Type.BROWSER_SESSION)
                .setAuthorityCertificate(config.getCACertificate(certIssuerSigningCert.getSerialNumber().longValue()))
                .setSignedDocument(signedDocument);
        Certificate mobileCertificate = Certificate.SIGNER(signer, mobileCert).setType(Certificate.Type.MOBILE_SESSION)
                .setAuthorityCertificate(config.getCACertificate(certIssuerSigningCert.getSerialNumber().longValue()))
                .setSignedDocument(signedDocument);

        SessionCertificationDto csrResponse = csrRequest.setOperation(new OperationTypeDto(
                CurrencyOperation.GET_SESSION_CERTIFICATION, config.getEntityId())).setUser(new UserDto(signer))
                .setBrowserCertificate(new String(PEMUtils.getPEMEncoded(browserCert)))
                .setMobileCertificate(new String(PEMUtils.getPEMEncoded(mobileCert)))
                .setSignerCertPEM(new String(PEMUtils.getPEMEncoded(signerCertificate)));
        em.persist(browserCertificate);
        em.persist(mobileCertificate);

        SessionCertification sessionCertification = new SessionCertification(signer, mobileCertificate,
                browserCertificate, signedDocument);
        em.persist(sessionCertification);
        log.info("sessionCertification id: " + sessionCertification.getId());
        SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.IDENTITY_SERVER,
                OperationType.SESSION_CERTIFICATION_RECEIPT).setWithTimeStampValidation(true);
        SignedDocument response = null;
        if(signedDocument instanceof CMSDocument)
            response = cmsEJB.signAndSave(new JSON().getMapper().writeValueAsBytes(csrResponse),
                    OperationType.SESSION_CERTIFICATION_RECEIPT);
        else
            response = signatureService.signXAdESAndSave(new XML().getMapper().writeValueAsBytes(csrResponse), signatureParams);
        em.merge(signedDocument.setReceipt(response).setOperationType(OperationType.SESSION_CERTIFICATION));
        return response;
    }


}
