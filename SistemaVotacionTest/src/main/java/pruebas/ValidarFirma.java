package pruebas;

import com.sun.mail.util.BASE64DecoderStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.Security;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.util.Store;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailValidator.ValidationResult;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class ValidarFirma {
    
    private static Logger logger = (Logger) LoggerFactory.getLogger(ValidarFirma.class);
    

    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
    private static X509Certificate certificadoCA;
    
    public static void main(String args[]) {
            validarMensajeKeyStoreSinParametros();
            //validarMensajeKeyStore();
    }
    
    public static void validarMensaje () {
        logger.debug("validarMensaje");
        try {
            Security.addProvider(new BouncyCastleProvider());
            SMIMEMessageWrapper dniemm = new SMIMEMessageWrapper(null, 
            		new FileInputStream("signedKeystore.message"), null);
            //DNIeMimeMessage dniemm = DNIeMimeMessage.build(new FileInputStream("C:\\temp\\SistemaVotacionClientePublicacion\\signed.message"));

            //logger.debug("signatura valida?" + dniemm.isValidSignature());
            logger.debug("Contenido: " + dniemm.getSignedContent());
        } catch(Exception ex){
            logger.error(ex.getMessage(), ex);
        }
    }
    
     public static void validarMensajeKeyStoreSinParametros () {
        try {
            Security.addProvider(new BouncyCastleProvider());
            SMIMEMessageWrapper dniemm = new SMIMEMessageWrapper(
            		null, new FileInputStream("/home/jgzornoza/temp/Signedtemp1"), null);
               //DNIeMimeMessage dniemm = DNIeMimeMessage.build(new FileInputStream("C:\\temp\\SistemaVotacionClientePublicacion\\signed.message"));

               //logger.debug("signatura valida?" + dniemm.isValidSignature());
               logger.debug("Contenido: " + dniemm.getSignedContent());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
            
     
     }
    
    public static void validarMensajeKeyStore () {
        logger.debug("validarMensaje");
        try {
            Security.addProvider(new BouncyCastleProvider());
            X509Certificate rootDNIe = Contexto.getHttpHelper().obtenerCertificadoDeServidor(
                    "http://localhost:8082/SistemaVotacionControlAcceso/certificado/raizDNIe");
            X509Certificate intermedioCA = Contexto.getHttpHelper().obtenerCertificadoDeServidor(
                    "http://localhost:8082/SistemaVotacionControlAcceso/certificado/intermedioCA");            

            X509Certificate rootCA = Contexto.getHttpHelper().obtenerCertificadoDeServidor(
                        "http://localhost:8082/SistemaVotacionControlAcceso/certificado/raizCA");
            
            TrustAnchor anchorDNIe = new TrustAnchor(rootDNIe, null);
            TrustAnchor anchorRootCA = new TrustAnchor(rootCA, null);
            TrustAnchor anchorInter = new TrustAnchor(intermedioCA, null);
            Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
            anchors.add(anchorInter);
            //anchors.add(anchorRootCA);
            anchors.add(anchorDNIe);
            //PKIXParameters params = new PKIXParameters(anchors);
            //PKIXParameters params = DNIeMimeMessage.getPKIXParameters(rootDNIe);
            
            PKIXParameters params = Contexto.getHttpHelper().obtenerPKIXParametersDeServidor("http://localhost:8082/SistemaVotacionControlAcceso");
            Security.addProvider(new BouncyCastleProvider());
            //DNIeMimeMessage dniemm = DNIeMimeMessage.build(new FileInputStream("signedKeystore.message"));
            //
            
            byte[] bytes = FileUtils.getBytesFromFile(new File("C:\\temp\\Validado.txt"));
            SMIMEMessageWrapper dnieMimeMessage = new SMIMEMessageWrapper(
            		null, new ByteArrayInputStream(bytes), null);
            
            //DNIeMimeMessage dnieMimeMessage = DNIeMimeMessage.build(new FileInputStream("C:\\temp\\Validado.txt"));
            logger.debug("dniemm: " + dnieMimeMessage);
            ValidationResult validationResult = null;
            if (dnieMimeMessage != null) {
                //logger.debug("signatura valida?" + dnieMimeMessage.isValidSignature());
                logger.debug("Contenido: " + dnieMimeMessage.getSignedContent());
                validationResult = dnieMimeMessage.verify(params);
                logger.debug("validationResult.isVerifiedSignature: " + validationResult.isVerifiedSignature());
                logger.debug("validationResult.isValidSignature: " + validationResult.isValidSignature());
            }

            logger.debug("dniemm: " + dnieMimeMessage);
        } catch(Exception ex){
            logger.error(ex.getMessage(), ex);
        }
    }
    

    
    
    public static void main1(String args[]) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            //
            // Get a Session object with the default properties.
            //
            Properties props = System.getProperties();

            Session session = Session.getDefaultInstance(props, null);

            // read message
            SMIMEMessageWrapper msg = new SMIMEMessageWrapper(session, new FileInputStream("signedKeystore.message"), null);
           // MimeMessage msg = new MimeMessage(session, new FileInputStream("C:\\temp\\SistemaVotacionClientePublicacion\\signed.message"));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
   
            
            msg.writeTo(out);
            
            logger.debug("Tipo contenido:" + msg.getContent().getClass());
            

            MimeMultipart mimeMultipart;
            SMIMESigned smimeSigned; 
            if (msg.getContent() instanceof BASE64DecoderStream) {
                smimeSigned = new SMIMESigned(msg); 
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
   

                logger.debug("baos.toString():" + baos.toString());
                
            } else smimeSigned = new SMIMESigned((MimeMultipart)msg.getContent());

            MimeBodyPart content = smimeSigned.getContent();
            
            logger.debug("Content:");
            Object  cont = content.getContent();
            if (cont instanceof String) {
                logger.debug("Is String:" + (String)cont);
            }

            else if (cont instanceof Multipart){
                Multipart   mp = (Multipart)cont;
                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    BodyPart    m = mp.getBodyPart(i);
                    Object      part = m.getContent();

                    logger.debug("Part " + i);
                    logger.debug("---------------------------");

                    if (part instanceof String) {
                        logger.debug((String)part);
                    }
                    else  {
                        logger.debug("can't print...");
                    }
                }
            }

            logger.debug("Status:");

            verify(smimeSigned);
        
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

    }
    
     // extract the information to verify the signatures.
    private static void verify(SMIMESigned s) throws Exception {
        // certificates and crls passed in the signature
        Store certs = s.getCertificates();

        //
        // SignerInfo blocks which contain the signatures
        //
        SignerInformationStore  signers = s.getSignerInfos();

        Collection              c = signers.getSigners();
        Iterator                it = c.iterator();

        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());

            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC).getCertificate((X509CertificateHolder)certIt.next());

            //
            // verify that the sig is correct and that it was generated
            // when the certificate was current
            //
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert)))
            {
                logger.debug("signature verified");
            }
            else
            {
                logger.debug("signature failed!");
            }
        }
    }
}
