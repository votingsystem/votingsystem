package pruebas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class CATest {
    
    private static Logger logger = (Logger) LoggerFactory.getLogger(CATest.class);
        
    public static void main (String[] args) throws Exception {
        Contexto.inicializar(); 
        cargarAutoridadesCertificadoras();
        validarCertificadoUsuarioDNIe();
        //extraerCertificadosDNIe();
        //fromCrtToPEM();
    }
    
    public static void validarCertificadoUsuarioDNIe () throws Exception {
        logger.debug("validarCertificadoUsuarioDNIe");
        InputStream inUsuarioDNIe =  Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("usuarioDNIe.pem");
        X509Certificate certUsu = CertUtil.fromPEMToX509Cert(FileUtils.getBytesFromInputStream(inUsuarioDNIe));
        logger.debug("certUsu: " + certUsu.getSubjectDN().toString());
        Set<X509Certificate> trustedCerts = cargarAutoridadesCertificadoras();
        verifyCertificate(certUsu, trustedCerts);
    }
    
    public static void fromCrtToPEM () throws Exception {
        File crtCertFile  = new File("/home/jgzornoza/Descargas/ACDNIE001-SHA1.crt");
        X509Certificate crtCert = CertUtil.loadCertificateFromStream(new FileInputStream(crtCertFile));
        logger.debug("crtCert: " + crtCert.getSubjectDN().toString()+
                "- numserie: " + crtCert.getSerialNumber().longValue());
        byte[] pemCert = CertUtil.fromX509CertToPEM(crtCert);
        logger.debug("pemCert: '" + new String(pemCert) + "'");
    }
    
    public static void verifyCertificate(X509Certificate cert, 
            Set<X509Certificate> trustedCerts) {
        try {
            PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                    cert, trustedCerts, false);
            // Get the CA used to validate this path
            TrustAnchor ta = pkixResult.getTrustAnchor();
            X509Certificate certCaResult = ta.getTrustedCert();
            logger.debug("certCaResult: " + certCaResult.getSubjectDN().toString()+
                    "- numserie: " + certCaResult.getSerialNumber().longValue());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public static Set<X509Certificate> cargarAutoridadesCertificadoras() throws Exception {
        String baseDir = Thread.currentThread().getContextClassLoader().
                getResource(".").getFile();
        logger.debug("baseDir: " + baseDir);
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();
        File fileDir = new File(baseDir);
        File[] acFiles = fileDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String fileName) {
                        return fileName.startsWith("AC_") && fileName.endsWith(".pem");
                }
          });
        for(File file:acFiles) {
            logger.debug("File CA: " + file.getName());
            X509Certificate certAC = CertUtil.fromPEMToX509Cert(FileUtils.getBytesFromFile(file));
            trustedCerts.add(certAC);
        }
        logger.debug("Cargadas " + trustedCerts.size() + " Autoridades Certificadoras");
        return trustedCerts;
    }
    
}
