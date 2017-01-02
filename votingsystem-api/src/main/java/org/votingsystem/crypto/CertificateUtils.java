package org.votingsystem.crypto;

import org.votingsystem.util.FileUtils;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class to work with certificates
 *
 * @author votingsystem
 */
public class CertificateUtils {

    private static final Logger log = Logger.getLogger(CertificateUtils.class.getName());

    /**
     * Method that load all PEM formated certificates found on a folder
     *
     * @param certificatesFolder folder with the PEM certificates
     * @return  a list with all the PEM certificates found on the folder
     */
    public static Collection<X509Certificate> loadCertificatesFromFolder(File certificatesFolder) {
        File[] listOfFiles = certificatesFolder.listFiles();
        if(listOfFiles != null) {
            List<X509Certificate> trustedCerts = new ArrayList<>();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    if(listOfFiles[i].getName().toLowerCase().endsWith(".pem")) {
                        try{
                            Collection<X509Certificate> fileSystemX509TrustedCerts =
                                    PEMUtils.fromPEMToX509CertCollection(FileUtils.getBytesFromFile(listOfFiles[i]));
                            trustedCerts.addAll(fileSystemX509TrustedCerts);
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    } else log.log(Level.INFO, "found unexpected file name: " + listOfFiles[i].getAbsolutePath());
                } else if (listOfFiles[i].isDirectory()) {
                    log.log(Level.SEVERE, "found unexpected dir on certificates dir");
                }
            }
            return trustedCerts;
        } else return Collections.emptyList();

    }
}
