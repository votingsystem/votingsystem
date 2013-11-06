package org.sistemavotacion.smime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSVerifierCertificateNotValidException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.controlacceso.modelo.InformacionVoto;
import org.sistemavotacion.controlacceso.modelo.Usuario;
import org.sistemavotacion.seguridad.PKIXCertPathReviewer;
import org.sistemavotacion.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMEMessageWrapper extends MimeMessage {
    
    private static Logger log = LoggerFactory.getLogger(SMIMEMessageWrapper.class);
    
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    private String messageId;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;

	private Set<Usuario> firmantes = null;
	private Usuario firmante;
	private InformacionVoto informacionVoto;
	private boolean isValidSignature = false;
	
	private static Properties props = System.getProperties();
	// Get a Session object with the default properties.
	private static Session defaultSession = Session.getDefaultInstance(props, null);
	private byte[] messageBytes = null;
	
    public SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        log.debug("SMIMEMessageWrapper(Session session)");
        String fileName =  StringUtils.randomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + ".p7m");
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + ".p7m";
    }
    
    public SMIMEMessageWrapper (SMIMESigned simeSigned) throws Exception {
    	super(defaultSession);
    	this.smimeSigned = simeSigned;
        smimeSigned = new SMIMESigned(this); 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
        signedContent = baos.toString(); 
        checkSignature();	
    }
 
    public SMIMEMessageWrapper (InputStream inputStream) throws Exception {
        super(defaultSession, inputStream);
        init();
    }

    public void init() throws Exception {
        if(getContent() instanceof MimeMultipart){
            log.debug("content instanceof MimeMultipart");
            smimeSigned = new SMIMESigned((MimeMultipart)getContent());
            MimeBodyPart content = smimeSigned.getContent();
            Object  cont = content.getContent();
            if (cont instanceof String) {
                signedContent = (String)cont;
            } else if (cont instanceof Multipart){
                Multipart multipart = (Multipart)cont;
                int count = multipart.getCount();
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    Object part = bodyPart.getContent();
                    stringBuilder.append("Part " + i).append("---------------------------");
                    if (part instanceof String) {
                        stringBuilder.append((String)part);
                    } else if (part instanceof ByteArrayInputStream) {
                        ByteArrayInputStream contentStream = (ByteArrayInputStream)cont;
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buf =new byte[2048];
                        int len;
                        while((len = contentStream.read(buf)) > 0){
                            output.write(buf,0,len);
                        }
                        output.close();
                        contentStream.close();
                        signedContent = new String(output.toByteArray());
                    }  else  {
                        log.debug(" TODO - get content from part instanceof -> " + part.getClass());
                    }
                }
                signedContent = stringBuilder.toString();
            } else if (cont instanceof ByteArrayInputStream) {
                ByteArrayInputStream contentStream = (ByteArrayInputStream)cont;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buf =new byte[2048];
                int len;
                while((len = contentStream.read(buf)) > 0){
                    output.write(buf,0,len);
                }
                output.close();
                contentStream.close();
                signedContent = new String(output.toByteArray());
            }
        } else if(getContent() instanceof String){ 
            log.error("TODO - content instanceof String -> " + getContent()); 
        }
        checkSignature(); 
    }
    
    public void updateChanges() throws Exception {
    	super.saveChanges();
    	init();
    }
    
    public void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
    }

    public void setMessageID(String messageId) throws MessagingException {
    	this.messageId = messageId;
    	updateMessageID();
    }

    /**
     * @return the signedContent
     */
    public String getSignedContent() {
        return signedContent;
    }

    /**
     * Digest for storing unique MensajeSMIME in database 
     * @return the contentDigestStr
     * @throws NoSuchAlgorithmException 
     */
    public String getContentDigestStr() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] resultDigest =  messageDigest.digest(signedContent.getBytes());
        return new String(Base64.encode(resultDigest));
    }
    
    /**
     * @return the smimeSigned
     */
    public SMIMESigned getSmimeSigned() {
        return smimeSigned;
    }
    
	private boolean verifySignerCert(SignerInformation signer, X509Certificate cert) {
    	boolean result = false;
        try {
        	if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))){
                log.debug("signature verified");
                result = true;
            } else {log.debug("signature failed!");}
        } catch(CMSVerifierCertificateNotValidException ex) {
        	log.debug("-----> cert.getNotBefore(): " + cert.getNotBefore());
        	log.debug("-----> cert.getNotAfter(): " + cert.getNotAfter());
    		log.error(ex.getMessage(), ex);
        } finally {
        	return result;
        }
    }
    
    public boolean isValidSignature()  {
    	return isValidSignature;
    }
    
    /**
     * verify that the sig is correct and that it was generated when the 
     * certificate was current(assuming the cert is contained in the message).
     */
    private void checkSignature() throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        informacionVoto = new InformacionVoto();
        log.debug("checkSignature - document with '" + signers.size() + "' signers");
        Collection c = signers.getSigners();
        Iterator it = c.iterator();
        Date firstSignature = null;
        isValidSignature = false;
        firmantes = new HashSet<Usuario>();
        while (it.hasNext()) {// check each signer
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC)
                    .getCertificate((X509CertificateHolder)certIt.next());
            log.debug("checkSignature - cert: " + cert.getSubjectDN() + " --- " + 
                    certCollection.size() + " match");
            isValidSignature = verifySignerCert(signer, cert);
            if(!isValidSignature) return;
            Usuario usuario = Usuario.getUsuario(cert);
            TimeStampToken timeStampToken = checkTimeStampToken(signer);//method can only be called after verify.
            if(timeStampToken != null) {
            	Date timeStampDate = timeStampToken.getTimeStampInfo().getGenTime();
            	if(firstSignature == null || firstSignature.after(timeStampDate)) {
            		firstSignature = timeStampDate;
            		this.firmante = usuario;
            	}
            }
            usuario.setTimeStampToken(timeStampToken);
            firmantes.add(usuario);
            if (cert.getSubjectDN().toString().contains("OU=hashCertificadoVotoHEX:")) {
            	informacionVoto.setCertificadoVoto(cert);
            	informacionVoto.setVoteTimeStampToken(timeStampToken);
            } else {informacionVoto.addServerCert(cert);} 
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        messageBytes = baos.toByteArray();
        baos.close();
    }
    
    public SignedMailValidator.ValidationResult verify(
            PKIXParameters params) throws Exception {
        SignedMailValidator validator = new SignedMailValidator(this, params);
        // iterate over all signatures and print results
        Iterator it = validator.getSignerInformationStore().getSigners().iterator();
        Locale loc = Locale.ENGLISH;
        //only one signer supposed!!!
        SignedMailValidator.ValidationResult result = null;
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            result = validator.getValidationResult(signer);
            if (result.isValidSignature()){
                log.debug("isValidSignature");
            }
            else {
                log.debug("sigInvalid");
                log.debug("Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    log.debug("ERROR - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                log.debug("Notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    log.debug("NOTIFICACION - " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) {
                    log.debug("Certificate path valid");
                }
                else {
                    log.debug("Certificate path invalid");
                }
                log.debug("Certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    log.debug("ERROR - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1)
                        .iterator();
                while (notificationsIt.hasNext()) {
                    log.debug("NOTIFICACION - " + notificationsIt.next().toString());
                }
                // per certificate errors and notifications
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    log.debug("Certificate " + i + " ----------- ");
                    log.debug("Issuer: " + cert.getIssuerDN().getName());
                    log.debug("Subject: " + cert.getSubjectDN().getName());
                    log.debug("Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        log.debug( errorsIt.next().toString());
                    }
                    // notifications
                    log.debug("Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        log.debug(notificationsIt.next().toString());
                    }
                    i++;
                }
            }
        }
        return result;
    }

    public Set<Usuario> getFirmantes() {
    	return firmantes;
    }
  
    public byte[] getBytes () {
    	return messageBytes;
    }

	public InformacionVoto getInformacionVoto() {
		return informacionVoto;
	}

    public void setFirmantes(Set<Usuario> firmantes) {
    	this.firmantes = firmantes;
    }

	public Usuario getFirmante() {
		if(firmante == null && !firmantes.isEmpty()) {
			firmante = firmantes.iterator().next();
		}
		return firmante;
	}
	
    //Call this method after isValidSignature()!!!
    private TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        TimeStampToken timeStampToken = null;
        byte[] digestBytes = signer.getContentDigest();//method can only be called after verify.
        String digestStr = new String(Base64.encode(digestBytes));
        AttributeTable  unsignedAttributes = signer.getUnsignedAttributes();
        if(unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(
                    PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if(timeStampAttribute != null) {
                DEREncodable dob = timeStampAttribute.getAttrValues().getObjectAt(0);
                org.bouncycastle.cms.CMSSignedData signedData = 
                        new org.bouncycastle.cms.CMSSignedData(dob.getDERObject().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                byte[] hashToken = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
                String digestTokenStr = new String(Base64.encode(hashToken));
                Calendar cal = new GregorianCalendar();
                cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                log.debug("checkTimeStampToken - timeStampToken - fecha: " +  cal.getTime());
                //log.debug("checkTimeStampToken - digestStr: " + digestStr 
                //		+ " - digestTokenStr " + digestTokenStr);
                return timeStampToken;
            }
        } else log.debug("checkTimeStampToken - without unsignedAttributes"); 
        return timeStampToken;
    }
    
}
