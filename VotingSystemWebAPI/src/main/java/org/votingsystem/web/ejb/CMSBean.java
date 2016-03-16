package org.votingsystem.web.ejb;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.cms.CMSGenerator;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.CMSDto;
import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.dto.UserCertificationRequestDto;
import org.votingsystem.dto.voting.KeyStoreDto;
import org.votingsystem.model.*;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.model.voting.Vote;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.*;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class CMSBean {

    private static Logger log = Logger.getLogger(CMSBean.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TimeStampBean timeStampBean;
    @Inject
    SubscriptionBean subscriptionBean;
    private CMSGenerator cmsGenerator;
    private Encryptor encryptor;
    private Set<TrustAnchor> trustAnchors;
    private Set<TrustAnchor> currencyAnchors;
    private Set<Long> anonymousCertIssuers;
    private Set<X509Certificate> trustedCerts;
    private PrivateKey serverPrivateKey;
    private Certificate serverCertificate;
    private X509Certificate localServerCertSigner;
    private List<X509Certificate> certChain;
    private byte[] keyStoreCertificatesPEM;
    private Map<Long, Certificate> trustedCertsHashMap = new HashMap<>();
    private static final HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsMap = new HashMap<>();
    private Set<String> admins;
    private User systemUser;
    private String password;
    private String keyAlias;

    public void init() throws Exception {
        Properties properties = new Properties();
        URL res = Thread.currentThread().getContextClassLoader().getResource("KeyStore.properties");
        log.info("init - res: " + res.toURI());
        properties.load(res.openStream());
        keyAlias = properties.getProperty("vs.signKeyAlias");
        password = properties.getProperty("vs.signKeyPassword");
        String keyStoreFileName = properties.getProperty("vs.keyStoreFile");
        res = Thread.currentThread().getContextClassLoader().getResource(keyStoreFileName);
        File keyStoreFile = FileUtils.getFileFromBytes(IOUtils.toByteArray(res.openStream()));
        cmsGenerator = new CMSGenerator(FileUtils.getBytesFromFile(keyStoreFile),
                keyAlias, password.toCharArray(), ContextVS.SIGNATURE_ALGORITHM);
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
        certChain = new ArrayList<>();
        for(java.security.cert.Certificate certificate : keyStore.getCertificateChain(keyAlias)) {
            checkAuthorityCertDB((X509Certificate) certificate);
            certChain.add((X509Certificate) certificate);
        }
        keyStoreCertificatesPEM = PEMUtils.getPEMEncoded (certChain);
        localServerCertSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        currencyAnchors = new HashSet<>();

        currencyAnchors.add(new TrustAnchor(localServerCertSigner, null));
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber")
                .setParameter("serialNumber", localServerCertSigner.getSerialNumber().longValue());
        serverCertificate = dao.getSingleResult(Certificate.class, query);
        serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        encryptor = new Encryptor(localServerCertSigner, serverPrivateKey);
    }

    public boolean isAdmin(String nif) {
        return admins.contains(nif);
    }

    public void setAdmins(Set<String> admins) {
        this.admins = admins;
    }

    public User getSystemUser() {
        return systemUser;
    }

    public byte[] getKeyStoreCertificatesPEM() {
        return keyStoreCertificatesPEM;
    }

    public Set<TrustAnchor> getCurrencyAnchors() {
        return currencyAnchors;
    }

    public KeyStoreInfo getKeyStoreInfo(byte[] keyStoreBytes, String keyAlias) throws Exception {
        java.security.KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password.toCharArray());
        PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        return new KeyStoreInfo(keyStore, privateKeySigner, certSigner);
    }

    public void initAdmins(List<User> admins) throws Exception {
        systemUser = config.getSystemUser();
        Set<String> adminsNIF = new HashSet<>();
        for(User user :admins) {
            verifyUserCertificate(user);
            user = subscriptionBean.checkUser(user);
            adminsNIF.add(user.getNif());
        }
        systemUser.updateAdmins(adminsNIF);
        dao.merge(systemUser);
        log.info("initAdmins - admins list:" + adminsNIF);
        setAdmins(adminsNIF);
    }

    public void initAnonymousCertAuthorities(List<X509Certificate>  anonymous_provider_TrustedCerts) throws Exception {
        anonymousCertIssuers = new HashSet<>();
        for(X509Certificate anonymous_provider:anonymous_provider_TrustedCerts) {
            anonymousCertIssuers.add(anonymous_provider.getSerialNumber().longValue());
        }
    }

    public void initCertAuthorities(List<X509Certificate> resourceCerts) throws Exception {
        log.info("initCertAuthorities - resourceCerts.size: " + resourceCerts.size());
        for(X509Certificate fileSystemX509TrustedCert:resourceCerts) {
            checkAuthorityCertDB(fileSystemX509TrustedCert);
        }
        Query query = dao.getEM().createQuery("SELECT c FROM Certificate c WHERE c.type in :typeList and c.state =:state")
                .setParameter("typeList", Arrays.asList(Certificate.Type.CERTIFICATE_AUTHORITY,
                        Certificate.Type.CERTIFICATE_AUTHORITY_ID_CARD))
                .setParameter("state", Certificate.State.OK);
        List<Certificate>  trustedCertsList = query.getResultList();
        trustedCertsHashMap = new HashMap<>();
        trustedCerts = new HashSet<>();
        trustAnchors = new HashSet<>();
        for (Certificate certificate : trustedCertsList) {
            addCertAuthority(certificate);
        }
    }

    public void addCertAuthority(Certificate certificate) throws Exception {
        X509Certificate x509Cert = certificate.getX509Cert();
        trustedCerts.add(x509Cert);
        trustedCertsHashMap.put(x509Cert.getSerialNumber().longValue(), certificate);
        TrustAnchor trustAnchor = new TrustAnchor(x509Cert, null);
        trustAnchors.add(trustAnchor);
        log.info("addCertAuthority - certificate.id: " + certificate.getId() + " - " + x509Cert.getSubjectDN() +
                " - num. trustedCerts: " + trustedCerts.size());
    }

    private Certificate checkAuthorityCertDB(X509Certificate x509AuthorityCert) throws CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException, ExceptionVS {
        log.info(x509AuthorityCert.getSubjectDN().toString());
        Query query = dao.getEM().createQuery("SELECT c FROM Certificate c WHERE c.serialNumber =:serialNumber")
                .setParameter("serialNumber", x509AuthorityCert.getSerialNumber().longValue());
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if(certificate == null) {
            certificate = dao.persist(Certificate.AUTHORITY(x509AuthorityCert, null));
            log.info("ADDED NEW FILE SYSTEM CA CERT - certificate.id:" + certificate.getId() + " - type: " +
                    certificate.getType());
        } else if (Certificate.State.OK != certificate.getState()) {
            throw new ExceptionVS("File system athority cert: " + x509AuthorityCert.getSubjectDN() + " }' " +
                    " - certificate.id: " + certificate.getId() + " - state:" + certificate.getState());
        }
        return certificate;
    }

    public void validateVoteCerts(CMSSignedMessage cmsMessage, EventVS eventVS) throws Exception {
        Set<User> signersVS = cmsMessage.getSigners();
        if(signersVS.isEmpty()) throw new ExceptionVS("ERROR - document without signers");
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS);
        for(User user : signersVS) {
            CertUtils.verifyCertificate(eventTrustedAnchors, false, Arrays.asList(user.getX509Certificate()));
            //X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        }
    }

    public Set<TrustAnchor> getEventTrustedAnchors(EventVS eventVS) throws Exception {
        Set<TrustAnchor> eventTrustedAnchors = eventTrustedAnchorsMap.get(eventVS.getId());
        if(eventTrustedAnchors == null) {
            Certificate eventCACert = eventVS.getCertificate();
            X509Certificate certCAEventVS = eventCACert.getX509Cert();
            eventTrustedAnchors = new HashSet<>();
            eventTrustedAnchors.add(new TrustAnchor(certCAEventVS, null));
            eventTrustedAnchors.addAll(getTrustAnchors());
            eventTrustedAnchorsMap.put(eventVS.getId(), eventTrustedAnchors);
        }
        return eventTrustedAnchors;
    }

    public boolean isSignerCertificate(Set<User> signers, X509Certificate cert) throws CertificateEncodingException {
        for(User user : signers) {
            if(Arrays.equals(user.getX509Certificate().getEncoded(), cert.getEncoded())) return true;
        }
        return false;
    }

    public KeyStoreDto generateElectionKeysStore(EventVS eventVS) throws Exception {
        //StringUtils.getRandomAlphaNumeric(7).toUpperCase()
        // _ TODO _ ====== crypto token
        String eventVSUrl = config.getContextURL() + "/rest/eventElection/id/" + eventVS.getId();
        String strSubjectDNRoot = format("CN=eventVSUrl:{0}, OU=Elections", eventVSUrl);
        java.security.KeyStore keyStore = KeyStoreUtil.createRootKeyStore(eventVS.getDateBegin(), eventVS.getDateFinish(),
                password.toCharArray(), keyAlias, strSubjectDNRoot);
        java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        java.security.cert.Certificate cert = chain[0];
        return new KeyStoreDto(new KeyStore(keyAlias, KeyStoreUtil.getBytes(keyStore, password.toCharArray()),
                eventVS.getDateBegin(), eventVS.getDateFinish()), (X509Certificate) cert);
    }

    public java.security.KeyStore generateKeysStore(String givenName, String surname, String nif, char[] password) throws Exception {
        log.info("generateKeysStore - nif: " + nif);
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(localServerCertSigner,
                serverPrivateKey, keyAlias);
        String testUserDN = null;
        if(surname == null) testUserDN = format("GIVENNAME={0}, SERIALNUMBER={1}", givenName, nif);
        else testUserDN = format("GIVENNAME={0}, SURNAME={1} , SERIALNUMBER={2}", givenName, surname, nif);
        //String strSubjectDN = "CN=Voting System Cert Authority , OU=VotingSystem"
        //KeyStore rootCAKeyStore = KeyStoreUtil.createRootKeyStore (validFrom.getTime(), (validTo.getTime() - validFrom.getTime()),
        //        userPassword.toCharArray(), keyAlias, strSubjectDN);
        //X509Certificate certSigner = (X509Certificate)rootCAKeyStore.getX509Certificate(keyAlias);
        //PrivateKey privateKeySigner = (PrivateKey)rootCAKeyStore.getKey(keyAlias, userPassword.toCharArray());
        //X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKeySigner,  keyAlias);
        return KeyStoreUtil.createUserKeyStore(validFrom.getTime(),
                (validTo.getTime() - validFrom.getTime()), password, ContextVS.KEYSTORE_USER_CERT_ALIAS,
                rootCAPrivateCredential, testUserDN);
    }

    public X509Certificate getServerCert() {
        return localServerCertSigner;
    }

    public Certificate getServerCertificate() {
        return serverCertificate;
    }

    private PrivateKey getServerPrivateKey() {
        return serverPrivateKey;
    }

    private Map<Long, Certificate> getTrustedCertsHashMap() {
        return trustedCertsHashMap;
    }

    public PKIXCertPathValidatorResult verifyCertificate(X509Certificate certToValidate) throws Exception {
        return CertUtils.verifyCertificate(getTrustAnchors(), false, Arrays.asList(certToValidate));
    }

    public boolean isSystemSignedMessage(Set<User> signers) {
        for(User user : signers) {
            if(user.getX509Certificate().equals(localServerCertSigner)) return true;
        }
        return false;
    }

    public X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, Date dateBegin,
                                   Date dateFinish) throws Exception {
        X509Certificate issuedCert = CertUtils.signCSR(csr, organizationalUnit, getServerPrivateKey(),
                getServerCert(), dateBegin, dateFinish);
        return issuedCert;
    }

    public CMSSignedMessage signData(byte[] contentToSign) throws Exception {
        return cmsGenerator.signData(contentToSign);
    }

    public CMSSignedMessage signDataWithTimeStamp(byte[] contentToSign) throws Exception {
        TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(cmsGenerator.getSignatureMechanism(), contentToSign);
        return cmsGenerator.signDataWithTimeStamp(contentToSign, timeStampToken);
    }

    public synchronized CMSSignedMessage addSignature (final CMSSignedMessage cmsMessage) throws Exception {
        return new CMSSignedMessage(cmsGenerator.addSignature(cmsMessage));
    }

    public CMSDto validateCMS(CMSSignedMessage cmsSignedMessage, ContentType contenType) throws Exception {
        if (cmsSignedMessage.isValidSignature() != null) {
            MessagesVS messages = MessagesVS.getCurrentInstance();
            Query query = dao.getEM().createNamedQuery("findcmsMessageByBase64ContentDigest")
                    .setParameter("base64ContentDigest", cmsSignedMessage.getContentDigestStr());
            CMSMessage cmsMessage = dao.getSingleResult(CMSMessage.class, query);
            if(cmsMessage != null) throw new ExceptionVS(messages.get("cmsDigestRepeatedErrorMsg",
                    cmsSignedMessage.getContentDigestStr()));
            CMSDto cmsDto = validateSignersCerts(cmsSignedMessage);
            TypeVS typeVS = TypeVS.OK;
            if(ContentType.CURRENCY == contenType) typeVS = TypeVS.CURRENCY;
            cmsMessage = dao.persist(new CMSMessage(cmsSignedMessage, cmsDto, typeVS));
            CMSMessage.setCurrentInstance(cmsMessage);
            cmsDto.setCmsMessage(cmsMessage);
            return cmsDto;
        } else throw new ValidationException("invalid CMSMessage");
    }

    public CMSDto validatedVote(CMSSignedMessage cmsSignedMessage) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        Query query = dao.getEM().createNamedQuery("findcmsMessageByBase64ContentDigest")
                .setParameter("base64ContentDigest", cmsSignedMessage.getContentDigestStr());
        CMSMessage cmsMessage = dao.getSingleResult(CMSMessage.class, query);
        if(cmsMessage != null) throw new ExceptionVS(messages.get("cmsDigestRepeatedErrorMsg",
                    cmsSignedMessage.getContentDigestStr()));
        Vote vote = cmsSignedMessage.getVote();
        CMSDto cmsDto = new CMSDto(vote);
        if(vote == null || vote.getX509Certificate() == null) throw new ExceptionVS(
                messages.get("documentWithoutSignersErrorMsg"));
        if (vote.getRepresentativeURL() != null) {
            query = dao.getEM().createQuery("select u from User u where u.url =:userURL")
                    .setParameter("userURL", vote.getRepresentativeURL());
            User checkedSigner = dao.getSingleResult(User.class, query);
            if(checkedSigner == null) checkedSigner = dao.persist(User.REPRESENTATIVE(vote.getRepresentativeURL()));
            cmsDto.setSigner(checkedSigner);
        }
        query = dao.getEM().createQuery("select e from EventVS e where e.accessControlEventId =:eventId and " +
                "e.accessControl.serverURL =:serverURL").setParameter("eventId", vote.getAccessControlEventId())
                .setParameter("serverURL", vote.getAccessControlURL());
        EventElection eventVS = dao.getSingleResult(EventElection.class, query);
        if(eventVS == null) throw new ExceptionVS(messages.get("voteEventElectionUnknownErrorMsg",
                vote.getAccessControlURL(), vote.getAccessControlEventId()));
        if(eventVS.getState() != EventVS.State.ACTIVE)
            throw new ExceptionVS(messages.get("electionClosed", eventVS.getSubject()));
        cmsDto.setEventVS(eventVS);
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS);
        timeStampBean.validateToken(vote.getTimeStampToken());
        X509Certificate checkedCert = vote.getX509Certificate();
        PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                eventTrustedAnchors, false, Arrays.asList(checkedCert));
        validatorResult.getTrustAnchor().getTrustedCert();
        cmsDto.setCmsMessage(dao.persist(new CMSMessage(cmsSignedMessage, cmsDto, TypeVS.SEND_VOTE)));
        return cmsDto;
    }

    public CMSDto validatedVoteFromControlCenter(CMSSignedMessage cmsSignedMessage) throws Exception {
        CMSDto cmsDto = validatedVote(cmsSignedMessage);
        Query query = dao.getEM().createQuery("select c from Certificate c where c.hashCertVSBase64 =:hashCertVS and c.state =:state")
                .setParameter("hashCertVS", cmsDto.getVote().getHashCertVSBase64()).setParameter("state", Certificate.State.OK);
        Certificate certificate = dao.getSingleResult(Certificate.class, query);
        if (certificate == null) {
            cmsDto.getCmsMessage().setType(TypeVS.ERROR).setReason("missing Vote Certificate");
            dao.merge(cmsDto.getCmsMessage());
            throw new ValidationException("missing Vote Certificate");
        }
        cmsDto.getCmsMessage().getCMS().getVote().setCertificate(certificate);
        return cmsDto;
    }
    
    public PKIXCertPathValidatorResult validateCertificates(List<X509Certificate> certificateList) throws ExceptionVS {
        log.log(Level.FINE, "validateCertificates");
        return CertUtils.verifyCertificate(getTrustAnchors(), false, certificateList);
        //X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
    }

    public CMSDto validateSignersCerts(CMSSignedMessage cmsSignedMessage) throws Exception {
        Set<User> signersVS = cmsSignedMessage.getSigners();
        if(signersVS.isEmpty()) throw new ExceptionVS("documentWithoutSignersErrorMsg");
        String signerNIF = null;
        if(cmsSignedMessage.getSigner().getNif() != null) signerNIF =
                org.votingsystem.util.NifUtils.validate(cmsSignedMessage.getSigner().getNif());
        CMSDto cmsDto = new CMSDto();
        for(User user: signersVS) {
            if(user.getTimeStampToken() != null) timeStampBean.validateToken(user.getTimeStampToken());
            else log.info("signature without timestamp - signer: " + user.getX509Certificate().getSubjectDN());
            verifyUserCertificate(user);
            if(user.isAnonymousUser()) {
                log.log(Level.FINE, "validateSignersCerts - is anonymous signer");
                cmsDto.setAnonymousSigner(user);
            } else {
                user = subscriptionBean.checkUser(user);
                if(user.getNif().equals(signerNIF)) cmsDto.setSigner(user);
                else cmsDto.addSigner(user);
            }
        }
        return cmsDto;
    }

    public PKIXCertPathValidatorResult verifyUserCertificate(User user) throws Exception {
        PKIXCertPathValidatorResult validatorResult = CertUtils.verifyCertificate(
                getTrustAnchors(), false, Arrays.asList(user.getX509Certificate()));
        X509Certificate certCaResult = validatorResult.getTrustAnchor().getTrustedCert();
        user.setCertificateCA(getTrustedCertsHashMap().get(certCaResult.getSerialNumber().longValue()));
        if(anonymousCertIssuers.contains(certCaResult.getSerialNumber().longValue()) &&
                Boolean.valueOf(CertUtils.getCertExtensionData(user.getX509Certificate(), ContextVS.ANONYMOUS_CERT_OID))) {
            user.setAnonymousUser(true);
        }
        log.log(Level.FINE, "verifyCertificate - user:" + user.getNif() + " cert issuer: " + certCaResult.getSubjectDN() +
                " - CA certificate.id : " + user.getCertificateCA().getId());
        return validatorResult;
    }

    //issues certificates if the request is signed with an Id card
    public X509Certificate signCSRSignedWithIDCard(CMSMessage cmsReq) throws Exception {
        User signer = cmsReq.getUser();
        if(signer.getCertificate().getType() != Certificate.Type.USER_ID_CARD)
                throw new Exception("Service available only for ID CARD signed requests");
        UserCertificationRequestDto requestDto = cmsReq.getSignedContent(UserCertificationRequestDto.class);
        PKCS10CertificationRequest csr = PEMUtils.fromPEMToPKCS10CertificationRequest(requestDto.getCsrRequest());
        CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                CertExtensionDto.class, csr, ContextVS.DEVICE_OID);
        String validatedNif = NifUtils.validate(certExtensionDto.getNif());
        Address address = signer.getAddress();
        if(address == null) {
            address = dao.persist(requestDto.getAddress());
            signer.setAddress(address);
            dao.merge(signer);
        } else {
            address.update(requestDto.getAddress());
            dao.merge(address);
        }
        dao.merge(signer);
        Date validFrom = new Date();
        Date validTo = DateUtils.addDays(validFrom, 365).getTime(); //one year
        X509Certificate issuedCert = signCSR(csr, null, validFrom, validTo);
        Certificate certificate = dao.persist(Certificate.ISSUED_USER_CERT(signer, issuedCert, serverCertificate));
        Query query = dao.getEM().createQuery("select d from Device d where d.deviceId =:deviceId and d.user.nif =:nif ")
                .setParameter("deviceId", certExtensionDto.getDeviceId()).setParameter("nif", validatedNif);
        Device device = dao.getSingleResult(Device.class, query);
        if(device == null) {
            device = dao.persist(new Device(signer, certExtensionDto.getDeviceId(), certExtensionDto.getEmail(),
                    certExtensionDto.getMobilePhone(), certExtensionDto.getDeviceType()).setState(Device.State.OK)
                    .setCertificate(certificate));
        } else {
            dao.merge(device.getCertificate().setState(Certificate.State.CANCELED).setCmsMessage(cmsReq));
            dao.merge(device.setEmail(certExtensionDto.getEmail()).setPhone(certExtensionDto.getMobilePhone())
                    .setCertificate(certificate));
        }
        dao.getEM().createQuery("UPDATE UserToken SET state=:state WHERE user=:user").setParameter(
                "state", UserToken.State.CANCELLED).setParameter("user", signer).executeUpdate();
        dao.persist(new UserToken(signer, requestDto.getToken(), certificate, cmsReq));
        log.info("signCertUser - issued new Certificate id: " + certificate.getId() + " for device: " + device.getId());
        return issuedCert;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        return trustAnchors;
    }

    public Set<X509Certificate> getTrustedCerts() {
        return trustedCerts;
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        return encryptor.encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, PublicKey publicKey) throws Exception {
        return encryptor.encryptToCMS(dataToEncrypt, publicKey);
    }

    public byte[] decryptCMS (byte[] encryptedMessageBytes) throws Exception {
        return encryptor.decryptCMS(encryptedMessageBytes);
    }

    public List<X509Certificate> getCertChain() {
        return certChain;
    }

}
