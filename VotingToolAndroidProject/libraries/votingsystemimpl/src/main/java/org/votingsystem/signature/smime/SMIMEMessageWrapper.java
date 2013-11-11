package org.votingsystem.signature.smime;

import android.util.Log;

import com.sun.mail.util.BASE64DecoderStream;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1InputStream;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.DEREncodable;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.DERUTCTime;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSProcessable;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.SignerInformationVerifier;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.mail.smime.SMIMEException;
import org.bouncycastle2.mail.smime.SMIMESigned;
import org.bouncycastle2.util.Store;
import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.model.UserVSBase;
import org.votingsystem.signature.util.PKIXCertPathReviewer;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.activation.CommandMap;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.SharedByteArrayInputStream;

import static org.votingsystem.model.ContextVS.DEFAULT_SIGNED_FILE_NAME;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIGNED_PART_EXTENSION;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMEMessageWrapper extends MimeMessage {
	
	public static final String TAG = "SMIMEMessageWrapper";
	
    static {
        MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();

        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        
        mc.addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.pkcs7_signature");
        mc.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.pkcs7_mime");
        mc.addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.x_pkcs7_signature");
        mc.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.x_pkcs7_mime");
        mc.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.multipart_signed");

        CommandMap.setDefaultCommandMap(mc);
    }

    private String messageId;
    private String fileName;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;
    private boolean isValidSignature = false;
	private byte[] messageBytes = null;
    private Set<UserVSBase> userVSBases;

    
    public Set<UserVSBase> getUserVSBases() {
        return userVSBases;
    }
    
    public SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        fileName =  StringUtils.randomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + 
        		SIGNED_PART_EXTENSION);
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + 
        		SIGNED_PART_EXTENSION;
    }

    public SMIMEMessageWrapper (Session session, InputStream inputStream, String fileName) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        super(session, inputStream);
        //Properties props = System.getProperties();
        //Session.getDefaultInstance(props, null);
        if (fileName == null) this.fileName = DEFAULT_SIGNED_FILE_NAME; 
        else this.fileName = fileName;
        initSMIMEMessage();
    }
    
    private void initSMIMEMessage() throws IOException, MessagingException, 
    	CMSException, SMIMEException, Exception{
     	Log.d("SMIMEMessageWrapper", " -initSMIMEMessage - getContent().getClass(): " + getContent().getClass());
    	if (getContent() instanceof BASE64DecoderStream) {
             smimeSigned = new SMIMESigned(this); 
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
             signedContent = baos.toString();
             baos.close();
        } else if (getContent() instanceof SharedByteArrayInputStream) {
         	File tempFile = FileUtils.copyStreamToFile(
         			(SharedByteArrayInputStream)getContent(), 
         			File.createTempFile("multipart", SIGNED_PART_EXTENSION)); 
         	FileDataSource fileDataSource = new FileDataSource(tempFile);
         	smimeSigned = new SMIMESigned(new MimeMultipart(fileDataSource));
         	tempFile.deleteOnExit();
     	} else if (getContent() instanceof MimeMultipart) {
         		smimeSigned = new SMIMESigned((MimeMultipart)getContent());
         		MimeBodyPart content = smimeSigned.getContent();
         		Object  cont = content.getContent();
             	if (cont instanceof String) {
             		signedContent = (String)cont;
             	} else if (cont instanceof Multipart || 
             		cont instanceof ByteArrayInputStream){
             	Multipart multipart;
             	if(cont instanceof ByteArrayInputStream) {
             		if(getContent() instanceof MimeMultipart) {
             			multipart = (Multipart) getContent();
             		} else {
             			File tempFile = FileUtils.copyStreamToFile(
                     			(SharedByteArrayInputStream)getContent(), 
                     			File.createTempFile("signedContent", "json")); 
                     	FileDataSource fileDataSource = new FileDataSource(tempFile);
                     	multipart = new MimeMultipart(fileDataSource);
             		}
             	} else multipart = (Multipart)cont;
                 BodyPart bodyPart = multipart.getBodyPart(0);
                 Object part = bodyPart.getContent();
                 if (part instanceof String) {
                 	signedContent = (String)part;
                 } else if (part instanceof BASE64DecoderStream ||
                 		part instanceof ByteArrayInputStream) {
                     InputStreamReader isr;
                     if(part instanceof ByteArrayInputStream) 
                     	isr = new InputStreamReader((ByteArrayInputStream)part);
                     else isr = new InputStreamReader((BASE64DecoderStream)part);
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
                 	Log.d("SMIMEMessageWrapper", "IMPOSIBLE EXTRAER CONTENIDO FIRMADO");
                 }
             }
        }
        checkSignature(); 
    }
    
    public void save() throws Exception {
    	Log.d("SMIMEMessageWrapper", "save");
        super.saveChanges();
        initSMIMEMessage();
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
     * @return the smimeSigned
     */
    public SMIMESigned getSmimeSigned() {
        return smimeSigned;
    }


    /**
     * verify that the sig is correct and that it was generated when the 
     * certificate was current(assuming the cert is contained in the message).
     */
    public static boolean isValidSignature(SMIMESigned smimeSigned) throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
		Log.d(TAG + ".isValidSignature(...) ", 
				"signers.size(): " + signers.size());
        Iterator it = signers.getSigners().iterator();
        boolean result = false;
        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
    		Log.d(TAG + ".isValidSignature(...) ", 
    				"Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(PROVIDER).getCertificate(
                    (X509CertificateHolder)certIt.next());
    		Log.d(TAG + ".isValidSignature(...) ", 
    				"cert.getSubjectDN(): " + cert.getSubjectDN());
    		Log.d(TAG + ".isValidSignature(...) ", 
    				"cert.getNotBefore(): " + cert.getNotBefore());
    		Log.d(TAG + ".isValidSignature(...) ", 
    				"cert.getNotAfter(): " + cert.getNotAfter());    		
    		

            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(PROVIDER).build(cert))){
        		Log.d(TAG + ".isValidSignature(...) ", "signature verified"); 
                result = true;
            } else {
            	Log.d(TAG + ".isValidSignature(...) ", "signature failed!"); 
                result = false;
            }
        }
        return result;
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
    	Log.d(TAG + ".checkSignature() ", "signers.size(): " + signers.size()); ;
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        // check each signer
        userVSBases = new HashSet<UserVSBase>();
        isValidSignature = false;
        while (it.hasNext()) {
            Log.d(TAG, "----------------------- Signer -----------------------------------");
            SignerInformation   signer = it.next();
            AttributeTable  attributes = signer.getSignedAttributes();
            DERUTCTime time = null;
            Collection certCollection = certs.getMatches(signer.getSID());
            Log.d(TAG, "Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(PROVIDER).getCertificate(
                            (X509CertificateHolder)certIt.next());

            UserVSBase userVSBase = UserVSBase.getUsuario(cert);
            userVSBase.setSigner(signer);
            userVSBase.setContenidoFirmado(getSignedContent());
            byte[] hash = null;
            if (attributes != null) {
                Attribute signingTimeAttribute = attributes.get(CMSAttributes.signingTime);
                time = (DERUTCTime) signingTimeAttribute.getAttrValues().getObjectAt(0);
                userVSBase.setFechaFirma(time.getDate());
                Attribute messageDigestAttribute = attributes.get( CMSAttributes.messageDigest );
                hash = ((ASN1OctetString)messageDigestAttribute.getAttrValues().getObjectAt(0)).getOctets();
                String hashStr = new String(Base64.encode(hash));
                Log.d(TAG, " -- hashStr: " + hashStr);
            }   



            userVSBase.setCertificate(cert);
            userVSBases.add(userVSBase);
            Log.d(TAG, "cert.getSubjectDN(): " + cert.getSubjectDN());
            SignerInformationVerifier siv = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(PROVIDER).build(cert);
            isValidSignature = signer.verify(siv);
            if(isValidSignature) {
            	userVSBase.setTimeStampToken(checkTimeStampToken(signer));//method can only be called after verify.
            } else {
            	Log.d(TAG, "signature failed!");
            	return;
            }
            byte[] digestParams = signer.getDigestAlgParams();
            String digestParamsStr = new String(Base64.encode(hash));
            Log.d(TAG, " -- digestParamsStr: " + digestParamsStr);
            
            // boolean cmsVerifyDigest = CMSUtils.verifyDigest(signer, cert, PROVIDER);
            // Log.d(TAG, " -- cmsVerifyDigest: " + cmsVerifyDigest);
            // boolean cmsVerifySignature = CMSUtils.verifySignature(signer, cert, PROVIDER);
            // Log.d(TAG, " -- cmsVerifySignature: " + cmsVerifySignature);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        messageBytes = baos.toByteArray();
        baos.close();
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
        String digestStr = new String(Base64.encode(digestBytes));
        AttributeTable  unsignedAttributes = signer.getUnsignedAttributes();
        if(unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(
                    PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if(timeStampAttribute != null) {
                DEREncodable dob = timeStampAttribute.getAttrValues().getObjectAt(0);
                org.bouncycastle2.cms.CMSSignedData signedData = 
                        new org.bouncycastle2.cms.CMSSignedData(dob.getDERObject().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                byte[] hashToken = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
                String hashTokenStr = new String(Base64.encode(hashToken));
                Calendar cal = new GregorianCalendar();
                cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                Log.d(TAG, "checkTimeStampToken - timeStampToken - fecha: " 
                        +  DateUtils.getStringFromDate(cal.getTime()));
                Log.d(TAG, "checkTimeStampToken - digestStr: " + digestStr);
                Log.d(TAG, "checkTimeStampToken - timeStampToken - hashTokenStr: " +  hashTokenStr);
                return timeStampToken;
            }
        } else Log.d(TAG, " --- without unsignedAttributes"); 
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
            	Log.d(TAG, "setTimeStampToken - firmante"); 
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable != null) {
                	Log.d(TAG, "setTimeStampToken - signer with UnsignedAttributes"); 
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
        //logger.debug("setTimeStampToken - num. userVSBases: " + newSigners.size());
        SignerInformationStore newSignersStore = new SignerInformationStore(newSigners);
        CMSSignedData cmsdata = smimeSigned.replaceSigners(smimeSigned, newSignersStore);
        replaceSigners(cmsdata);
    }
    
    public TimeStampRequest getTimeStampRequest() {
    	Log.d(TAG , "getTimeStampRequest");
        SignerInformation signerInformation = ((SignerInformation)
                        smimeSigned.getSignerInfos().getSigners().iterator().next());
        if(signerInformation == null) return null;
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
        //String digest = Base64.encodeToString(as.getOctets(), Base64.DEFAULT);
        //Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - digest: " + digest);
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        reqgen.setCertReq(true);
        //reqgen.setReqPolicy(m_sPolicyOID);
        Log.d(TAG, " ========= getTimeStampRequest - signerInformation.getDigestAlgOID(): " + 
        		signerInformation.getDigestAlgOID()); 
        return reqgen.generate(signerInformation.getDigestAlgOID(), as.getOctets());
    }
    

    private void replaceSigners(CMSSignedData cmsdata) throws Exception {
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

    public byte[] getBytes () throws IOException, MessagingException {
        return messageBytes;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
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
                Log.d(TAG, "isValidSignature");
            }
            else {
                Log.d(TAG, "sigInvalid");
                Log.d(TAG, "Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    Log.d(TAG, "ERROR - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                Log.d(TAG, "Notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    Log.d(TAG, "NOTIFICACION - " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) {
                    Log.d(TAG, "Certificate path valid");
                }
                else {
                    Log.d(TAG, "Certificate path invalid");
                }
                Log.d(TAG, "Certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    Log.d(TAG, "ERROR - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1)
                        .iterator();
                while (notificationsIt.hasNext()) {
                    Log.d(TAG, "NOTIFICACION - " + notificationsIt.next().toString());
                }
                // per certificate errors and notifications
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    Log.d(TAG, "Certificate " + i + "-------------------");
                    Log.d(TAG, "Issuer: " + cert.getIssuerDN().getName());
                    Log.d(TAG, "Subject: " + cert.getSubjectDN().getName());
                    Log.d(TAG, "Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        Log.d(TAG,  errorsIt.next().toString());
                    }
                    // notifications
                    Log.d(TAG, "Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        Log.d(TAG, notificationsIt.next().toString());
                    }
                    i++;
                }
            }
        }
        return result;
    }
    
    
}