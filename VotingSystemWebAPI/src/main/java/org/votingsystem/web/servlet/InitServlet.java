package org.votingsystem.web.servlet;

import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.web.ejb.SignatureBean;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * Servlet to load resources from war
 */
public class InitServlet extends HttpServlet{

    private static Logger log = Logger.getLogger(InitServlet.class.getSimpleName());

    @Inject SignatureBean signatureBean;

    @Override public void init() throws ServletException {
        try {
            List<X509Certificate> fileSystemX509TrustedCerts = new ArrayList<>();
            List<UserVS> admins = new ArrayList<>();
            for (String res : getServletContext().getResourcePaths("WEB-INF/votingsystem/certs")) {
                String resFileName = res.split("WEB-INF/votingsystem/certs/")[1];
                log.info("checking resource: " + res + " - resFileName: " + resFileName);
                if(resFileName.startsWith("AC_") && resFileName.endsWith(".pem")) {
                    X509Certificate fileSystemX509TrustedCert = CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(
                            getServletContext().getResourceAsStream(res)));
                    fileSystemX509TrustedCerts.add(fileSystemX509TrustedCert);
                } else if(resFileName.startsWith("ADMIN_") && resFileName.endsWith(".pem")) {
                    X509Certificate adminCert = CertUtils.fromPEMToX509Cert(FileUtils.getBytesFromStream(
                            getServletContext().getResourceAsStream(res)));
                    UserVS userVS = UserVS.getUserVS(adminCert);
                    admins.add(userVS);
                }
            }
            signatureBean.initCertAuthorities(fileSystemX509TrustedCerts);
            signatureBean.initAdmins(admins);

            URL res = getServletContext().getResource("/bower_components/polymer/polymer.js");
            if(res == null) {
                log.log(Level.SEVERE, "Have you executed 'bower install' from web-app dir ???");
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

}