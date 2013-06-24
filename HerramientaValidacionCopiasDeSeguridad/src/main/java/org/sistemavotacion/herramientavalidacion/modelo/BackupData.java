package org.sistemavotacion.herramientavalidacion.modelo;

import java.security.cert.X509Certificate;
import java.util.Collection;
import org.sistemavotacion.modelo.MetaInf;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public interface BackupData {
    
    public String getFormattedInfo();
    public void setMetaInf(MetaInf metainf);
    public void setSystemTrustedCerts(byte[] systemTrustedCertsBytes) throws Exception;
    public void setTimeStampCert(byte[] timeStampCertBytes) throws Exception;
    public Collection<X509Certificate> getTimeStampCerts();
    
}
