package org.votingsystem.web.ejb;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreInfo;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.RequestRepeatedException;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.Header;
import javax.persistence.Query;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
public class SignatureBean {

    private static Logger log = Logger.getLogger(SignatureBean.class.getSimpleName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    @Inject TimeStampBean timeStampBean;
    @Inject SubscriptionVSBean subscriptionVSBean;
    @Inject MessagesBean messages;
    private SMIMESignedGeneratorVS signedMailGenerator;
    private Encryptor encryptor;
    private Set<TrustAnchor> trustAnchors;
    private Set<TrustAnchor> currencyAnchors;
    private Set<X509Certificate> trustedCerts;
    private PrivateKey serverPrivateKey;
    private CertificateVS serverCertificateVS;
    private X509Certificate localServerCertSigner;
    private List<X509Certificate> certChain;
    private byte[] keyStorePEMCerts;
    private Map<Long, CertificateVS> trustedCertsHashMap = new HashMap<>();
    private static final HashMap<Long, Set<TrustAnchor>> eventTrustedAnchorsMap = new HashMap<Long, Set<TrustAnchor>>();
    private Set<String> admins;
    private UserVS systemUser;
    private String password;
    private String keyAlias;
    private String serverName;

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
        signedMailGenerator = new SMIMESignedGeneratorVS(FileUtils.getBytesFromFile(keyStoreFile),
                keyAlias, password.toCharArray(), ContextVS.SIGN_MECHANISM);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
        certChain = new ArrayList<>();
        for(java.security.cert.Certificate certificate : keyStore.getCertificateChain(keyAlias)) {
            checkAuthorityCertDB((X509Certificate) certificate);
            certChain.add((X509Certificate) certificate);
        }
        keyStorePEMCerts = CertUtils.getPEMEncoded (certChain);
        localServerCertSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        currencyAnchors = new HashSet<>();
        currencyAnchors.add(new TrustAnchor(localServerCertSigner, null));
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumber")
                .setParameter("serialNumber", localServerCertSigner.getSerialNumber().longValue());
        serverCertificateVS = dao.getSingleResult(CertificateVS.class, query);
        serverPrivateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        encryptor = new Encryptor(localServerCertSigner, serverPrivateKey);
        serverName = config.getServerName();
    }

    public boolean isUserAdmin(String nif) {
        return admins.contains(nif);
    }

    public void setAdmins(Set<String> admins) {
        this.admins = admins;
    }

    public UserVS getSystemUser() {
        return systemUser;
    }

    public byte[] getKeyStorePEMCerts() {
        return keyStorePEMCerts;
    }

    public Set<TrustAnchor> getCurrencyAnchors() {
        return currencyAnchors;
    }

    public KeyStoreInfo getKeyStoreInfo(byte[] keyStoreBytes, String keyAlias) throws Exception {
        KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password.toCharArray());
        PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());
        X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(keyAlias);
        return new KeyStoreInfo(keyStore, privateKeySigner, certSigner);
    }

    public void initAdmins(List<UserVS> admins) throws Exception {
        Query query = dao.getEM().createNamedQuery("findUserByType").setParameter("type", UserVS.Type.SYSTEM);
        systemUser = dao.getSingleResult(UserVS.class, query);
        Set<String> adminsNIF = new HashSet<>();
        for(UserVS userVS:admins) {
            verifyUserCertificate(userVS);
            userVS = subscriptionVSBean.checkUser(userVS);
            adminsNIF.add(userVS.getNif());
        }
        systemUser.updateAdmins(adminsNIF);
        dao.merge(systemUser);
        log.info("initAdmins - admins list:" + adminsNIF);
        setAdmins(adminsNIF);
    }

    public void initCertAuthorities(List<X509Certificate> resourceCerts) throws Exception {
        log.info("initCertAuthorities - resourceCerts.size: " + resourceCerts.size());
        for(X509Certificate fileSystemX509TrustedCert:resourceCerts) {
            checkAuthorityCertDB(fileSystemX509TrustedCert);
        }
        Query query = dao.getEM().createNamedQuery("findCertByStateAndType")
                .setParameter("type", CertificateVS.Type.CERTIFICATE_AUTHORITY)
                .setParameter("state", CertificateVS.State.OK);
        List<CertificateVS>  trustedCertsList = query.getResultList();
        trustedCertsHashMap = new HashMap<>();
        trustedCerts = new HashSet<>();
        trustAnchors = new HashSet<>();
        for (CertificateVS certificateVS : trustedCertsList) {
            addCertAuthority(certificateVS);
        }
    }

    public void addCertAuthority(CertificateVS certificateVS) throws Exception {
        X509Certificate x509Cert = certificateVS.getX509Cert();
        trustedCerts.add(x509Cert);
        trustedCertsHashMap.put(x509Cert.getSerialNumber().longValue(), certificateVS);
        trustAnchors.add(new TrustAnchor(x509Cert, null));
        log.info("certificateVS.id: " + certificateVS.getId() + " - " + x509Cert.getSubjectDN() +
                " - num. trustedCerts: " + trustedCerts.size());
    }

    private CertificateVS checkAuthorityCertDB(X509Certificate x509AuthorityCert) throws CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException, ExceptionVS {
        log.info(x509AuthorityCert.getSubjectDN().toString());
        Query query = dao.getEM().createNamedQuery("findCertBySerialNumberAndType")
                .setParameter("type", CertificateVS.Type.CERTIFICATE_AUTHORITY)
                .setParameter("serialNumber", x509AuthorityCert.getSerialNumber().longValue());
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(certificateVS == null) {
            certificateVS = new CertificateVS(CertUtils.isSelfSigned(x509AuthorityCert),
                    CertificateVS.Type.CERTIFICATE_AUTHORITY, CertificateVS.State.OK, null, x509AuthorityCert.getEncoded(),
                    x509AuthorityCert.getSerialNumber().longValue(), x509AuthorityCert.getNotBefore(),
                    x509AuthorityCert.getNotAfter());
            dao.persist(certificateVS);
            log.info("ADDED NEW FILE SYSTEM CA CERT - certificateVS.id:" + certificateVS.getId());
        } else if (CertificateVS.State.OK != certificateVS.getState()) {
            throw new ExceptionVS("File system athority cert: " + x509AuthorityCert.getSubjectDN() + " }' " +
                    " - certificateVS.id: " + certificateVS.getId() + " - state:" + certificateVS.getState());
        } else if(certificateVS.getType() != CertificateVS.Type.CERTIFICATE_AUTHORITY) {
            String msg = "Updated from type " + certificateVS.getType() + " to type 'CERTIFICATE_AUTHORITY'";
            certificateVS.setDescription(certificateVS.getDescription() + "###" + msg);
            certificateVS.setType(CertificateVS.Type.CERTIFICATE_AUTHORITY);
            dao.merge(certificateVS);
        }
        return certificateVS;
    }

    public void validateVoteCerts(SMIMEMessage smimeMessage, EventVS eventVS) throws Exception {
        Set<UserVS> signersVS = smimeMessage.getSigners();
        if(signersVS.isEmpty()) throw new ExceptionVS("ERROR - document without signers");
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS);
        for(UserVS userVS: signersVS) {
            CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                    eventTrustedAnchors, false, Arrays.asList(userVS.getCertificate()));
            X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
        }
    }

    public Set<TrustAnchor> getEventTrustedAnchors(EventVS eventVS) throws Exception {
        Set<TrustAnchor> eventTrustedAnchors = eventTrustedAnchorsMap.get(eventVS.getId());
        if(eventTrustedAnchors == null) {
            Query query = dao.getEM().createQuery("select c from CertificateVS c where c.eventVS =:eventVS and " +
                    "c.state =:state and c.type =:type").setParameter("eventVS", eventVS)
                    .setParameter("state", CertificateVS.State.OK).setParameter("type", CertificateVS.Type.VOTEVS_ROOT);
            CertificateVS eventCACert = dao.getSingleResult(CertificateVS.class, query);
            X509Certificate certCAEventVS = eventCACert.getX509Cert();
            eventTrustedAnchors = new HashSet<TrustAnchor>();
            eventTrustedAnchors.add(new TrustAnchor(certCAEventVS, null));
            eventTrustedAnchors.addAll(getTrustAnchors());
            eventTrustedAnchorsMap.put(eventVS.getId(), eventTrustedAnchors);
        }
        return eventTrustedAnchors;
    }

    public MessageSMIME processSMIMERequest(SMIMEMessage smimeMessage, ContentTypeVS contenType) throws Exception {
        if (smimeMessage.isValidSignature()) {
            SMIMECheck smimeCheck = null;
            switch(contenType) {
                //case CURRENCY: break;
                case VOTE:
                    smimeCheck = validateSMIMEVote(smimeMessage);
                    break;
                default:
                    smimeCheck = validateSMIME(smimeMessage);
            }
            TypeVS typeVS = TypeVS.OK;
            if(contenType != null && ContentTypeVS.CURRENCY == contenType) typeVS = TypeVS.CURRENCY;
            return dao.persist(new MessageSMIME(smimeMessage, smimeCheck, typeVS));
        } else throw new ValidationExceptionVS("invalid SMIMEMessage");
    }

    public boolean isSignerCertificate(Set<UserVS> signers, X509Certificate cert) throws CertificateEncodingException {
        for(UserVS userVS : signers) {
            if(Arrays.equals(userVS.getCertificate().getEncoded(), cert.getEncoded())) return true;
        }
        return false;
    }

    public Set<X509Certificate> getEventTrustedCerts(EventVS eventVS) throws Exception {
        Query query = dao.getEM().createQuery("select c from CertificateVS c where c.eventVS =:eventVS and " +
                "c.state =:state and c.type =:type").setParameter("eventVS", eventVS)
                .setParameter("state", CertificateVS.State.OK).setParameter("type", CertificateVS.Type.VOTEVS_ROOT);
        CertificateVS eventVSCertificateVS = dao.getSingleResult(CertificateVS.class, query);
        if(eventVSCertificateVS == null) throw new ValidationExceptionVS(
                "ERROR - eventWithoutCAErrorMsg - EventVS id: " + eventVS.getId());
        Set<X509Certificate> eventTrustedCerts = new HashSet<X509Certificate>();
        eventTrustedCerts.add(CertUtils.loadCertificate(eventVSCertificateVS.getContent()));
        return eventTrustedCerts;
    }

    public KeyStoreVS generateElectionKeysStore(EventVS eventVS) throws Exception {
        Query query = dao.getEM().createQuery("select k from KeyStoreVS k where k.valid =:valid " +
                "and k.eventVS =:eventVS").setParameter("valid", Boolean.TRUE).setParameter("eventVS", eventVS);
        KeyStoreVS keyStoreVS = dao.getSingleResult(KeyStoreVS.class, query);
        if (keyStoreVS != null) throw new ExceptionVS("ERROR EventVS with KeyStoreVS associated - EventVS id:" +
                eventVS.getId() + " - KeyStoreVS id: " + keyStoreVS.getId());
        //StringUtils.getRandomAlphaNumeric(7).toUpperCase()
        // _ TODO _ ====== crypto token
        String eventVSUrl = config.getRestURL() + "/eventVS/id/" + eventVS.getId();
        String strSubjectDNRoot = format("CN=eventVSUrl:{0}, OU=Elections", eventVSUrl);
        KeyStore keyStore = KeyStoreUtil.createRootKeyStore(eventVS.getDateBegin(), eventVS.getDateFinish(),
                password.toCharArray(), keyAlias, strSubjectDNRoot);
        java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        java.security.cert.Certificate cert = chain[0];
        CertificateVS certificateVS = dao.persist(new CertificateVS((X509Certificate) cert, eventVS,
                CertificateVS.Type.VOTEVS_ROOT, CertificateVS.State.OK));
        keyStoreVS = dao.persist(new KeyStoreVS (Boolean.TRUE, Boolean.TRUE, keyAlias, eventVS, eventVS.getDateBegin(),
                eventVS.getDateFinish(), KeyStoreUtil.getBytes(keyStore, password.toCharArray())));
        keyStoreVS.setCertificateVS(certificateVS);
        return keyStoreVS;
    }

    public void cancel (EventVS eventVS) throws ValidationExceptionVS {
        Query query = dao.getEM().createQuery("select k from KeyStoreVS k where k.eventVS =:eventVS and k.valid =:valid")
                .setParameter("eventVS", eventVS).setParameter("valid", Boolean.TRUE);
        KeyStoreVS keyStoreVS = dao.getSingleResult(KeyStoreVS.class, query);
        if (keyStoreVS == null) throw new ValidationExceptionVS(
                "ERROR - keyStoreVS not found - EventVS id: " + eventVS.getId());
        keyStoreVS.setValid(Boolean.FALSE);
        dao.merge(keyStoreVS);
    }

    public KeyStore generateUserTestKeysStore(String givenName, String surname, String nif,
                                              String userPassword) throws Exception {
        log.info("generateUserTestKeysStore - nif: " + nif);
        Date validFrom = Calendar.getInstance().getTime();
        Calendar today_plus_year = Calendar.getInstance();
        today_plus_year.add(Calendar.YEAR, 1);
        today_plus_year.set(Calendar.HOUR_OF_DAY, 0);
        today_plus_year.set(Calendar.MINUTE, 0);
        today_plus_year.set(Calendar.SECOND, 0);
        Date validTo = today_plus_year.getTime();
        X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(localServerCertSigner,
                serverPrivateKey, keyAlias);
        String testUserDN = format("GIVENNAME={0}, SURNAME={1} , SERIALNUMBER={2}", givenName, surname, nif);
        //String strSubjectDN = "CN=Voting System Cert Authority , OU=VotingSystem"
        //KeyStore rootCAKeyStore = KeyStoreUtil.createRootKeyStore (validFrom.getTime(), (validTo.getTime() - validFrom.getTime()),
        //        userPassword.toCharArray(), keyAlias, strSubjectDN);
        //X509Certificate certSigner = (X509Certificate)rootCAKeyStore.getCertificate(keyAlias);
        //PrivateKey privateKeySigner = (PrivateKey)rootCAKeyStore.getKey(keyAlias, userPassword.toCharArray());
        //X500PrivateCredential rootCAPrivateCredential = new X500PrivateCredential(certSigner, privateKeySigner,  keyAlias);
        return KeyStoreUtil.createUserKeyStore(validFrom.getTime(),
                (validTo.getTime() - validFrom.getTime()), userPassword.toCharArray(), ContextVS.KEYSTORE_USER_CERT_ALIAS,
                rootCAPrivateCredential, testUserDN);
    }

    public X509Certificate getServerCert() {
        return localServerCertSigner;
    }

    public CertificateVS getServerCertificateVS() {
        return serverCertificateVS;
    }

    private PrivateKey getServerPrivateKey() {
        return serverPrivateKey;
    }

    private Map<Long, CertificateVS> getTrustedCertsHashMap() {
        return trustedCertsHashMap;
    }

    public CertUtils.CertValidatorResultVS verifyCertificate(X509Certificate certToValidate) throws Exception {
        return CertUtils.verifyCertificate(getTrustAnchors(), false, Arrays.asList(certToValidate));
    }

    public boolean isSystemSignedMessage(Set<UserVS> signers) {
        for(UserVS userVS: signers) {
            if(userVS.getCertificate().equals(localServerCertSigner)) return true;
        }
        return false;
    }

    public X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, Date dateBegin,
                                   Date dateFinish, DERTaggedObject... certExtensions) throws Exception {
        X509Certificate issuedCert = CertUtils.signCSR(csr, organizationalUnit, getServerPrivateKey(),
                getServerCert(), dateBegin, dateFinish, certExtensions);
        return issuedCert;
    }

    public SMIMEMessage getSMIME (String fromUser,String toUser,String textToSign,String subject, Header header) throws Exception {
        log.info("getSMIME - subject: " + subject + "  - fromUser:" + fromUser + " to user: " + toUser);
        return getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject, header);
    }

    public SMIMEMessage getSMIMETimeStamped (String fromUser,String toUser,String textToSign,String subject,
                                             Header... headers) throws Exception {
        log.info("getSMIMETimeStamped - subject:" + subject + " - fromUser: " + fromUser + " to user: " + toUser);
        SMIMEMessage smimeMessage = getSignedMailGenerator().getSMIME(fromUser, toUser, textToSign, subject, headers);
        return timeStampBean.timeStampSMIME(smimeMessage);
    }

    public synchronized SMIMEMessage getSMIMEMultiSigned (
            String fromUser, String toUser,	final SMIMEMessage smimeMessage, String subject) throws Exception {
        log.info("getSMIMEMultiSigned - subject:" + subject + " - fromUser: " + fromUser + " to user: " + toUser);
        return getSignedMailGenerator().getSMIMEMultiSigned(fromUser, toUser, smimeMessage, subject);
    }

    public synchronized SMIMEMessage getSMIMEMultiSigned (String toUser, final SMIMEMessage smimeMessage,
                                                          String subject) throws Exception {
        return getSignedMailGenerator().getSMIMEMultiSigned(serverName, toUser, smimeMessage, subject);
    }

    public SMIMECheck validateSMIME(SMIMEMessage smimeMessage) throws Exception {
        Query query = dao.getEM().createNamedQuery("findMessageSMIMEByBase64ContentDigest")
                .setParameter("base64ContentDigest", smimeMessage.getContentDigestStr());
        MessageSMIME messageSMIME = dao.getSingleResult(MessageSMIME.class, query);
        if(messageSMIME != null) throw new ExceptionVS("'smimeDigestRepeatedErrorMsg'");
        return validateSignersCerts(smimeMessage);
    }

    public SMIMECheck validateSMIMEVote(SMIMEMessage smimeMessage) throws Exception {
        Query query = dao.getEM().createNamedQuery("findMessageSMIMEByBase64ContentDigest")
                .setParameter("base64ContentDigest", smimeMessage.getContentDigestStr());
        MessageSMIME messageSMIME = dao.getSingleResult(MessageSMIME.class, query);
        if(messageSMIME != null) throw new ExceptionVS("smimeDigestRepeatedErrorMsg");
        VoteVS voteVS = smimeMessage.getVoteVS();
        SMIMECheck result = new SMIMECheck(voteVS);
        if(voteVS == null || voteVS.getX509Certificate() == null) throw new ExceptionVS(
                messages.get("documentWithoutSignersErrorMsg"));
        if (voteVS.getRepresentativeURL() != null) {
            query = dao.getEM().createQuery("select u from UserVS u where u.url =:userURL")
                    .setParameter("userURL", voteVS.getRepresentativeURL());
            UserVS checkedSigner = dao.getSingleResult(UserVS.class, query);
            if(checkedSigner == null) checkedSigner = dao.persist(UserVS.REPRESENTATIVE(voteVS.getRepresentativeURL()));
            result.setSigner(checkedSigner);
        }
        query = dao.getEM().createQuery("select e from EventVS e where e.accessControlEventVSId =:eventId")
                .setParameter("eventId", voteVS.getEventVS().getId());
        EventVS eventVS = dao.getSingleResult(EventVS.class, query);
        if(eventVS == null) throw new ExceptionVS(messages.get("voteEventVSElectionUnknownErrorMsg"));
        if(eventVS.getState() != EventVS.State.ACTIVE)
            throw new ExceptionVS(messages.get("electionClosed", eventVS.getSubject()));
        query = dao.getEM().createQuery("select c from CertificateVS c where c.hashCertVSBase64 =:hashCertVS")
                .setParameter("hashCertVS", voteVS.getHashCertVSBase64());
        CertificateVS certificateVS = dao.getSingleResult(CertificateVS.class, query);
        if (certificateVS != null) {
            String url = config.getRestURL() + "/voteVS/certificateVS/id/" + certificateVS.getId();
            throw new RequestRepeatedException(messages.get("voteRepeatedErrorMsg", certificateVS.getId()), url);
        }
        Set<TrustAnchor> eventTrustedAnchors = getEventTrustedAnchors(eventVS);
        timeStampBean.validateToken(voteVS.getTimeStampToken());
        X509Certificate checkedCert = voteVS.getX509Certificate();
        CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                eventTrustedAnchors, false, Arrays.asList(checkedCert));
        X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
        return result;
    }
    
    public CertUtils.CertValidatorResultVS validateCertificates(List<X509Certificate> certificateList) throws ExceptionVS {
        log.log(Level.FINE, "validateCertificates");
        return CertUtils.verifyCertificate(getTrustAnchors(), false, certificateList);
        //X509Certificate certCaResult = validationResponse.data.pkixResult.getTrustAnchor().getTrustedCert();
    }

    public SMIMECheck validateSignersCerts(SMIMEMessage smimeMessage) throws Exception {
        Set<UserVS> signersVS = smimeMessage.getSigners();
        if(signersVS.isEmpty()) throw new ExceptionVS("documentWithoutSignersErrorMsg");
        CertUtils.CertValidatorResultVS validatorResult = null;
        String signerNIF = org.votingsystem.util.NifUtils.validate(smimeMessage.getSigner().getNif());
        SMIMECheck result = new SMIMECheck();
        for(UserVS userVS: signersVS) {
            timeStampBean.validateToken(userVS.getTimeStampToken());
            validatorResult = verifyUserCertificate(userVS);
            if(validatorResult.getChecker().isAnonymousSigner()) {
                log.log(Level.FINE, "validateSignersCerts - is anonymous signer");
                result.setAnonymousSigner(userVS);
            } else {
                UserVS user = subscriptionVSBean.checkUser(userVS);
                if(user.getNif().equals(signerNIF)) result.setSigner(user);
                else result.addSigner(user);
            }
        }
        return result;
    }

    public CertUtils.CertValidatorResultVS verifyUserCertificate(UserVS userVS) throws Exception {
        CertUtils.CertValidatorResultVS validatorResult = CertUtils.verifyCertificate(
                getTrustAnchors(), false, Arrays.asList(userVS.getCertificate()));
        X509Certificate certCaResult = validatorResult.getResult().getTrustAnchor().getTrustedCert();
        userVS.setCertificateCA(getTrustedCertsHashMap().get(certCaResult.getSerialNumber().longValue()));
        log.log(Level.FINE, "verifyCertificate - user:" + userVS.getNif() + " cert issuer: " + certCaResult.getSubjectDN() +
                " - CA certificateVS.id : " + userVS.getCertificateCA().getId());
        return validatorResult;
    }

    public Set<TrustAnchor> getTrustAnchors() {
        return trustAnchors;
    }

    public Set<X509Certificate> getTrustedCerts() {
        return trustedCerts;
    }
    public byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptToCMS(dataToEncrypt, receiverCert);
    }

    public byte[] encryptToCMS(byte[] dataToEncrypt, PublicKey receptorPublicKey) throws Exception {
        return getEncryptor().encryptToCMS(dataToEncrypt, receptorPublicKey);
    }

    public byte[] decryptCMS (byte[] encryptedFile) throws Exception {
        return getEncryptor().decryptCMS(encryptedFile);
    }

    public byte[] encryptMessage(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        return getEncryptor().encryptMessage(bytesToEncrypt, publicKey);
    }

    public byte[] encryptMessage(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptMessage(bytesToEncrypt, receiverCert);
    }

    public ResponseVS decryptMessage (byte[] encryptedFile) throws Exception {
        return getEncryptor().decryptMessage(encryptedFile);
    }

    ResponseVS encryptSMIME(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        return getEncryptor().encryptSMIME(bytesToEncrypt, receiverCert);
    }

    ResponseVS decryptSMIME(byte[] encryptedMessageBytes) throws Exception {
        return getEncryptor().decryptSMIME(encryptedMessageBytes);
    }

    private Encryptor getEncryptor() {
        return encryptor;
    }

    private SMIMESignedGeneratorVS getSignedMailGenerator() {
        return signedMailGenerator;
    }

    public List<X509Certificate> getCertChain() {
        return certChain;
    }
}