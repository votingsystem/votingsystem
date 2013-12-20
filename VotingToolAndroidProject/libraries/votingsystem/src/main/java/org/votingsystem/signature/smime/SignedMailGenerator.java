package org.votingsystem.signature.smime;

import android.util.Log;
import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle2.asn1.smime.SMIMECapability;
import org.bouncycastle2.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.operator.OperatorCreationException;
import org.bouncycastle2.util.Store;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignedMailGenerator {
	
	public static final String TAG = "SignedMailGenerator";
    
    private SMIMESignedGenerator smimeSignedGenerator = null;
    // Get a Session object and create the mail message
    private static Properties props = System.getProperties();
    private static Session session = Session.getDefaultInstance(props, null);

    public SignedMailGenerator(byte[] keyStoreBytes, String keyAlias, 
    		char[] password, String signMechanism) throws VotingSystemKeyStoreException {
        try {
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey key = (PrivateKey)keyStore.getKey(keyAlias, password);
            Certificate[] chain = keyStore.getCertificateChain(keyAlias);
            init(key, chain, signMechanism);
        }catch(Exception ex) {
            throw new VotingSystemKeyStoreException(ex);
        }
    }
    
    public SignedMailGenerator(PrivateKey key, Certificate[] chain, 
    		String signMechanism) throws Exception {
        init(key, chain, signMechanism);
    }
    
    public SignedMailGenerator(PrivateKey privateKey,
			X509Certificate[] arrayCerts, String signMechanism) 
					throws CertificateEncodingException, OperatorCreationException {
    	init(privateKey, arrayCerts, signMechanism);
	}

	private void init (PrivateKey key, Certificate[] chain, String signatureMechanism) 
            throws CertificateEncodingException, OperatorCreationException {
    	Log.d(TAG + ".init(...)", " - signatureMechanism: " + signatureMechanism);
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        //create some smime capabilities in case someone wants to respond        
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        List certList = Arrays.asList(chain);
        Store certs = new JcaCertStore(certList);
        smimeSignedGenerator = new SMIMESignedGenerator();
        SimpleSignerInfoGeneratorBuilder signerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        signerInfoGeneratorBuilder.setProvider(ContextVS.PROVIDER);
        signerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                signatureMechanism, key, (X509Certificate)chain[0]);
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(certs);
    }
    
    public SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, String textToSign,
            String subject, Header... headers) throws Exception {
        if (subject == null) subject = "";
        if (textToSign == null) textToSign = "";
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textToSign);
        MimeMultipart mimeMultipart = smimeSignedGenerator.generate(msg,
                ContextVS.DEFAULT_SIGNED_FILE_NAME);
        SMIMEMessageWrapper body = new SMIMEMessageWrapper(session);
        if (headers != null) {
            for(Header header : headers) {
                if (header != null) body.setHeader(header.getName(), header.getValue());
            }
        }
        if (fromUser != null && !"".equals(fromUser)) {
        	Address fromUserAddress = new InternetAddress(fromUser);
        	body.setFrom(fromUserAddress);
        }
        if (toUser != null && !"".equals(toUser)) {
        	Address toUserAddress = new InternetAddress(toUser.replace(" ", ""));
        	body.setRecipient(Message.RecipientType.TO, toUserAddress);
        }
        body.setSubject(subject);
        body.setContent(mimeMultipart, mimeMultipart.getContentType());
        body.save();
        return body;
    }
   
     public MimeMultipart genMimeMultipart(MimeBodyPart body, 
             SMIMEMessageWrapper dnieMimeMessage) throws Exception {
         smimeSignedGenerator.addSigners(dnieMimeMessage.getSmimeSigned().getSignerInfos());
         smimeSignedGenerator.addAttributeCertificates(dnieMimeMessage.getSmimeSigned().getAttributeCertificates());
         smimeSignedGenerator.addCertificates(dnieMimeMessage.getSmimeSigned().getCertificates());
         smimeSignedGenerator.addCRLs(dnieMimeMessage.getSmimeSigned().getCRLs());
         smimeSignedGenerator.getGeneratedDigests();
         MimeMultipart mimeMultipart = smimeSignedGenerator.generate(
        		 body, ContextVS.PROVIDER, ContextVS.SIGNATURE_ALGORITHM);
         return mimeMultipart;
     }
    
}