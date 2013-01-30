package org.sistemavotacion.smime;

import static org.sistemavotacion.android.Aplicacion.DEFAULT_SIGNED_FILE_NAME;
import static org.sistemavotacion.android.Aplicacion.SIGNED_PART_EXTENSION;
import static org.sistemavotacion.android.Aplicacion.SIGN_PROVIDER;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
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
import java.util.Random;
import java.util.Set;

import javax.activation.FileDataSource;
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

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.DEREncodable;
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
import org.sistemavotacion.modelo.Firmante;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.seguridad.PKIXCertPathReviewer;
import org.sistemavotacion.seguridad.TimeStampWrapper;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;

import android.util.Log;

import com.sun.mail.util.BASE64DecoderStream;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class SMIMEMessageWrapper extends MimeMessage {
	
	public static final String TAG = "SMIMEMessageWrapper";

    private String messageId;
    private String fileName;
    private File file;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;
    private boolean isValidSignature = false;
    
    private Set<Firmante> firmantes;

    
    public Set<Firmante> getFirmantes() {
        return firmantes;
    }
    
    private SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        fileName =  RandomLowerString(System.currentTimeMillis(), 7);
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

    public SMIMEMessageWrapper (Session session, File file) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
    	this(session, new FileInputStream(file), file.getName());
    	this.file = file;
    }
    
    private void initSMIMEMessage() throws IOException, MessagingException, 
    	CMSException, SMIMEException, Exception{
     	Log.d("SMIMEMessageWrapper", " -initSMIMEMessage - getContent().getClass(): " + getContent().getClass());
    	if (getContent() instanceof BASE64DecoderStream) {
             smimeSigned = new SMIMESigned(this); 
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
             signedContent = baos.toString(); 
         } else if (getContent() instanceof MimeMultipart ||
         		getContent() instanceof SharedByteArrayInputStream) {
         	if (getContent() instanceof SharedByteArrayInputStream) {
             	File tempFile = FileUtils.copyStreamToFile(
             			(SharedByteArrayInputStream)getContent(), 
             			File.createTempFile("multipart", SIGNED_PART_EXTENSION)); 
             	FileDataSource fileDataSource = new FileDataSource(tempFile);
             	smimeSigned = new SMIMESigned(new MimeMultipart(fileDataSource));
             	tempFile.deleteOnExit();
         	} else smimeSigned = new SMIMESigned((MimeMultipart)getContent());
             MimeBodyPart content = smimeSigned.getContent();
             Object  cont = content.getContent();
             if (cont instanceof String) {
                 signedContent = (String)cont;
             } else if (cont instanceof Multipart || 
             		cont instanceof ByteArrayInputStream){
             	Multipart multipart;
             	if(cont instanceof ByteArrayInputStream) {
                 	File tempFile = FileUtils.copyStreamToFile(
                 			(SharedByteArrayInputStream)getContent(), 
                 			File.createTempFile("signedContent", "json")); 
                 	FileDataSource fileDataSource = new FileDataSource(tempFile);
                 	multipart = new MimeMultipart(fileDataSource);
             	}
             	else multipart = (Multipart)cont;
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
         isValidSignature = checkSignature(); 
    }

    @Override
    public void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
    }

    public void setContent (byte[] content) throws MessagingException {
            setContent(content, contentType);
            saveChanges();
    }

    public void updateMessageID(String nifUsuario) throws MessagingException {
            messageId = getFileName() + "@" + nifUsuario;
            Address[] addresses = {new InternetAddress(nifUsuario)};
            addFrom(addresses);
            updateMessageID(); 
    }
	
    public static String RandomLowerString(long seed, int size) {
        StringBuffer tmp = new StringBuffer();
        Random random = new Random(seed);
        for (int i = 0; i < size; i++) {
            long newSeed = random.nextLong();
            int currInt = (int) (26 * random.nextFloat());
            currInt += 97;
            random = new Random(newSeed);
            tmp.append((char) currInt);
        }
        return tmp.toString();
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
                    .setProvider(SIGN_PROVIDER).getCertificate(
                    (X509CertificateHolder)certIt.next());
    		Log.d(TAG + ".isValidSignature(...) ", 
    				"cert.getSubjectDN(): " + cert.getSubjectDN());
    		Log.d(TAG + ".isValidSignature(...) ", 
    				"cert.getNotBefore(): " + cert.getNotBefore());
    		Log.d(TAG + ".isValidSignature(...) ", 
    				"cert.getNotAfter(): " + cert.getNotAfter());    		
    		

            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(SIGN_PROVIDER).build(cert))){
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
    private boolean checkSignature() throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
    	Log.d(TAG + ".checkSignature() ", "signers.size(): " + signers.size()); ;
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        boolean result = false;
        // check each signer
        firmantes = new HashSet<Firmante>();
        while (it.hasNext()) {
            Log.d(TAG, "----------------------- Signer -----------------------------------");
            SignerInformation   signer = it.next();
            AttributeTable  attributes = signer.getSignedAttributes();
            DERUTCTime time = null;
            Firmante firmante = new Firmante();
            firmante.setSigner(signer);
            firmante.setContenidoFirmado(getSignedContent());
            byte[] hash = null;
            if (attributes != null) {
                Attribute signingTimeAttribute = attributes.get(CMSAttributes.signingTime);
                time = (DERUTCTime) signingTimeAttribute.getAttrValues().getObjectAt(0);
                firmante.setFechaFirma(time.getDate());
                Attribute messageDigestAttribute = attributes.get( CMSAttributes.messageDigest );
                hash = ((ASN1OctetString)messageDigestAttribute.getAttrValues().getObjectAt(0)).getOctets();
                String hashStr = new String(Base64.encode(hash));
                Log.d(TAG, " -- hashStr: " + hashStr);
            }   
            Collection certCollection = certs.getMatches(signer.getSID());
            Log.d(TAG, "Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(SIGN_PROVIDER).getCertificate(
                    (X509CertificateHolder)certIt.next());

            firmante.setUsuario(Usuario.getUsuario(cert));
            firmante.setCert(cert);
            firmantes.add(firmante);
            Log.d(TAG, "cert.getSubjectDN(): " + cert.getSubjectDN());
            SignerInformationVerifier siv = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(SIGN_PROVIDER).build(cert);
            if (signer.verify(siv)){
                Log.d(TAG, "signature verified");
                result = true;
                firmante.setTimeStampToken(checkTimeStampToken(signer));//method can only be called after verify.
            } else {
                Log.d(TAG, "signature failed!");
                result = false;
            }
            byte[] digestParams = signer.getDigestAlgParams();
            String digestParamsStr = new String(Base64.encode(hash));
            Log.d(TAG, " -- digestParamsStr: " + digestParamsStr);
            
            // boolean cmsVerifyDigest = CMSUtils.verifyDigest(signer, cert, SIGN_PROVIDER);
            // Log.d(TAG, " -- cmsVerifyDigest: " + cmsVerifyDigest);
            // boolean cmsVerifySignature = CMSUtils.verifySignature(signer, cert, SIGN_PROVIDER);
            // Log.d(TAG, " -- cmsVerifySignature: " + cmsVerifySignature);
        }
        return result;
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
    
    public File setTimeStampToken(TimeStampWrapper timeStampWrapper) throws Exception {
        if(timeStampWrapper == null || timeStampWrapper.getTimeStampToken() == null) 
            throw new Exception("NULL_TIME_STAMP_TOKEN");
        TimeStampToken timeStampToken = timeStampWrapper.getTimeStampToken();
        byte[] hashTokenBytes = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
        String hashTokenStr = new String(Base64.encode(hashTokenBytes));
        Log.d(TAG, "setTimeStampToken - timeStampToken - hashTokenStr: " +  hashTokenStr);
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        List<SignerInformation> newSigners = new ArrayList<SignerInformation>();
        while (it.hasNext()) {
            SignerInformation signer = it.next();
            byte[] digestBytes = signer.getContentDigest();//method can only be called after verify.
            String digestStr = new String(Base64.encode(digestBytes));
            Log.d(TAG, "setTimeStampToken - hash firmante: " +  digestStr + 
                    " - hash token: " + hashTokenStr);
            if(hashTokenStr.equals(digestStr)) {
                Log.d(TAG, "setTimeStampToken - firmante");
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable == null) {
                    Log.d(TAG, "setTimeStampToken - signer without UnsignedAttributes - actualizando token");
                    updatedSigner = 
                            signer.replaceUnsignedAttributes(signer, timeStampWrapper.
                            		getTimeStampTokenAsAttributeTable());
                    newSigners.add(updatedSigner);
                } else {
                    Log.d(TAG, "setTimeStampToken - signer with UnsignedAttributes - actualizando token");
                    Hashtable hashTable = attributeTable.toHashtable();
                    hashTable.put(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, timeStampWrapper.getTimeStampTokenAsAttribute());
                    attributeTable = new AttributeTable(hashTable);
                    updatedSigner = signer.replaceUnsignedAttributes(signer, 
                    		timeStampWrapper.getTimeStampTokenAsAttributeTable());
                    newSigners.add(updatedSigner);
                }
            } else newSigners.add(signer);
        }
        Log.d(TAG, "setTimeStampToken - num. firmantes: " + newSigners.size());
        SignerInformationStore newSignersStore = new SignerInformationStore(newSigners);
        CMSSignedData cmsdata = smimeSigned.replaceSigners(smimeSigned, newSignersStore);
        replaceSigners(cmsdata);
        if(file != null) writeTo(new FileOutputStream(file));
        return file;
    }
    
    public TimeStampRequest getTimeStampRequest(String timeStampRequestAlg) {
        SignerInformation signerInformation = ((SignerInformation)
                        smimeSigned.getSignerInfos().getSigners().iterator().next());
        if(signerInformation == null) return null;
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
        //String digest = Base64.encodeToString(as.getOctets(), Base64.DEFAULT);
        //Log.d(TAG + ".obtenerSolicitudAcceso(...)", " - digest: " + digest);
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(timeStampRequestAlg, as.getOctets());
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
    
    public static PKIXParameters getPKIXParameters (X509Certificate... certs) 
            throws InvalidAlgorithmParameterException{
        return getPKIXParameters(Arrays.asList(certs));
    }
    
    public static PKIXParameters getPKIXParameters (Collection<X509Certificate> certs) 
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
                    Log.d(TAG, "Certificate " + i + "========");
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