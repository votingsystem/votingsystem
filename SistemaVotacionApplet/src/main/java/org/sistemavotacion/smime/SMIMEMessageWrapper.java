package org.sistemavotacion.smime;

import static org.sistemavotacion.Contexto.*;

import com.sun.mail.util.BASE64DecoderStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.util.SharedByteArrayInputStream;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.sistemavotacion.modelo.Firmante;
import org.sistemavotacion.seguridad.PKIXCertPathReviewer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.StringUtils;

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

        CommandMap.setDefaultCommandMap(mc);
    }

    private String messageId;
    private String fileName;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;
    private boolean isValidSignature = false;
    
    private Set<Firmante> firmantes;
    private static Properties props = System.getProperties();
    // Get a Session object with the default properties.
    private static Session defaultSession = Session.getDefaultInstance(props, null);
    
    public Set<Firmante> getFirmantes() {
        return firmantes;
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
        if (getContent() instanceof BASE64DecoderStream) {
            logger.debug("content instanceof BASE64DecoderStream");
            smimeSigned = new SMIMESigned(this); 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
            signedContent = baos.toString(); 
        } else if(getContent() instanceof SharedByteArrayInputStream){ 
            logger.debug("content instanceof SharedByteArrayInputStream");
            smimeSigned = new SMIMESigned(this); 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
            signedContent = baos.toString();
        } else {
            smimeSigned = new SMIMESigned((MimeMultipart)getContent());
            logger.debug("content instanceof MimeMultipart");
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
                    } else if (part instanceof BASE64DecoderStream) {
                        InputStreamReader isr = new InputStreamReader((BASE64DecoderStream)part);
                        Writer writer = new StringWriter();
                        char[] buffer = new char[1024];
                        try {
                            Reader reader = new BufferedReader(isr);
                            int n;
                            while ((n = reader.read(buffer)) != -1) {
                                writer.write(buffer, 0, n);
                            }
                        } finally {
                            isr.close();
                        }
                        signedContent = writer.toString();
                    } else  {
                        logger.debug("IMPOSIBLE EXTRAER CONTENIDO DE LA SECCION " + i);
                    }
                }
                signedContent = stringBuilder.toString();
            }
        }
        checkSignature(); 
    }
    
    @Override
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
        logger.debug("signers.size(): " + signers.size());
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        isValidSignature = false;
        // check each signer
        firmantes = new HashSet<Firmante>();
        while (it.hasNext()) {
            logger.debug("-------------- signer -------------------");
            SignerInformation   signer = it.next();
            AttributeTable  attributes = signer.getSignedAttributes();
            DERUTCTime time = null;
            Firmante firmante = new Firmante();
            firmante.setSigner(signer);
            firmante.setContenidoFirmado(getSignedContent());
            /*byte[] hash = null;
            String hashStr = null;
            if (attributes != null) {
                Attribute signingTimeAttribute = attributes.get(CMSAttributes.signingTime);
                time = (DERUTCTime) signingTimeAttribute.getAttrValues().getObjectAt(0);
                firmante.setFechaFirma(time.getDate());
                Attribute messageDigestAttribute = attributes.get( CMSAttributes.messageDigest );
                hash = ((ASN1OctetString)messageDigestAttribute.getAttrValues().getObjectAt(0)).getOctets();
                hashStr = new String(Base64.encode(hash));
            }*/   
            Collection certCollection = certs.getMatches(signer.getSID());
            logger.debug("Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(PROVIDER).getCertificate(
                    (X509CertificateHolder)certIt.next());
            SignerInformationVerifier siv = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(PROVIDER).build(cert);
            isValidSignature  = signer.verify(siv);
            if(!isValidSignature) {
                logger.debug("signature verified");
                return;
            } 
            firmante.setUsuario(Usuario.getUsuario(cert));
            firmante.setCert(cert);
            firmantes.add(firmante);
            firmante.setTimeStampToken(checkTimeStampToken(signer));//method can only be called after verify.
            logger.debug("cert.getSubjectDN(): " + cert.getSubjectDN());

            //byte[] digestParams = signer.getDigestAlgParams();
            //String digestParamsStr = new String(Base64.encode(digestParams));
            //logger.debug(" -- digestParamsStr: " + digestParamsStr);
            //byte[] digest, AlgorithmIdentifier encryptionAlgorithm, AlgorithmIdentifier  digestAlgorithm, PublicKey key, byte[] signature, 
            //String sigProviderSignerInformation signer, X509Certificate cert,  String provider
            
            //boolean cmsVerifyDigest = CMSUtils.verifyDigest(signer, cert, SIGN_PROVIDER);
            //logger.debug(" -- cmsVerifyDigest: " + cmsVerifyDigest);
            //boolean cmsVerifySignature = CMSUtils.verifySignature(signer, cert, SIGN_PROVIDER);
            //logger.debug(" -- cmsVerifySignature: " + cmsVerifySignature);
        }
    }
    
    
    public boolean hasTimeStampToken() throws Exception {
        //Call this method after isValidSignature()
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        boolean result = false;
        while (it.hasNext()) {
            TimeStampToken timeStampToken = checkTimeStampToken(it.next());
            if(timeStampToken != null) result = true;
        }
        return result;
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
        return reqgen.generate(signerInformation.getDigestAlgOID(), as.getOctets());
    }
    

    private void replaceSigners(CMSSignedData cmsdata) throws Exception {
        logger.debug("replaceSigners");
        SMIMESignedGenerator gen = 
                new SMIMESignedGenerator();
        gen.addAttributeCertificates(cmsdata.getAttributeCertificates());
        gen.addCertificates(cmsdata.getCertificates());
        gen.addSigners(cmsdata.getSignerInfos());
        MimeMultipart mimeMultipart = gen.generate(smimeSigned.getContent(), 
                smimeSigned.getContent().getFileName());
        setContent(mimeMultipart, mimeMultipart.getContentType());
        saveChanges();
    }
    
    @Override public void saveChanges() throws MessagingException {
        logger.debug("saveChanges");
        super.saveChanges(); 
        try {
            initSMIMEMessage();
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        } 
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
        byte[] resultado = baos.toByteArray();
        baos.close();
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