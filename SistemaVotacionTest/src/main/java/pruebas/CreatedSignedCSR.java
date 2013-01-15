package pruebas;

import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;

import java.io.File;
import javax.mail.Header;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatedSignedCSR {
    
    private static Logger logger = (Logger) LoggerFactory.getLogger(CsrTest.class);
    
    public static void main (String[] args) {
        genSignedMail();
    }

    
    public static void genSignedMail () {
        try {
            Contexto.inicializar();            
            ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
            SMIMECapabilityVector       caps = new SMIMECapabilityVector();
            caps.addCapability(SMIMECapability.dES_EDE3_CBC);
            caps.addCapability(SMIMECapability.rC2_CBC, 128);
            caps.addCapability(SMIMECapability.dES_CBC);
            signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
            File keyStoreFile = new File(CsrTest.keyStoreFirmaVotosPath);
            SignedMailGenerator dnies = new SignedMailGenerator(
                    FileUtils.getBytesFromFile(keyStoreFile), CsrTest.keyStoreFirmaVotosAlias, 
                    CsrTest.keyStoreFirmaVotosPass.toCharArray(), 
                    ContextoPruebas.VOTE_SIGN_MECHANISM);
            Header header  = new Header("hasnCert", "34f3g25");
            dnies.genFile("from@m.com", "toUser@m.com", 
            		"blim blim", "asunto", header, 
                    SignedMailGenerator.Type.USER, new File(CsrTest.keyStoreFirmaVotosSignedFilePath));
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } 
    }
}
