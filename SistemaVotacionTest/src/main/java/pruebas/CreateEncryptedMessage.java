package pruebas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.Contexto;
import static org.sistemavotacion.Contexto.CERT_RAIZ_PATH;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class CreateEncryptedMessage {
    
    private static Logger logger = LoggerFactory.getLogger(CreateEncryptedMessage.class);  
    
    //public static String encryptedMessagePath = "./encripteMessage";
    public static String encryptedMessagePath = "/home/jgzornoza/emailPDF";
    public static String keyStorePassword = "PemPass";
    public static String keyStoreFileName = "ControlAcceso.jks";
    public static String receiverCertAlias = "ClavesControlAcceso";
    
    public static void main(String args[]) throws Exception {
        Contexto.inicializar();
        
        byte[] receiverKeyStoreBytes = FileUtils.getBytesFromInputStream(Thread.currentThread().getContextClassLoader()
               .getResourceAsStream(keyStoreFileName));
        KeyStore receiverKeyStore = KeyStoreUtil.getKeyStoreFromBytes(
               receiverKeyStoreBytes, keyStorePassword.toCharArray());
        java.security.cert.Certificate[] chain = 
                receiverKeyStore.getCertificateChain(receiverCertAlias);
        X509Certificate cert = (X509Certificate) chain[0];
        logger.debug(" ------ receiver cert:" + cert.getSubjectDN().toString());
        logger.debug(" ------ receiver cert serial number:" + cert.getSerialNumber());
        
        File signedFile = File.createTempFile("testSignedMessage", ".p7s");
        FileUtils.copyStreamToFile(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("testSignedMessage.p7s"), signedFile);
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        SMIMEMessageWrapper smimeMessageReq = 
                new SMIMEMessageWrapper(session, signedFile);
        
        SignerInformationStore  signers = 
                smimeMessageReq.getSmimeSigned().getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        byte[] digestBytes = it.next().getContentDigest();//method can only be called after verify.
        String digestStr = new String(Base64.encode(digestBytes));
        logger.debug(" ------ Encriptando mensaje con digestStr: " + digestStr);

        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        JceKeyTransRecipientInfoGenerator recipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(cert);
        encrypter.addRecipientInfoGenerator(recipientInfoGenerator.setProvider(BouncyCastleProvider.PROVIDER_NAME));

        
        //http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
        //String algorithmOid = SMIMEEnvelopedGenerator.AES128_CBC;
        //What is triple-DES -> http://www.rsa.com/rsalabs/node.asp?id=2231
        String algorithmOid = SMIMEEnvelopedGenerator.DES_EDE3_CBC;
        
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(smimeMessageReq,
            new JceCMSContentEncryptorBuilder(
                new ASN1ObjectIdentifier(algorithmOid)).setProvider("BC").build());

        /*
         * Create a new MimeMessage that contains the encrypted and signed
         * content
         */
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encryptedPart.writeTo(out);

        MimeMessage encryptedMessage = new MimeMessage(null,
                new ByteArrayInputStream(out.toByteArray()));

        /* Set all original MIME headers in the encrypted message */
        Enumeration headers = smimeMessageReq.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            logger.debug(" ------ adding headerLine:" + headerLine);
            /*
             * Make sure not to override any content-* headers from the
             * original message
             */
            if (!Strings.toLowerCase(headerLine).startsWith("content-"))  {
                encryptedMessage.addHeaderLine(headerLine);
            }
        }
        encryptedMessage.addHeaderLine("SignedMessageDigest: " + digestStr);
        File encryptedMessageFile = new File(encryptedMessagePath);
        logger.debug(" ------ encryptedMessageFile path:" + encryptedMessageFile.getAbsolutePath());
        encryptedMessage.writeTo(new FileOutputStream(encryptedMessageFile));
        //encryptedMessage.writeTo(System.out);


    }

}
