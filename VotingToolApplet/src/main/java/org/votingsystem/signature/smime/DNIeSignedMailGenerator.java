package org.votingsystem.signature.smime;

import static org.votingsystem.model.ContextVS.*;
import javax.mail.Header;

import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.util.Store;
import org.votingsystem.applet.votingtool.VotingToolContext;
import org.votingsystem.model.ContextVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DNIeSignedMailGenerator {

    private static Logger logger = Logger.getLogger(DNIeSignedMailGenerator.class); 

    
     public static SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, 
             String textoAFirmar, char[] password, String asunto, 
             Header header) throws Exception {
        if (asunto == null) asunto = "";
        if (textoAFirmar == null) textoAFirmar = "";
        ASN1EncodableVector         signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector       caps = new SMIMECapabilityVector();
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        DNIeContentSigner dnieContentSigner = new DNIeContentSigner(DNIe_SIGN_MECHANISM);
        try {
            dnieContentSigner.setPkcs11Session(DNIeSessionHelper.getSession(
                    password, VotingToolContext.DNIe_SESSION_MECHANISM));
        } catch(Exception ex) {
            if ("CKR_DEVICE_ERROR".equals(ex.getMessage()) || 
                    "CKR_CRYPTOKI_ALREADY_INITIALIZED".equals(ex.getMessage())) {
                logger.debug("Nuevo intento de obtención de sesión PKCS11 con el DNI");
                Thread.sleep(3000);
                dnieContentSigner.setPkcs11Session(DNIeSessionHelper.getSession(
                        password, VotingToolContext.DNIe_SESSION_MECHANISM));
            } else throw ex;
        }
        SimpleSignerInfoGeneratorBuilder dnieSignerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        dnieSignerInfoGeneratorBuilder = dnieSignerInfoGeneratorBuilder.setProvider(PROVIDER);
        dnieSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = dnieSignerInfoGeneratorBuilder.build(
                DNIe_SIGN_MECHANISM, null, DNIeSessionHelper.getCertificadoUsuario(), dnieContentSigner);
        List certList = new ArrayList();
        certList.add(DNIeSessionHelper.getCertificadoCA());
        certList.add(DNIeSessionHelper.getCertificadoIntermedio());            
        certList.add(DNIeSessionHelper.getCertificadoUsuario());
        gen.addSignerInfoGenerator(signerInfoGenerator);
        // create a CertStore containing the certificates we want carried
        // in the signature
        Store certs = new JcaCertStore(certList);
        // add our pool of certs and cerls (if any) to go with the signature
        gen.addCertificates(certs);
        // create the base for our message
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textoAFirmar);
        // extract the multipart object from the SMIMESigned object.
        MimeMultipart mimeMultipart = gen.generate(msg, "");
        
        // Get a Session object and create the mail message
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        String usuario = null;
        if (ContextVS.INSTANCE.getSessionUser() != null) usuario = 
                ContextVS.INSTANCE.getSessionUser().getNif();
        Address fromUserAddress = new InternetAddress(usuario);
        Address toUserAddress = new InternetAddress(toUser.replace(" ", ""));

        SMIMEMessageWrapper body = new SMIMEMessageWrapper(session);
        if (header != null) body.setHeader(header.getName(), header.getValue());
        body.setHeader("Content-Type", "text/plain; charset=UTF-8");
        body.setFrom(fromUserAddress);
        body.setRecipient(Message.RecipientType.TO, toUserAddress);
        body.setSubject(asunto, "UTF-8");
        body.setContent(mimeMultipart, mimeMultipart.getContentType());
        body.updateChanges();
        //sessionHelper.closeSession();
        return body;
    }
}
