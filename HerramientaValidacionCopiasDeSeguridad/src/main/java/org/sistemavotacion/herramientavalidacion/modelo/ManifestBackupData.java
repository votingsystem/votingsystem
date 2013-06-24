package org.sistemavotacion.herramientavalidacion.modelo;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sistemavotacion.modelo.MetaInf;
import org.sistemavotacion.seguridad.CertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestBackupData implements BackupData{
    
    private static Logger logger = LoggerFactory.getLogger(ManifestBackupData.class);
    
    private List<SignedFile> signatures = new ArrayList<SignedFile>();
    
    private byte[] systemTrustedCertsBytes;
    private byte[] timeStampCertBytes;   
    
    private Collection<X509Certificate> timeStampCerts;
    private Collection<X509Certificate> systemTrustedCerts;
    private MetaInf metaInf;    
    
    public String getFormattedInfo() {
        StringBuilder result = new StringBuilder("");
        result.append("\n - with systemTrustedCerts: " + 
                (systemTrustedCertsBytes != null)).append(
                "\n - Num. signed files: " + signatures.size());
        return result.toString();
    }

    public void addSignature(SignedFile signature) {
        if(signature == null) {
            logger.debug("signature null");
            return;
        } 
        signatures.add(signature);
    }

    /**
     * @return the metaInf
     */
    public MetaInf getMetaInf() {
        return metaInf;
    }

    /**
     * @param metaInf the metaInf to set
     */
    public void setMetaInf(MetaInf metaInf) {
        this.metaInf = metaInf;
    }

    @Override public void setSystemTrustedCerts(
            byte[] systemTrustedCertsBytes) throws Exception { 
        this.systemTrustedCertsBytes = systemTrustedCertsBytes;
        systemTrustedCerts = CertUtil.fromPEMToX509CertCollection(
                systemTrustedCertsBytes);
    }
    
    @Override
    public void setTimeStampCert(byte[] timeStampCertBytes) throws Exception {
        this.timeStampCertBytes = timeStampCertBytes;
        if(timeStampCertBytes != null)
            timeStampCerts = CertUtil.fromPEMToX509CertCollection(timeStampCertBytes);
    } 
    
    @Override public Collection<X509Certificate> getTimeStampCerts() {
        return timeStampCerts;
    }
    
}