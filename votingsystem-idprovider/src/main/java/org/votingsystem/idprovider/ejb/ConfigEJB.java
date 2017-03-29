package org.votingsystem.idprovider.ejb;


import eu.europa.esig.dss.test.mock.MockServiceInfo;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.x509.CertificateToken;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.votingsystem.crypto.KeyGenerator;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.dto.metadata.MetadataUtils;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.SystemEntityType;
import org.votingsystem.model.Certificate;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;
import org.votingsystem.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Class that encapsulates most of the logic related to configuration data
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class ConfigEJB implements Config, ConfigIdProvider {

    private static final Logger log = Logger.getLogger(ConfigEJB.class.getName());

    public static final String DEFAULT_APP_HOME = "/var/local/votingsystem-idprovider";
    public static final Integer DEFAULT_METADATA_LIVE_IN_HOURS = 1;

    @PersistenceContext
    private EntityManager em;
    @EJB private TrustedServicesEJB trustedServices;
    @EJB private CertIssuerEJB certIssuer;

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
    private Map<String, MetadataDto> entityMap;
    private Set<X509Certificate> trustedTimeStampServers;
    private String ocspServerURL;


    @PostConstruct
    public void initialize() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            org.apache.xml.security.Init.init();
            KeyGenerator.INSTANCE.init(Constants.SIG_NAME, Constants.PROVIDER, Constants.KEY_SIZE, Constants.ALGORITHM_RNG);
            HttpConn.init(HttpConn.HTTPS_POLICY.ALL, null);
            entityMap = new ConcurrentHashMap<>();

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
            while(aliases.hasMoreElements()) {
                String certAlias = aliases.nextElement();
                X509Certificate certificate = (X509Certificate) trustedCertsKeyStore.getCertificate(certAlias);
                loadAuthorityCertificate(new CertificateToken(certificate));
            }
            signingToken = new JKSSignatureToken(new FileInputStream(applicationDirPath + "/sec/" + keyStoreFileName),
                    keyStorePassword);
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
            trustedTimeStampServers = new HashSet<>();
        trustedTimeStampServers.add(trustedTimeStampIssuer);
    }

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
            log.info(" --------- shutdown ---------");
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

    public void putEntityMetadata(MetadataDto metadata) {
        entityMap.put(metadata.getEntity().getId(), metadata);
    }

    public MetadataDto getEntityMetadata(String entityId) {
        MetadataDto metadata = entityMap.get(entityId);
        if(metadata != null) {
            if (metadata.getValidUntilDate().isAfter(LocalDateTime.now())) {
                log.info("metadata expired - entityId: " + entityId);
                entityMap.remove(entityId);
                return null;
            }
        }
        return metadata;
    }

    public Set<X509Certificate> getTrustedTimeStampServers() {
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