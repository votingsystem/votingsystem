package org.votingsystem.idprovider.ejb;

import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.ejb.Config;
import org.votingsystem.ejb.SignerInfoService;
import org.votingsystem.ejb.TrustedServicesEJB;
import org.votingsystem.model.Certificate;
import org.votingsystem.ocsp.RootCertOCSPInfo;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.FileUtils;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@LocalBean
@Singleton
@Startup
@DependsOn("ConfigEJB")
public class StartUpEJB {

    private static final Logger log = Logger.getLogger(StartUpEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @EJB private TrustedServicesEJB trustedServices;
    @EJB private Config config;
    @EJB private CertIssuerEJB certIssuer;
    @EJB private SignerInfoService signerInfoService;

    @PostConstruct
    public void initialize() {
        try {
            trustedServices.loadTrustedServices();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        try {
            loadPreinstalledIssuedCerts();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Load already issued user certificates. This method is mainly for development.
     */
    private void loadPreinstalledIssuedCerts() throws Exception {
        File preinstalledIssuedCerts = new File(config.getApplicationDirPath() + "/sec/preinstalled-issued-certs.pem");
        Collection<X509Certificate> preInstalledCerts = PEMUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(preinstalledIssuedCerts));
        log.info("num. certificates: " + preInstalledCerts.size());
        RootCertOCSPInfo rootCertOCSPInfo = certIssuer.getRootCertOCSPInfo();
        for(X509Certificate certificate : preInstalledCerts) {
            Certificate caCertificate = signerInfoService.verifyCertificate(certificate);
            if(!rootCertOCSPInfo.getCertificate().getUUID().equals(caCertificate.getUUID()))
                throw new ValidationException(MessageFormat.format("Error with authority certificate of ''{0}''",
                        certificate.getSubjectDN()));
            List<Certificate> certificates = em.createQuery("SELECT c FROM Certificate c WHERE c.UUID=:UUID ")
                    .setParameter("UUID", CertificateUtils.getHash(certificate)).getResultList();
            if(certificates.isEmpty()) {
                Certificate updatedCert = Certificate.ISSUED_USER_CERT(null, certificate, rootCertOCSPInfo.getCertificate());
                em.persist(updatedCert);
            }
        }
    }

}
