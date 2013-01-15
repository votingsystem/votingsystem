package pruebas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.KeyUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailValidator.ValidationResult;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.seguridad.CertUtil;


public class ValidateSignatureFromCsrSignedCert {

    private static Logger logger = LoggerFactory.getLogger(ValidateSignatureFromCsrSignedCert.class);

    
    public static void main(String[] args) {
        System.out.println("validarMensaje");
        try {
            Contexto.inicializar();
            //PKIXParameters params = Contexto.getHttpHelper().obtenerPKIXParametersDeServidor("http://localhost:8080/SistemaVotacionControlAcceso");
            PKIXParameters params = obtenerPKIXParametersFromFile(CsrTest.cadenaVerisign);

        
            byte[] bytes = FileUtils.getBytesFromFile(new File(CsrTest.keyStoreFirmaVotosSignedFilePath));
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(
            		null, new ByteArrayInputStream(bytes), null);
            ValidationResult validationResult = null;
            if (dnieMimeMessage != null) {
                System.out.println("signatura valida?" + SMIMEMessageWrapper.isValidSignature(dnieMimeMessage.getSmimeSigned()));
                System.out.println("Contenido: " + dnieMimeMessage.getSignedContent());
                validationResult = dnieMimeMessage.verify(params);
                System.out.println("validationResult.isVerifiedSignature: " + validationResult.isVerifiedSignature());
                System.out.println("validationResult.isValidSignature: " + validationResult.isValidSignature());
            }
        } catch(Exception ex){
            logger.error(ex.getMessage(), ex);
        }
    }
    
    private static PKIXParameters obtenerPKIXParametersFromFile(String rutaArchivoCadenaVerif) throws Exception {
       Collection<X509Certificate> certificados = CertUtil.fromPEMChainToX509Certs(
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
