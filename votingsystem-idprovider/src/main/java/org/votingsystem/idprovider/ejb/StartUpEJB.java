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


    @EJB private TrustedServicesEJB trustedServices;

    @PostConstruct
    public void initialize() {
        try {
            trustedServices.loadTrustedServices();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }


}
