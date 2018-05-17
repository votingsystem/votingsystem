package org.votingsystem.idprovider.ejb;


import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.x509.CertificateToken;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Class that encapsulates most of the logic related to configuration data
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@javax.ejb.Singleton(name = "ConfigEJB")
@Startup
@Lock(LockType.READ)
public class ConfigEJB implements Config, ConfigIdProvider {

    private static final Logger log = Logger.getLogger(ConfigEJB.class.getName());

    public static final String DEFAULT_APP_HOME = "/var/local/wildfly/votingsystem-idprovider";
    public static final Integer DEFAULT_METADATA_LIVE_IN_HOURS = 1;

    @PersistenceContext
    private EntityManager em;
    @EJB private TrustedServicesEJB trustedServices;

    private String entityId;
    private String timestampServiceURL;
    private String applicationDirPath;
    private byte[] signatureCertChainPEMBytes;

    private AbstractSignatureTokenConnection signingToken;
    private DSSPrivateKeyEntry privateKey;
    private X509Certificate signingCert;
    private TrustedListsCertificateSource trustedCertSource;
    private Set<TrustAnchor> trustedCertAnchors;
    private Map<Long, Certificate> trustedCACertsMap = new HashMap<>();
    private MetadataDto metadata;
    private Map<Long, X509Certificate> trustedTimeStampServers;
    private String ocspServerURL;
    private Map<String, User> adminMap = new HashMap<>();

    @PostConstruct
    public void initialize() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            org.apache.xml.security.Init.init();
            KeyGenerator.INSTANCE.init(Constants.SIG_NAME, Constants.PROVIDER, Constants.KEY_SIZE, Constants.ALGORITHM_RNG);
            HttpConn.init(HttpConn.HTTPS_POLICY.ALL, null);

            Collection<X509Certificate> adminCertificates = CertificateUtils.loadCertificatesFromFolder(
                    new File(applicationDirPath + "/sec/admins"));
            for(X509Certificate adminCert : adminCertificates) {
                User admin = User.FROM_CERT(adminCert, User.Type.USER);
                adminMap.put(CertificateUtils.getHash(adminCert), admin);
            }

            applicationDirPath = System.getProperty("idprovider_server_dir");
            if(StringUtils.isEmpty(applicationDirPath))
                applicationDirPath = DEFAULT_APP_HOME;

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

            log.info("entityId: " + entityId + " - applicationDirPath: " + applicationDirPath
                    + " - selectedLogLevel: " + selectedLogLevel + " - timestampServiceURL: " + timestampServiceURL +
                    " - defaultMetadataLiveInHours: " + DEFAULT_METADATA_LIVE_IN_HOURS + " - ocspServerURL: " + ocspServerURL);

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

            Date currentDateUTC = DateUtils.getUTCDateNow();
            while(aliases.hasMoreElements()) {
                String certAlias = aliases.nextElement();
                X509Certificate certificate = (X509Certificate) trustedCertsKeyStore.getCertificate(certAlias);
                if(certificate.getNotAfter().before(currentDateUTC))
                    throw new Exception("Certificated expired: " + certificate.getSubjectDN());
                loadAuthorityCertificate(new CertificateToken(certificate));
            }
            signingToken = new JKSSignatureToken(new FileInputStream(applicationDirPath + "/sec/" + keyStoreFileName),
                    new KeyStore.PasswordProtection(keyStorePassword.toCharArray()));
            privateKey = signingToken.getKeys().get(0);
            signingCert = privateKey.getCertificate().getCertificate();

            KeyStore signatureKeyStore = KeyStore.getInstance("JKS");
            signatureKeyStore.load(new FileInputStream(applicationDirPath + "/sec/" + keyStoreFileName),
                    keyStorePassword.toCharArray());

            CertificateToken[] certificateChain = privateKey.getCertificateChain();
            List<X509Certificate> certificateList = new ArrayList<>();
            for(CertificateToken certificateToken :  certificateChain) {
                certificateList.add(certificateToken.getCertificate());
            }
            signatureCertChainPEMBytes = PEMUtils.getPEMEncoded(certificateList);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void addTrustedTimeStampIssuer(X509Certificate trustedTimeStampIssuer) {
        if(trustedTimeStampServers == null)
            trustedTimeStampServers = new HashMap<>();
        trustedTimeStampServers.put(trustedTimeStampIssuer.getSerialNumber().longValue(), trustedTimeStampIssuer);
    }

    public Certificate loadAuthorityCertificate(CertificateToken trustedCertificate) throws IOException, CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException {
        Certificate caCertificate = checkCACertificate(trustedCertificate);
        trustedCACertsMap.put(caCertificate.getSerialNumber(), caCertificate);
        log.log(Level.SEVERE, caCertificate.getSubjectDN());
        if(trustedCertSource == null) {
            trustedCertAnchors = new HashSet<>();
            trustedCertSource = new TrustedListsCertificateSource();
        }
        trustedCertAnchors.add(new TrustAnchor(trustedCertificate.getCertificate(), null));
        trustedCertSource.addCertificate(trustedCertificate, null);
        return caCertificate;
    }

    public TrustedListsCertificateSource getTrustedCertSource() {
        return trustedCertSource;
    }

    public Certificate checkCACertificate(CertificateToken certificateToken) throws CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException {
        if(certificateToken.getIssuerToken() != null) {
            checkCACertificate(certificateToken.getIssuerToken());
        }
        X509Certificate x509Cert = certificateToken.getCertificate();
        List<Certificate> certificates = em.createNamedQuery(Certificate.FIND_BY_SERIALNUMBER_AND_SUBJECT_DN)
                .setParameter("serialNumber", x509Cert.getSerialNumber().longValue())
                .setParameter("subjectDN", x509Cert.getSubjectDN().toString()).getResultList();
        Certificate certificate;
        if(certificates.isEmpty()) {
            //TODO
            Certificate.Type certType = Certificate.Type.CERTIFICATE_AUTHORITY_ID_CARD;
            certificate = Certificate.AUTHORITY(x509Cert, certType, null);
            em.persist(certificate);
            log.info("Added new CA certificate to database - id: " + certificate.getId() + " - SubjectDN: " +
                    certificate.getSubjectDN());
        } else certificate = certificates.iterator().next();
        return certificate;
    }

    public byte[] getSignatureCertPEMBytes() {
        return signatureCertChainPEMBytes;
    }

    @Override
    public Certificate getCACertificate(Long certificateId) {
        return trustedCACertsMap.get(certificateId);
    }

    @Override
    public AbstractSignatureTokenConnection getSigningToken() {
        return signingToken;
    }

    @Override
    public String getEntityId() {
        return entityId;
    }

    @Override
    public String getApplicationDirPath() {
        return applicationDirPath;
    }

    @Override
    public String getTimestampServiceURL() {
        return timestampServiceURL;
    }

    @PreDestroy
    private void shutdown() {
        try {
            if(HttpConn.getInstance() != null)
                HttpConn.getInstance().shutdown();
            log.info(" --------- shutdown idprovider ---------");
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public X509Certificate getSigningCert() {
        return signingCert;
    }

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
            return adminMap.containsKey(certHash);
        } catch (Exception ex) {
            throw new ValidationException(ex.getMessage());
        }

    }

    public Map<Long, X509Certificate> getTrustedTimeStampServers() {
        return trustedTimeStampServers;
    }

    public DSSPrivateKeyEntry getPrivateKey() {
        return privateKey;
    }

    public String getOcspServerURL() {
        return ocspServerURL;
    }

    public Set<TrustAnchor> getTrustedCertAnchors() {
        return trustedCertAnchors;
    }
}