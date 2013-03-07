package pruebas;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailValidator.ValidationResult;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 * 'Compañero' de CreatedSignedWithKeyStore
 */

public class ValidateSignedWithKeyStore {
        
    private static Logger logger = (Logger) LoggerFactory.getLogger(MultiFirmaTest.class);
    
    //public static String cadenaCA = "/host/temp/cadenaVerisign.pem";
    public static String cadenaCA = ContextoPruebas.APPDIR + "temp/cadenaCert.pem";
    
    //public static String signedFilePath = ContextoPruebas.APPDIR + "temp/mensajeSMIME.txt";
    public static String signedFilePath = ContextoPruebas.APPDIR + "temp/user.p7s";
         
    public static void main(String args[]) {
        validateMultiSignedMail();
    }
    
    public static void validateMultiSignedMail () {
        logger.debug("validateMultiSignedMail");
        try {
            Contexto.inicializar();
            PKIXParameters params = obtenerPKIXParametersFromFile(cadenaCA);
            //byte[] bytes = FileUtils.getBytesFromFile(new File(CreatedSignedWithKeyStore.signedFilePath));
            byte[] bytes = FileUtils.getBytesFromFile(new File(signedFilePath));
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(
            		null, new ByteArrayInputStream(bytes), null);
            ValidationResult validationResult = null;
            if (dnieMimeMessage != null) {
                logger.debug("signatura valida?" + dnieMimeMessage.isValidSignature());
                logger.debug("Contenido: " + dnieMimeMessage.getSignedContent());
                validationResult = dnieMimeMessage.verify(params);
                logger.debug("validationResult.isVerifiedSignature: " + validationResult.isVerifiedSignature());
                logger.debug("validationResult.isValidSignature: " + validationResult.isValidSignature());
                logger.debug("Asunto: " + dnieMimeMessage.getSubject());
                logger.debug("contenido de la firma: " + dnieMimeMessage.getSignedContent());
            }
        } catch(Exception ex){
            logger.error(ex.getMessage(), ex);
        }
    }
    
    private static PKIXParameters obtenerPKIXParametersFromFile(String rutaArchivoCadenaVerif) throws Exception {
       Collection<X509Certificate> certificados = CertUtil.fromPEMToX509CertCollection(
    		   FileUtils.getBytesFromFile(new File(rutaArchivoCadenaVerif)));
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for (X509Certificate certificado:certificados) {
            TrustAnchor anchorCertificado = new TrustAnchor(certificado, null);
            anchors.add(anchorCertificado);
        }
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false); // tell system do not check CRL's
        return params;
    }
}
