package org.sistemavotacion.smime;

import static org.sistemavotacion.Contexto.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.util.Collection;
import java.util.Iterator;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.sistemavotacion.seguridad.PKIXCertPathReviewer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSVerifierCertificateNotValidException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.InformacionVoto;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.util.VotingSystemKeyGenerator;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMEMessageWrapper extends MimeMessage {
    
    private static Logger logger = LoggerFactory.getLogger(SMIMEMessageWrapper.class);

    static {
        MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();

        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        
        mc.addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature");
        mc.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime");
        mc.addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature");
        mc.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime");
        mc.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed");

        CommandMap.setDefaultCommandMap(mc);
    }

    private String messageId;
    private String fileName;
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
    
    public Set<Usuario> getFirmantes() {
        return firmantes;
    }
    
    public Usuario getFirmante() {
        return firmante;
    }
    
    public SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        fileName =  StringUtils.RandomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + SIGNED_PART_EXTENSION);
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" 
                + fileName + SIGNED_PART_EXTENSION;
    }

    public SMIMEMessageWrapper (Session session, InputStream inputStream, String fileName) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        super(session, inputStream);
        if (fileName == null) this.fileName = DEFAULT_SIGNED_FILE_NAME; 
        else this.fileName = fileName;
        initSMIMEMessage();
    }

    private void initSMIMEMessage() throws IOException, MessagingException, 
            CMSException, SMIMEException, Exception{
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
                    } else  {
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
    
    //@Override
    public void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
    }
 
    public void updateMessageID(String nifUsuario) throws MessagingException {
        messageId = getFileName() + "@" + nifUsuario;
        Address[] addresses = {new InternetAddress(nifUsuario)};
        addFrom(addresses);
        updateMessageID(); 
    }

    /**
     * @return the signedContent
     */
    public String getSignedContent() {
        return signedContent;
    }
    
    /**
     * @param signedContent the signedContent to set
     */
    public void setSignedContent(String signedContent) {
        this.signedContent = signedContent;
    }

    /**
     * @return the smimeSigned
     */
    public SMIMESigned getSmimeSigned() {
        return smimeSigned;
    }
    
    public boolean isValidSignature() {
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
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(Contexto.PROVIDER)
                    .getCertificate((X509CertificateHolder)certIt.next());
            logger.debug("checkSignature - cert: " + cert.getSubjectDN() + " --- " + 
                    certCollection.size() + " match");
            isValidSignature = verifySignerCert(signer, cert);
            if(!isValidSignature) return;
            Usuario usuario = Usuario.getUsuario(cert);
            usuario.setSigner(signer);
            TimeStampToken timeStampToken = checkTimeStampToken(signer);//method can only be called after verify.
            usuario.setTimeStampToken(timeStampToken);
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
    }
    
    public InformacionVoto getInformacionVoto() {
        return informacionVoto;
    }
    
    private boolean verifySignerCert(SignerInformation signer, X509Certificate cert) {
    	boolean result = false;
        try {
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    Contexto.PROVIDER).build(cert))){
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
    
    private TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        //Call this method after isValidSignature()
        TimeStampToken timeStampToken = null;
        byte[] digestBytes = signer.getContentDigest();//method can only be called after verify.
        //String digestStr = new String(Base64.encode(digestBytes));
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
                //String hashTokenStr = new String(Base64.encode(hashToken));
                Calendar cal = new GregorianCalendar();
                cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                logger.debug("checkTimeStampToken - timeStampToken date: " 
                        +  DateUtils.getStringFromDate(cal.getTime()));
                //logger.debug("checkTimeStampToken - digestStr: " + digestStr);
                //logger.debug("checkTimeStampToken - timeStampToken - hashTokenStr: " +  hashTokenStr);
                return timeStampToken;
            }
        } else logger.debug(" --- without unsignedAttributes"); 
        return timeStampToken;
    }
    
    public void setTimeStampToken(TimeStampToken timeStampToken) throws Exception {
        if(timeStampToken == null ) throw new Exception("NULL_TIME_STAMP_TOKEN");
        DERObject derObject = new ASN1InputStream(timeStampToken.
                getEncoded()).readObject();
        DERSet derset = new DERSet(derObject);
        Attribute timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, derset);
        
        Hashtable hashTable = new Hashtable();
        hashTable.put(PKCSObjectIdentifiers.
                        id_aa_signatureTimeStampToken, timeStampAsAttribute);
        AttributeTable timeStampAsAttributeTable = new AttributeTable(hashTable);
        
        byte[] hashTokenBytes = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
        //String hashTokenStr = new String(Base64.encode(hashTokenBytes));
        //logger.debug("setTimeStampToken - timeStampToken - hashTokenStr: " +  hashTokenStr);
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        List<SignerInformation> newSigners = new ArrayList<SignerInformation>();
        while (it.hasNext()) {
            SignerInformation signer = it.next();
            byte[] digestBytes = signer.getContentDigest();//method can only be called after verify.
            //String digestStr = new String(Base64.encode(digestBytes));
            //logger.debug("setTimeStampToken - hash firmante: " +  digestStr + 
            //        " - hash token: " + hashTokenStr);
            if(Arrays.equals(hashTokenBytes, digestBytes)) {
                logger.debug("setTimeStampToken - firmante");
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable != null) {
                    logger.debug("setTimeStampToken - signer with UnsignedAttributes");
                    hashTable = attributeTable.toHashtable();
                    hashTable.put(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, timeStampAsAttribute);
                    attributeTable = new AttributeTable(hashTable);
                }
                updatedSigner = signer.replaceUnsignedAttributes(
                            signer, timeStampAsAttributeTable);
                newSigners.add(updatedSigner);
            } else newSigners.add(signer);
        }
        //logger.debug("setTimeStampToken - num. firmantes: " + newSigners.size());
        SignerInformationStore newSignersStore = new SignerInformationStore(newSigners);
        CMSSignedData cmsdata = smimeSigned.replaceSigners(smimeSigned, newSignersStore);
        replaceSigners(cmsdata);
    }
    
    public TimeStampRequest getTimeStampRequest() {
        if(smimeSigned == null) return null;
        SignerInformation signerInformation = ((SignerInformation)
                        smimeSigned.getSignerInfos().getSigners().iterator().next());
        if(signerInformation == null) {
            logger.debug("signerInformation null");
            return null;
        }
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
        //byte[] digest = Base64.encode(as.getOctets());
        //logger.debug(" - digest: " + new String(digest));
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(signerInformation.getDigestAlgOID(), as.getOctets(),
                BigInteger.valueOf(VotingSystemKeyGenerator.INSTANCE.getNextRandomInt()));
    }
    

    private void replaceSigners(CMSSignedData cmsdata) throws Exception {
        logger.debug("replaceSigners");
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        gen.addAttributeCertificates(cmsdata.getAttributeCertificates());
        gen.addCertificates(cmsdata.getCertificates());
        gen.addSigners(cmsdata.getSignerInfos());
        MimeMultipart mimeMultipart = gen.generate(smimeSigned.getContent(), 
                smimeSigned.getContent().getFileName());
        setContent(mimeMultipart, mimeMultipart.getContentType());
        saveChanges();
    }
    
    public void save() throws Exception {
        logger.debug("save");
        super.saveChanges();
        initSMIMEMessage();
    }
    
    public File copyContentToFile (File destFile) throws IOException, MessagingException {
        FileOutputStream fos = new FileOutputStream(destFile);
        writeTo(fos);
        fos.close();
        return destFile;
    }
    
    public byte[] getBytes () throws IOException, MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        baos.close();
        byte[] resultado = baos.toByteArray();
        return resultado;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    
    public SignedMailValidator.ValidationResult verify(
            PKIXParameters params) throws Exception {
        logger.debug("verify");
        SVCertExtensionChecker checker = new SVCertExtensionChecker();
        params.addCertPathChecker(checker);   
        
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
            } else {
                logger.debug(" --- sigInvalid --- ");
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
                    logger.debug("Certificate " + i + " -------------------- ");
                    logger.debug("Issuer: " + cert.getIssuerDN().getName());
                    logger.debug("Subject: " + cert.getSubjectDN().getName());
                    errorsIt = review.getErrors(i).iterator();
                    if(errorsIt.hasNext()) {
                        logger.debug("Errors:");
                        while (errorsIt.hasNext())  {
                            logger.debug( errorsIt.next().toString());
                        }
                    }
                    notificationsIt = review.getNotifications(i).iterator();
                    if(notificationsIt.hasNext()) {
                        logger.debug("Notifications:");
                        while (notificationsIt.hasNext()) {
                            logger.debug(notificationsIt.next().toString());
                        }
                    }
                    i++;
                }
            }
        }
        return result;
    }
    
    	//To bypass id_kp_timeStamping ExtendedKeyUsage exception
	private static class SVCertExtensionChecker extends PKIXCertPathChecker {
		
		Set supportedExtensions;
		
		SVCertExtensionChecker() {
			supportedExtensions = new HashSet();
			supportedExtensions.add(X509Extensions.ExtendedKeyUsage);
		}
		
		public void init(boolean forward) throws CertPathValidatorException {
		 //To change body of implemented methods use File | Settings | File Templates.
	    }

		public boolean isForwardCheckingSupported(){
			return true;
		}

		public Set getSupportedExtensions()	{
			return null;
		}

		public void check(Certificate cert, Collection<String> unresolvedCritExts)
				throws CertPathValidatorException {
			for(String ext : unresolvedCritExts) {
				if(X509Extensions.ExtendedKeyUsage.toString().equals(ext)) {
					logger.debug("------------- ExtendedKeyUsage removed from validation");
					unresolvedCritExts.remove(ext);
				}
			}
		}

	}
    
    
}