package pruebas;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.smime.SignedMailGenerator.Type;
import org.sistemavotacion.smime.SignedMailValidator;
import org.sistemavotacion.smime.SignedMailValidator.ValidationResult;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiFirmaTest {
        
    private static Logger logger = (Logger) LoggerFactory.getLogger(MultiFirmaTest.class);
    
    public static final String keyAlias1 = "aliascertfirmado";
    public static final String password1 = "PemPass";
    public static final String keyStorePath1 = "/host/temp/KeyStoreFirmaVoto.jks";
    
    public static final String keyAlias2 = "clavessistemavotacion";
    public static final String password2 = "PemPass";
    public static final String keyStorePath2 = "/host/temp/KeyStoreVerisign.jks";
    
    public static final String filePath1 = "/host/temp/firmaConCSR";
    //public static final String multiFirmaFilePath = "/host/temp/multifirma";
    
    public static final String multiFirmaFilePath = "/home/jgzornoza/Descargas/EventoEnviadoMultifirma.p7m";
    
    public static final String pathCadenaCertCSR = "/host/temp/cadenaCSR.pem";
    public static final String pathCadenaCertVerisign = "/host/temp/cadenaVerisign.pem";
    
    public static void main (String[] args) throws Exception {
        Contexto.inicializar();
        //generarMultifirma();
        validateMultiSignedMail();
    }
 

    
    public static void generarMultifirma () {
        try {
            File keyStoreFile = new File(keyStorePath2);
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    FileUtils.getBytesFromFile(keyStoreFile), keyAlias2, password2.toCharArray(),
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
            byte[] bytes = FileUtils.getBytesFromFile(new File(filePath1));
            //DNIeMimeMessage dnieMimeMessage = DNIeMimeMessage.build(new ByteArrayInputStream(bytes), null);
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null,new ByteArrayInputStream(bytes), null);
            MimeMultipart mimeMultipart = (MimeMultipart)dnieMimeMessage.getContent();
            logger.debug("mimeMultipart.getCount: " + mimeMultipart.getCount());
            MimeBodyPart bodyPart = (MimeBodyPart) mimeMultipart.getBodyPart(0);
            
            
            
            MimeMultipart signer2MimeMultipar = signedMailGenerator.genMimeMultipart(bodyPart, 
                dnieMimeMessage,  SignedMailGenerator.Type.CONTROL_CENTER, 
                ContextoPruebas.VOTE_SIGN_MECHANISM);
            
            logger.debug("signer2MimeMultipar.getCount: " + signer2MimeMultipar.getCount());
            
            //signer2MimeMultipar.addBodyPart((MimeBodyPart) mimeMultipart.getBodyPart(1));
            
            ///mimeMultipart.addBodyPart((MimeBodyPart)signer2MimeMultipar.getBodyPart(1));
            logger.debug("mimeMultipart.getCount: " + signer2MimeMultipar.getCount());
            //Session session = null;
            //MimeMessage mm = new MimeMessage(session);
            //dnieMimeMessage.setContent(signer2MimeMultipar, signer2MimeMultipar.getContentType());
            //dnieMimeMessage.writeTo(new FileOutputStream(multiFirmaFilePath));
            
            
            Properties props = System.getProperties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage body = new MimeMessage(session);
            body.setSubject("Ejemplo multifirma");
            body.setContent(signer2MimeMultipar, signer2MimeMultipar.getContentType());
            body.saveChanges();
            body.writeTo(new FileOutputStream(multiFirmaFilePath));
            
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } 
    }
    
    public static void validateMultiSignedMail () {
        //Mientras no descubra nada mejor salgo así del paso
        //Creando un params para cada actor y extrayendolo la parte firmada
        // hacer : dnieMimeMessage.verifyUser
        // hacer : dnieMimeMessage.verifyControlAcceso
        // hacer : dnieMimeMessage.verifyCentroControl
        logger.debug("validateMultiSignedMail");
        try {
            Contexto.inicializar();
            //PKIXParameters params = obtenerPKIXParametersFromFile(pathCadenaCertVerisign);
            byte[] bytes = FileUtils.getBytesFromFile(new File(multiFirmaFilePath));
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(null,new ByteArrayInputStream(bytes), null);
            boolean isValidSignature = SMIMEMessageWrapper.isValidSignature(dnieMimeMessage.getSmimeSigned());            
            logger.debug("dnieMimeMessage.getSignedContent(): '" + dnieMimeMessage.getSignedContent() + "'");
            logger.debug("isValidSignature: " + isValidSignature);
            /*DNIeMimeMessage dnieMimeMessage = DNIeMimeMessage.build(new ByteArrayInputStream(bytes), null);
            MimeMultipart mimeMultipart = (MimeMultipart)dnieMimeMessage.getContent();
            BodyPart bodyContenido = null; 
            BodyPart bodyUsuario = null;
            BodyPart bodyCentroControl = null;
            BodyPart bodyControlAcceso = null;
            for(int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                SignedMailGenerator.Type tipo = checkContentDisposition(bodyPart.getHeader("Content-Disposition"));
                if (tipo!= null) {
                    switch(tipo) {
                        case ACESS_CONTROL:
                            bodyControlAcceso = bodyPart;
                            break;
                        case USER:
                            bodyUsuario = bodyPart;
                            break;
                        case CONTROL_CENTER:
                            bodyCentroControl = bodyPart;
                            break;
                    }
                } else {
                    bodyContenido = bodyPart;
                }
            }
            ValidationResult validationResult = null;
            if (bodyUsuario != null) validationResult = validarFirmaUsuario(dnieMimeMessage.getSmimeSigned());
            //if (bodyUsuario != null) validationResult = validarFirmaUsuario1(bodyContenido, bodyCentroControl);
            //if (bodyCentroControl != null) validarFirmaControlAcceso(bodyContenido, bodyCentroControl);
            //if (bodyControlAcceso != null) validarFirmaCentroControl(bodyContenido, bodyControlAcceso);
            if (validationResult != null) {
                logger.debug("signatura valida?" + dnieMimeMessage.isValidSignature(dnieMimeMessage.getSmimeSigned()));
                logger.debug("Contenido: " + dnieMimeMessage.getSignedContent());
                validationResult = dnieMimeMessage.verify(params);
                logger.debug("validationResult.isVerifiedSignature: " + validationResult.isVerifiedSignature());
                logger.debug("validationResult.isValidSignature: " + validationResult.isValidSignature());
            }
            */

        } catch(Exception ex){
            logger.error(ex.getMessage(), ex);
        }
    }
    
    private static ValidationResult validarFirmaUsuario (SMIMESigned smimeSigned) 
            throws MessagingException, CMSException, Exception {
        logger.debug("validarFirmaUsuario");
        String pathCadenaCertCSR = "/host/temp/cadenaCSR.pem";
        PKIXParameters params = obtenerPKIXParametersFromFile(pathCadenaCertCSR); 
        

        //SMIMESigned smimeSigned = new SMIMESigned(null);
        //Store certs = smimeSigned.getCertificates();
        //Collection certCollection = certs.getMatches(signer.getSID());
        boolean isValidSignature = SMIMEMessageWrapper.isValidSignature(smimeSigned);
        logger.debug("isValidSignature: " + isValidSignature);
        
        //DNIeSignedMailValidator signedMailValidator = new DNIeSignedMailValidator(clonedMessage, params);
        //return DNIeMimeMessage.verify(smimeSigned);
        return null;
    }
    
    private static ValidationResult validarFirmaUsuario1 (BodyPart contenido, 
            BodyPart firmaUsuario) 
            throws MessagingException, CMSException, Exception {
        logger.debug("validarFirmaUsuario");
        
        String pathCadenaCertCSR = "/host/temp/cadenaCSR.pem";
        PKIXParameters params = obtenerPKIXParametersFromFile(pathCadenaCertCSR);   
        MimeMultipart mimeMultipart= new MimeMultipart();
        mimeMultipart.addBodyPart(contenido, 0);
        mimeMultipart.addBodyPart(firmaUsuario, 1);
        //mimeMultipart.writeTo(System.out);
        //Prueba
        //clonedMessage.setContent(mimeMultipart);
        //clonedMessage.writeTo(System.out);

        SMIMESigned smimeSigned = new SMIMESigned(mimeMultipart);
        boolean isValidSignature = SMIMEMessageWrapper.isValidSignature(smimeSigned);
        logger.debug("isValidSignature: " + isValidSignature);
        //

        //DNIeSignedMailValidator signedMailValidator = new DNIeSignedMailValidator(clonedMessage, params);
        //return DNIeMimeMessage.verify(signedMailValidator);
        return null;
    }
    
    private static ValidationResult validarFirma(
            BodyPart contenido, BodyPart firma, PKIXParameters params) 
            throws MessagingException, CMSException, Exception {
        logger.debug("validarFirmaUsuario");
        String pathCadenaCertCSR = "/host/temp/cadenaVerisign.pem"; 
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(null, null));
        MimeMultipart mimeMultipart= new MimeMultipart();
        mimeMultipart.addBodyPart(contenido, 0);
        mimeMultipart.addBodyPart(firma, 1);
        mimeMessage.setContent(mimeMultipart);
        SignedMailValidator signedMailValidator = new SignedMailValidator(mimeMessage, params);
        return signedMailValidator.verify();
    }
    
    private static void validarFirmaControlAcceso (BodyPart contenido, BodyPart firmaControlAcceso) 
            throws MessagingException, CMSException, Exception {
        logger.debug("validarFirmaUsuario");
        String pathCadenaCertCSR = "/host/temp/cadenaVerisign.pem";
        PKIXParameters params = obtenerPKIXParametersFromFile(pathCadenaCertCSR);
        MimeMultipart mimeMultipart= new MimeMultipart();
        mimeMultipart.addBodyPart(contenido, 0);
        mimeMultipart.addBodyPart(firmaControlAcceso, 1);
        SMIMESigned smimeSigned = new SMIMESigned(mimeMultipart);
    }
    
    private static void validarFirmaCentroControl (BodyPart contenido, BodyPart firmaCentroControl) 
            throws MessagingException, CMSException, Exception {
        logger.debug("validarFirmaUsuario");
        String pathCadenaCertCSR = "/host/temp/cadenaVerisign.pem";
        PKIXParameters params = obtenerPKIXParametersFromFile(pathCadenaCertCSR);         
        MimeMultipart mimeMultipart= new MimeMultipart();
        mimeMultipart.addBodyPart(contenido, 0);
        mimeMultipart.addBodyPart(firmaCentroControl, 1);
        SMIMESigned smimeSigned = new SMIMESigned(mimeMultipart);        
    }
    
    private static PKIXParameters obtenerPKIXParametersFromFile(String pathCadena) throws Exception {
       Collection<X509Certificate> certificados = CertUtil.fromPEMChainToX509Certs(
               FileUtils.getBytesFromFile(new File(pathCadena)));
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for (X509Certificate certificado:certificados) {
            TrustAnchor anchorCertificado = new TrustAnchor(certificado, null);
            anchors.add(anchorCertificado);
        }
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false); // tell system do not check CRL's
        return params;
    }

    private static Type checkContentDisposition(String[] header) {
        if (header == null) return null;
        String contentDisposition = header[0];
        String fileNameField = contentDisposition.split("filename=\"")[1];
        String fileNameValue = null;
        Type result = null;
        if (fileNameField != null) {
            fileNameValue = fileNameField.split("\"")[0];
            if (fileNameValue != null) {
                result = Type.valueOf(fileNameValue.split(
                        Contexto.SIGNED_PART_EXTENSION)[0]);
            }
        }
        return result;
    }

}