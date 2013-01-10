package pruebas;

import iaik.pkcs.pkcs11.Mechanism;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.util.Store;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.DNIeContentSigner;
import org.sistemavotacion.smime.DNIeSessionHelper;
import org.sistemavotacion.smime.SimpleSignerInfoGeneratorBuilder;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase para probar la firma con el DNIe
 */
public class CreateDNIeSignedMail {
    
    private static Logger logger = (Logger) LoggerFactory.getLogger(CsrTest.class);  
    
    private static final String directorioBase = "/home/jgzornoza/git/recursos/dnie/";

    public static void main(String args[]) {
        String nombreArchivoFirmado = directorioBase + "archivo" + 1;
        gen(nombreArchivoFirmado);
        nombreArchivoFirmado = directorioBase + "archivo" + 2;    
        gen(nombreArchivoFirmado);
    }
    
    public static void gen(String nombreArchivoFirmado) {
        try {
            Contexto.inicializar();
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            // create some smime capabilities in case someone wants to respond
            ASN1EncodableVector         signedAttrs = new ASN1EncodableVector();
            SMIMECapabilityVector       caps = new SMIMECapabilityVector();
            caps.addCapability(SMIMECapability.dES_EDE3_CBC);
            caps.addCapability(SMIMECapability.rC2_CBC, 128);
            caps.addCapability(SMIMECapability.dES_CBC);
            signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
            // add an encryption key preference for encrypted responses -
            // normally this would be different from the signing certificate...
            /*IssuerAndSerialNumber   issAndSer = new IssuerAndSerialNumber(
                    new X500Name(signDN), origCert.getSerialNumber());
            signedAttrs.add(new SMIMEEncryptionKeyPreferenceAttribute(issAndSer));*/
            // create the generator for creating an smime/signed message
            SMIMESignedGenerator gen = new SMIMESignedGenerator();
            // add a signer to the generator - this specifies we are using SHA1 and
            // adding the smime attributes above to the signed attributes that
            // will be generated as part of the signature. The encryption algorithm
            // used is taken from the key - in this RSA with PKCS1Padding
            DNIeSessionHelper sessionHelper = new DNIeSessionHelper();
            DNIeContentSigner dnieContentSigner = new DNIeContentSigner("SHA1withRSA");
            iaik.pkcs.pkcs11.Session sesion = DNIeSessionHelper.getSession("*****".toCharArray(), Mechanism.SHA1_RSA_PKCS);
            dnieContentSigner.setPkcs11Session(sesion);

            SimpleSignerInfoGeneratorBuilder dnieSignerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
            dnieSignerInfoGeneratorBuilder = dnieSignerInfoGeneratorBuilder.setProvider("BC");
            dnieSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
            SignerInfoGenerator signerInfoGenerator = dnieSignerInfoGeneratorBuilder.build(
                    "SHA1withRSA", null, sessionHelper.getCertificadoUsuario(), dnieContentSigner);
            
            List certList = new ArrayList();
            certList.add(sessionHelper.getCertificadoCA());
            certList.add(sessionHelper.getCertificadoIntermedio());            
            certList.add(sessionHelper.getCertificadoUsuario());
            gen.addSignerInfoGenerator(signerInfoGenerator);
            
            extraerPEMCertsFromDNIe(sessionHelper);
            // create a CertStore containing the certificates we want carried
            // in the signature
            Store certs = new JcaCertStore(certList);
            // add our pool of certs and cerls (if any) to go with the signature
            gen.addCertificates(certs);
            // create the base for our message
            MimeBodyPart    msg = new MimeBodyPart();
            msg.setText("Hello world!");
            // extract the multipart object from the SMIMESigned object.
            MimeMultipart mm = gen.generate(msg);
            // Get a Session object and create the mail message
            Properties props = System.getProperties();
            Session session = Session.getDefaultInstance(props, null);
            Address fromUser = new InternetAddress("\"Eric H. Echidna\"<eric@bouncycastle.org>");
            Address toUser = new InternetAddress("example@bouncycastle.org");

            MimeMessage body = new MimeMessage(session);
            body.setFrom(fromUser);
            body.setRecipient(Message.RecipientType.TO, toUser);
            body.setSubject("example signed message");
            body.setContent(mm, mm.getContentType());
            body.saveChanges();
            body.writeTo(new FileOutputStream(nombreArchivoFirmado));
            System.out.println("File: " + new File("Hola").getAbsolutePath());            
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    public static void extraerPEMCertsFromDNIe (DNIeSessionHelper sessionHelper) throws IOException, Exception {
            byte [] certCA = CertUtil.fromX509CertToPEM(sessionHelper.getCertificadoCA());
            FileUtils.copyStreamToFile(new ByteArrayInputStream(certCA), new File(directorioBase + "certRaizDNIe.pem"));
            byte [] certIntermedio = CertUtil.fromX509CertToPEM(sessionHelper.getCertificadoIntermedio());
            FileUtils.copyStreamToFile(new ByteArrayInputStream(certIntermedio), new File(directorioBase + "certIntermedioDNIe.pem"));
            byte [] certUsuario = CertUtil.fromX509CertToPEM(sessionHelper.getCertificadoUsuario());
            FileUtils.copyStreamToFile(new ByteArrayInputStream(certUsuario), new File(directorioBase + "usuarioDNIe.pem"));
    }
    
}
