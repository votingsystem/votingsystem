package org.sistemavotacion.smime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
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
import org.bouncycastle.cms.CMSVerifierCertificateNotValidException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.centrocontrol.modelo.InformacionVoto;
import org.sistemavotacion.centrocontrol.modelo.Usuario;
import org.sistemavotacion.seguridad.PKIXCertPathReviewer;
import org.sistemavotacion.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMEMessageWrapper extends MimeMessage {
    
    private static Logger logger = LoggerFactory.getLogger(SMIMEMessageWrapper.class);
    
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    private String messageId;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;

	private Set<Usuario> firmantes = null;
	private Usuario firmante;
	private InformacionVoto informacionVoto;
	private boolean isValidSignature = false;
	private byte[] messageBytes = null;
	
	private static Properties props = System.getProperties();
	// Get a Session object with the default properties.
	private static Session defaultSession = Session.getDefaultInstance(props, null);
    
    public SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        String fileName =  StringUtils.randomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + ".p7m");
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + ".p7m";
    }
    
    public SMIMEMessageWrapper (InputStream inputStream) throws Exception {
        super(defaultSession, inputStream);
        init();
    }

    public void init() throws Exception {
        if(getContent() instanceof MimeMultipart){
            logger.debug("content instanceof MimeMultipart");
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
                        logger.debug(" TODO - get content from part instanceof -> " + part.getClass());
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
            logger.error("TODO - content instanceof String -> " + getContent()); 
        }
        checkSignature(); 
    }

    public void updateChanges() throws Exception {
    	super.saveChanges();
    	init();
    }
    
    @Override
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

	private boolean verifySignerCert(SignerInformation signer, X509Certificate cert) {
    	boolean result = false;
        try {
        	if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))){
                logger.debug("signature verified");
                result = true;
            } else {logger.debug("signature failed!");}
        } catch(CMSVerifierCertificateNotValidException ex) {
        	logger.debug("-----> cert.getNotBefore(): " + cert.getNotBefore());
        	logger.debug("-----> cert.getNotAfter(): " + cert.getNotAfter());
    		logger.error(ex.getMessage(), ex);
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
        logger.debug("checkSignature - document with '" + signers.size() + "' signers");
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
            logger.debug("checkSignature - cert: " + cert.getSubjectDN() + " --- " + 
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
    
    public static PKIXParameters getPKIXParameters (X509Certificate... certs) 
            throws InvalidAlgorithmParameterException{
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for(X509Certificate cert:certs) {
            TrustAnchor anchor = new TrustAnchor(cert, null);
            anchors.add(anchor);
        }
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false); // tell system do not chec CRL's
        return params;
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
                logger.debug("isValidSignature");
            }
            else {
                logger.debug("sigInvalid");
                logger.debug("Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    logger.debug("ERROR - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                logger.debug("Notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    logger.debug("NOTIFICACION - " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) {
                    logger.debug("Certificate path valid");
                }
                else {
                    logger.debug("Certificate path invalid");
                }
                logger.debug("Certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    logger.debug("ERROR - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1)
                        .iterator();
                while (notificationsIt.hasNext()) {
                    logger.debug("NOTIFICACION - " + notificationsIt.next().toString());
                }
                // per certificate errors and notifications
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    logger.debug("Certificate " + i + " ----------- ");
                    logger.debug("Issuer: " + cert.getIssuerDN().getName());
                    logger.debug("Subject: " + cert.getSubjectDN().getName());
                    logger.debug("Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        logger.debug( errorsIt.next().toString());
                    }
                    // notifications
                    logger.debug("Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        logger.debug(notificationsIt.next().toString());
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


	public Usuario getFirmante() {
		if(firmante == null && !firmantes.isEmpty()) {
			firmante = firmantes.iterator().next();
		}
		return firmante;
	}
	
    /**
     * @return the smimeSigned
     */
    public SMIMESigned getSmimeSigned() {
        return smimeSigned;
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
                logger.debug("checkTimeStampToken - timeStampToken - fecha: " +  cal.getTime());
                //logger.debug("checkTimeStampToken - digestStr: " + digestStr 
                //		+ " - digestTokenStr " + digestTokenStr);
                return timeStampToken;
            }
        } else logger.debug("checkTimeStampToken - without unsignedAttributes"); 
        return timeStampToken;
    }
    
}
