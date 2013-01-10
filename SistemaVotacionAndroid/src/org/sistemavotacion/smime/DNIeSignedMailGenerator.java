package org.sistemavotacion.smime;

import java.io.ByteArrayOutputStream;
import javax.mail.Header;
import java.io.File;
import java.io.FileOutputStream;
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
import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle2.asn1.smime.SMIMECapability;
import org.bouncycastle2.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.mail.smime.SMIMESignedGenerator;
import org.bouncycastle2.util.Store;
import org.sistemavotacion.util.FileUtils;

import android.util.Log;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class DNIeSignedMailGenerator {

    
    public static final String NOMBRE_ARCHIVO_FIRMADO = "EventoEnviado";
    public static String CERT_STORE_TYPE = "Collection";
    public static String PROVIDER = "BC";    
          
    public static File genFile(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto) throws Exception {
    	return genFile(fromUser, toUser, textoAFirmar, password, asunto, null);
    }
    
    public static File genFile(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto, Header header) throws Exception {
        File resultado = new File(FileUtils.APPTEMPDIR + NOMBRE_ARCHIVO_FIRMADO);
        MimeMessage body = gen(fromUser, toUser, textoAFirmar,  password, asunto, header);
        body.writeTo(new FileOutputStream(resultado));
        return resultado;
    }
          
    public static String genString(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto, Header header) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MimeMessage body = gen(fromUser, toUser, textoAFirmar,  password, asunto, header);
        body.writeTo(baos);
        return new String(baos.toByteArray());
    }
    
    
    private static MimeMessage gen(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto, Header header) throws Exception {
        Log.i("DNIeSignedMailGenerator", "dummy genSMIMEdummy genSMIMEdummy genSMIME");
        /*File keyStoreFile = new File("/home/jgzornoza/git/recursos/dni_keystore/mockDnie.jks");
        SignedMailGenerator dnies = new SignedMailGenerator(
                FileUtils.getBytesFromFile(keyStoreFile), "endEntityAlias", "dnie".toCharArray());
        return dnies.genMimeMessage(fromUser, toUser, 
                textoAFirmar, asunto, header, SignedMailGenerator.Type.USER);*/
        return null;
    }
    
    
}
