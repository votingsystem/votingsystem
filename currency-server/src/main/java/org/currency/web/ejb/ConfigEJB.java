package org.currency.web.ejb;

import eu.europa.esig.dss.test.mock.MockServiceInfo;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.x509.CertificateSource;
import eu.europa.esig.dss.x509.CertificateToken;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.iban4j.*;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.KeyGenerator;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.MetadataUtils;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@javax.ejb.Singleton(name = "ConfigCurrencyServer")
@Startup
@Lock(LockType.READ)
public class ConfigEJB implements Config, ConfigCurrencyServer, Serializable {

    private static final Logger log = Logger.getLogger(ConfigEJB.class.getName());

    public static final String DEFAULT_APP_HOME = "/var/local/currency-server";
    public static final Integer DEFAULT_METADATA_LIVE_IN_HOURS = 1;

    @PersistenceContext
    private EntityManager em;
    @EJB private TrustedServicesEJB trustedServices;

    private String entityId;
    private String staticResourcesURL;
    private String webSocketURL;

    private String timestampServiceURL;
    //dir with web static content
    private String staticResourcesPath;

    //dir with configuration data
    private String applicationDirPath;
    //dir with backup files
    private String applicationDataPath;
    private byte[] signatureCertChainPEMBytes;

    private String bankCode = null;
    private String  branchCode = null;

    private AbstractSignatureTokenConnection signingToken;
    private DSSPrivateKeyEntry dssPrivateKey;
    private X509Certificate signingCert;
    private TrustedListsCertificateSource trustedCertSource;
    private Map<Long, Certificate> trustedCACertsMap = new HashMap<>();
    private Map<String, MetadataDto> entityMap;
    private Map<String, User> adminMap = new HashMap<>();
    private MetadataDto metadata;
    private Set<TrustAnchor> trustedCertAnchors;
    private Map<Long, X509Certificate> trustedTimeStampServers;

    private String ocspServerURL;
    private User systemUser;


    @PostConstruct
    public void initialize() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            org.apache.xml.security.Init.init();
            KeyGenerator.INSTANCE.init(Constants.SIG_NAME, Constants.PROVIDER, Constants.KEY_SIZE, Constants.ALGORITHM_RNG);
            HttpConn.init(HttpConn.HTTPS_POLICY.ALL, null);

            entityMap = new ConcurrentHashMap<>();
            applicationDirPath = System.getProperty("currency_server_dir");
            if(StringUtils.isEmpty(applicationDirPath))
                applicationDirPath = DEFAULT_APP_HOME;

            Collection<X509Certificate> adminCertificates = CertificateUtils.loadCertificatesFromFolder(
                    new File(applicationDirPath + "/sec/admins"));
            for(X509Certificate adminCert : adminCertificates) {
                User admin = User.FROM_CERT(adminCert, User.Type.USER);
                adminMap.put(CertificateUtils.getHash(adminCert), admin);
            }

            Properties properties = new Properties();
            File propertiesFile = new File(applicationDirPath + "/config.properties");
            properties.load(new FileInputStream(propertiesFile));
            String logLevel = ((String) properties.get("logLevel"));
            Level selectedLogLevel = Level.parse(logLevel);
            Logger log = LogManager.getLogManager().getLogger("org.votingsystem");
            for (Handler handler : log.getHandlers()) {
                handler.setLevel(selectedLogLevel);
            }
            timestampServiceURL = OperationType.TIMESTAMP_REQUEST.getUrl((String)properties.get("timestampServerURL"));

            entityId = (String)properties.get("entityId");
            ocspServerURL = entityId + "/ocsp";
            webSocketURL = entityId + "/websocket/service";
            applicationDataPath = (String)properties.get("applicationData");
            staticResourcesURL = (String)properties.get("staticResourcesURL");
            staticResourcesPath = (String)properties.get("staticResourcesPath");
            bankCode = (String)properties.get("IBAN_bankCode");
            branchCode = (String)properties.get("IBAN_branchCode");

            log.info("entityId: " + entityId + " - applicationDirPath: " + applicationDirPath +
                    " - ocspServerURL: " + ocspServerURL + " - webSocketURL: " + webSocketURL +
                    " - selectedLogLevel: " + selectedLogLevel + " - timestampServiceURL: " + timestampServiceURL +
                    " - defaultMetadataLiveInHours: " + DEFAULT_METADATA_LIVE_IN_HOURS);
            new File(applicationDirPath).mkdirs();
            new File(staticResourcesPath + "/error").mkdirs();

            properties = new Properties();
            propertiesFile = new File(applicationDirPath + "/sec/keystore.properties");
            properties.load(new FileInputStream(propertiesFile));
            String keyStoreFileName = (String) properties.get("keyStoreFileName");
            String keyStorePassword = (String) properties.get("keyStorePassword");
            String trustedCACertskeyStoreFileName = (String) properties.get("trustedCACertskeyStoreFileName");
            String trustedCACertsKeyStorePassword = (String) properties.get("trustedCACertsKeyStorePassword");
            KeyStore trustedCertsKeyStore = KeyStore.getInstance("JKS");
            trustedCertsKeyStore.load(new FileInputStream(applicationDirPath + "/sec/" + trustedCACertskeyStoreFileName),
                    trustedCACertsKeyStorePassword.toCharArray());
            Enumeration<String> aliases = trustedCertsKeyStore.aliases();
            while(aliases.hasMoreElements()) {
                String certAlias = aliases.nextElement();
                X509Certificate certificate = (X509Certificate) trustedCertsKeyStore.getCertificate(certAlias);
                loadAuthorityCertificate(new CertificateToken(certificate));
            }
            signingToken = new JKSSignatureToken(new FileInputStream(applicationDirPath + "/sec/" + keyStoreFileName),
                    keyStorePassword);

            dssPrivateKey = signingToken.getKeys().get(0);
            signingCert = dssPrivateKey.getCertificate().getCertificate();

            CertificateToken[] certificateChain = dssPrivateKey.getCertificateChain();
            List<X509Certificate> certificateList = new ArrayList<>();
            for(CertificateToken certificateToken :  certificateChain) {
                certificateList.add(certificateToken.getCertificate());
            }
            signatureCertChainPEMBytes = PEMUtils.getPEMEncoded(certificateList);
            loadAuthorityCertificate(dssPrivateKey.getCertificate());

            List<User> userList = em.createNamedQuery(User.FIND_USER_BY_TYPE)
                    .setParameter("type", User.Type.CURRENCY_SERVER).getResultList();
            if(userList.isEmpty()) { //First time run;
                systemUser = new User(User.Type.CURRENCY_SERVER, entityId).setEntityId(entityId);
                em.persist(systemUser);
                createIBAN(systemUser);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public Certificate checkCACertificate(CertificateToken certificateToken) throws CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException {
        if(certificateToken.getIssuerToken() != null) {
            checkCACertificate(certificateToken.getIssuerToken());
        }
        X509Certificate x509Cert = certificateToken.getCertificate();
        List<Certificate> certificates = em.createQuery(
                "select c from Certificate c where c.serialNumber=:serialNumber and c.subjectDN=:subjectDN")
                .setParameter("serialNumber", x509Cert.getSerialNumber().longValue())
                .setParameter("subjectDN", x509Cert.getSubjectDN().toString()).getResultList();
        Certificate certificate = null;
        if(certificates.isEmpty()) {
            //TODO
            X500Name x500name = new JcaX509CertificateHolder(x509Cert).getSubject();
            Certificate.Type certType = null;
            for(RDN rdn : x500name.getRDNs()) {
                AttributeTypeAndValue attributeTypeAndValue = rdn.getFirst();
                if(BCStyle.O.getId().equals(attributeTypeAndValue.getType().getId())) {
                    if(attributeTypeAndValue.getValue().toString().contains("DIRECCION GENERAL DE LA POLICIA"))
                        certType = Certificate.Type.CERTIFICATE_AUTHORITY_ID_CARD;
                }
            }
            if(certType == null)
                certType = Certificate.Type.ENTITY;
            certificate = Certificate.AUTHORITY(x509Cert, certType, null);
            em.persist(certificate);
            log.info("Added new CA certificate to database - id: " + certificate.getId() + " - SubjectDN: " +
                    certificate.getSubjectDN());
        } else certificate = certificates.iterator().next();
        return certificate;
    }

    @PreDestroy private void shutdown() { log.info(" --------- shutdown ---------");}

    public String getIBAN(Long userId, String bankCodeStr, String branchCodeStr) {
        String accountNumberStr = String.format("%010d", userId);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCodeStr).branchCode(branchCodeStr)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        return iban.toString();
    }

    public String validateIBAN(String IBAN) throws IbanFormatException, InvalidCheckDigitException,
            UnsupportedCountryException {
        IbanUtil.validate(IBAN);
        return IBAN;
    }

    public String getTempDir() {
        return applicationDataPath + File.separator + "temp";
    }

    public String getWebSocketURL() {
        return webSocketURL;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public byte[] getSignatureCertChainPEMBytes() {
        return signatureCertChainPEMBytes;
    }

    public Map<String, MetadataDto> getEntityMap() {
        return entityMap;
    }

    public String getStaticResourcesURL() {
        return staticResourcesURL;
    }

    public String getApplicationDataPath() {
        return applicationDataPath;
    }

    @Override
    public MetadataDto getMetadata() {
        try {
            if(metadata == null) {
                Properties properties = new Properties();
                properties.load(new FileInputStream(new File(applicationDirPath + "/config.properties")));
                metadata = MetadataUtils.initMetadata(SystemEntityType.ID_PROVIDER, entityId, properties,
                        signingCert, signingCert);
                metadata.setTrustedEntities(trustedServices.getTrustedEntities());
            }
            metadata.setValidUntil(ZonedDateTime.now().plus(DEFAULT_METADATA_LIVE_IN_HOURS, ChronoUnit.HOURS)
                    .toInstant().toString());
            return metadata;
        } catch (Exception ex) {
            throw new RuntimeException(Messages.currentInstance().get("invalidMetadataMsg") + " - " + ex.getMessage());
        }
    }

    @Override
    public boolean isAdmin(User user) throws ValidationException {
        try {
            String certHash = CertificateUtils.getHash(user.getX509Certificate());
            if(adminMap.containsKey(certHash)) {
               return  CertificateUtils.equals(user.getX509Certificate(), adminMap.get(certHash).getX509Certificate());
            }
            return false;
        } catch (Exception ex) {
            throw new ValidationException(ex.getMessage());
        }

    }

    @Override
    public void putEntityMetadata(MetadataDto metadata) {
        entityMap.put(metadata.getEntity().getId(), metadata);
    }

    @Override
    public Certificate getCACertificate(Long certificateId) {
        return trustedCACertsMap.get(certificateId);
    }

    @Override
    public String getTimestampServiceURL() {
        return timestampServiceURL;
    }

    @Override
    public AbstractSignatureTokenConnection getSigningToken() {
        return signingToken;
    }

    @Override
    public CertificateSource getTrustedCertSource() {
        return trustedCertSource;
    }

    @Override
    public Map<Long, X509Certificate> getTrustedTimeStampServers() {
        return trustedTimeStampServers;
    }

    @Override
    public Set<TrustAnchor> getTrustedCertAnchors() {
        return trustedCertAnchors;
    }

    @Override
    public String getApplicationDirPath() {
        return applicationDirPath;
    }

    @Override
    public X509Certificate getSigningCert() {
        return signingCert;
    }

    @Override
    public String getEntityId() {
        return entityId;
    }

    @Override
    public void addTrustedTimeStampIssuer(X509Certificate trustedTimeStampIssuer) {
        if(trustedTimeStampServers == null)
            trustedTimeStampServers = new HashMap<>();
        trustedTimeStampServers.put(trustedTimeStampIssuer.getSerialNumber().longValue(), trustedTimeStampIssuer);
    }

    @Override
    public User getSystemUser() {
        return systemUser;
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public User createIBAN(User user) throws ValidationException {
        String accountNumberStr = String.format("%010d", user.getId());
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode(bankCode).branchCode(branchCode)
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        user.setIBAN(iban.toString());
        em.merge(user);
        em.persist(new CurrencyAccount(user, BigDecimal.ZERO, CurrencyCode.EUR));
        return user;
    }

    @Override
    public Certificate loadAuthorityCertificate(CertificateToken trustedCertificate) throws IOException, CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException {
        Certificate caCertificate = checkCACertificate(trustedCertificate);
        trustedCACertsMap.put(caCertificate.getSerialNumber(), caCertificate);
        log.log(Level.SEVERE, "TrustedListsCertificateSource with MockServiceInfo!!! - certificate: " +
                caCertificate.getSubjectDN());
        if(trustedCertSource == null) {
            trustedCertAnchors = new HashSet<>();
            trustedCertSource = new TrustedListsCertificateSource();
        }
        trustedCertAnchors.add(new TrustAnchor(trustedCertificate.getCertificate(), null));
        trustedCertSource.addCertificate(trustedCertificate, new MockServiceInfo());
        return caCertificate;
    }

    @Override
    public String getOcspServerURL() {
        return ocspServerURL;
    }

}