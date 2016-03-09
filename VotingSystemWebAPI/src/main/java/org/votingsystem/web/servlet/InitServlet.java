package org.votingsystem.web.servlet;

import org.votingsystem.model.UserVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.ejb.CMSBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * Servlet to load resources from war
 */
public class InitServlet extends HttpServlet{

    private static Logger log = Logger.getLogger(InitServlet.class.getName());

    @Inject CMSBean cmsBean;
    @Inject ConfigVS config;

    @Override public void init() throws ServletException {
        try {
            MessagesVS.setCurrentInstance(Locale.getDefault(), config.getProperty("vs.bundleBaseName"));
            List<X509Certificate> fileSystemCATrustedCerts = new ArrayList<>();
            List<X509Certificate> fileSystemCA_anonymous_provider_TrustedCerts = new ArrayList<>();
            List<UserVS> admins = new ArrayList<>();
            for (String res : getServletContext().getResourcePaths("WEB-INF/votingsystem/certs")) {
                String resFileName = res.split("WEB-INF/votingsystem/certs/")[1];
                log.info("checking resource: " + res + " - resFileName: " + resFileName);
                if(resFileName.startsWith("AC_") && resFileName.endsWith(".pem")) {
                    X509Certificate fileSystemX509TrustedCert = PEMUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(
                            getServletContext().getResourceAsStream(res)));
                    fileSystemCATrustedCerts.add(fileSystemX509TrustedCert);
                    if(resFileName.contains("ANONYMOUS_CERT_PROVIDER")) {
                        fileSystemCA_anonymous_provider_TrustedCerts.add(fileSystemX509TrustedCert);
                    }
                } else if(resFileName.startsWith("ADMIN_") && resFileName.endsWith(".pem")) {
                    X509Certificate adminCert = PEMUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(
                            getServletContext().getResourceAsStream(res)));
                    UserVS userVS = UserVS.FROM_X509_CERT(adminCert);
                    admins.add(userVS);
                }
            }
            cmsBean.initAnonymousCertAuthorities(fileSystemCA_anonymous_provider_TrustedCerts);
            cmsBean.initCertAuthorities(fileSystemCATrustedCerts);
            cmsBean.initAdmins(admins);
            config.mainServletInitialized();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("--- InitServlet broken: " + ex.getMessage());
        }
    }

}