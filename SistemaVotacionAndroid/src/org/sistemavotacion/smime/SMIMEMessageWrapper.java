package org.sistemavotacion.smime;

import android.util.Log;
import com.sun.mail.util.BASE64DecoderStream;
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
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.DERUTCTime;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.CMSEnvelopedDataParser;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSProcessable;
import org.bouncycastle2.cms.CMSProcessableByteArray;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.bouncycastle2.mail.smime.SMIMEException;
import org.bouncycastle2.mail.smime.SMIMESigned;
import org.bouncycastle2.util.Store;
import org.bouncycastle2.util.encoders.Base64;
import org.sistemavotacion.modelo.*;
import org.sistemavotacion.seguridad.PKIXCertPathReviewer;
import org.sistemavotacion.util.FileUtils;

import android.util.Log;
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class SMIMEMessageWrapper extends MimeMessage {
    
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
    public static final String SIGNED_FILE_EXTENSION = "p7m";
    
    public static final String DEFAULT_SIGNED_FILE_NAME = "smimeMessage";

    private String messageId;
    private String fileName;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;
    private SignedMailValidator.ValidationResult validationResult = null;
    private Set<Firmante> firmantes;

    public SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        fileName =  RandomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + ".p7m");
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + ".p7m";
    }

    public static SMIMEMessageWrapper build(InputStream inputStream, String name) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        SMIMEMessageWrapper dnieMimeMessage = null;
        try {
            dnieMimeMessage = new SMIMEMessageWrapper (null, inputStream, name);
        } catch (Exception ex) {
        	Log.e("SMIMEMessageWrapper.build", ex.getMessage(), ex);
            return null;
        } 
        return dnieMimeMessage;
    }

    public SMIMEMessageWrapper (Session session, InputStream inputStream, String fileName) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        super(session, inputStream);
        if (fileName == null) this.fileName = DEFAULT_SIGNED_FILE_NAME; 
        this.fileName = fileName;
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
            			File.createTempFile("multipart", "p7s")); 
            	FileDataSource fileDataSource = new FileDataSource(tempFile);
            	smimeSigned = new SMIMESigned(new MimeMultipart(fileDataSource));
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
     * @return the smimeSigned
     * @throws Exception 
     * @throws SMIMEException 
     * @throws CMSException 
     * @throws MessagingException 
     * @throws IOException 
     */
    public static SMIMESigned getSmimeSigned(Session session, 
    		InputStream inputStream, String fileName) throws IOException, 
    		MessagingException, CMSException, SMIMEException, Exception {
    	SMIMEMessageWrapper smimeSignedWrapper = new SMIMEMessageWrapper(session,
    			inputStream, fileName);
        return smimeSignedWrapper.getSmimeSigned();
    }
    
    /**
     * verify that the sig is correct and that it was generated when the 
     * certificate was current(assuming the cert is contained in the message).
     */
    public boolean isValidSignature() throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        //Not nice, as it is encoding and decoding, but it works.
        List list = new ArrayList();
        Collection<X509CertificateHolder> collection = certs.getMatches(null);
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider( "BC" );
        for (X509CertificateHolder col: collection) {
            list.add(certConverter.getCertificate(col));
        }
        CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(list);
        CertStore store = CertStore.getInstance("Collection", ccsp, "BC");
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        Log.i("SMIMEMessageWrapper","signers.size(): " + signers.size());
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        boolean result = false;
        // check each signer
        firmantes = new HashSet<Firmante>();
        while (it.hasNext()) {
            SignerInformation   signer = it.next();
            AttributeTable  attributes = signer.getSignedAttributes();

            DERUTCTime time = null;
            Firmante firmante = new Firmante();
            firmante.setSigner(signer);
            firmante.setContenidoFirmado(getSignedContent());
            String hashEnAtributos = null;
            if (attributes != null) {
                Attribute signingTimeAttribute = attributes.get( CMSAttributes.signingTime );
                time = (DERUTCTime) signingTimeAttribute.getAttrValues().getObjectAt(0);
                firmante.setFechaFirma(time.getDate());
                
                Attribute messageDigestAttribute = attributes.get( CMSAttributes.messageDigest );
                byte[] hash = ((ASN1OctetString)messageDigestAttribute.getAttrValues().getObjectAt(0)).getOctets();
                
                hashEnAtributos = new String(Base64.encode(hash));
                

            //byte[] contentDigest = (byte[])gen.getGeneratedDigests().get(CMSSignedGenerator.DIGEST_SHA1);
            //assertTrue(MessageDigest.isEqual(contentDigest, ((ASN1OctetString)hash.getAttrValues().getObjectAt(0)).getOctets()));
                
                
                
            }            
            Collection          certCollection = certs.getMatches(signer.getSID());
            Log.i("isValidSignature()", "Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();

            
            X509Certificate cert = certConverter.getCertificate((X509CertificateHolder)certIt.next());
            firmante.setUsuario(Usuario.getUsuario(cert));
            firmante.setCert(cert);
            
            
            
            Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
          //  firmante.setCertPath(CertUtil.verifyCertificate(cert, store, anchors));
            
//            Log.i("SMIMEMessageWrapper","Tamaño ruta: " + firmante.getCertPath().getCertificates().size());

            firmantes.add(firmante);
            Log.i("SMIMEMessageWrapper","cert.getSubjectDN(): " + cert.getSubjectDN() + 
            		  " - Not before: " + cert.getNotBefore() + " - Not after: " + cert.getNotAfter() + 
            		  " - Signing Time: " + signer.getSigningTime().getDate());
            try {
                if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))){
                    Log.i("SMIMEMessageWrapper","signature verified");
                    result = true;
                } else {
                    Log.i("SMIMEMessageWrapper","signature failed!");
                    result = false;
                }
                Log.i("SMIMEMessageWrapper","hashEnAtributos: '" + hashEnAtributos + "' hash en firmante:" +  firmante.getContentDigestBase64());
            } catch(Exception ex) {
            	Log.e("SMIMEMessageWrapper", ex.getMessage(), ex);
                Log.i("SMIMEMessageWrapper","hashEnAtributos: '" + hashEnAtributos + "' hash en firmante:" +  firmante.getContentDigestBase64());
                return false;
            }
            
        }
        return result;
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
                Log.i("SMIMEMessageWrapper","isValidSignature");
            }
            else {
                Log.i("SMIMEMessageWrapper","sigInvalid");
                Log.i("SMIMEMessageWrapper","Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","ERROR - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                Log.i("SMIMEMessageWrapper","Notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","NOTIFICACION - " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) {
                    Log.i("SMIMEMessageWrapper","Certificate path valid");
                }
                else {
                    Log.i("SMIMEMessageWrapper","Certificate path invalid");
                }
                Log.i("SMIMEMessageWrapper","Certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","ERROR - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1)
                        .iterator();
                while (notificationsIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","NOTIFICACION - " + notificationsIt.next().toString());
                }
                // per certificate errors and notifications
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    Log.i("SMIMEMessageWrapper","Certificate " + i + "========");
                    Log.i("SMIMEMessageWrapper","Issuer: " + cert.getIssuerDN().getName());
                    Log.i("SMIMEMessageWrapper","Subject: " + cert.getSubjectDN().getName());
                    Log.i("SMIMEMessageWrapper","Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        Log.i("SMIMEMessageWrapper", errorsIt.next().toString());
                    }
                    // notifications
                    Log.i("SMIMEMessageWrapper","Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        Log.i("SMIMEMessageWrapper",notificationsIt.next().toString());
                    }
                    i++;
                }
            }
        }
        validationResult = result;
        return result;
    }
    
    public static SignedMailValidator.ValidationResult verify(
            SignedMailValidator validator) throws Exception {
        // iterate over all signatures and print results
        Iterator it = validator.getSignerInformationStore().getSigners().iterator();
        Locale loc = Locale.ENGLISH;
        //only one signer supposed!!!
        SignedMailValidator.ValidationResult result = null;
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            result = validator.getValidationResult(signer);
            if (result.isValidSignature()){
                Log.i("SMIMEMessageWrapper","isValidSignature");
            }
            else {
                Log.i("SMIMEMessageWrapper","sigInvalid");
                Log.i("SMIMEMessageWrapper","Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","ERROR - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                Log.i("SMIMEMessageWrapper","Notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","NOTIFICACION - " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) {
                    Log.i("SMIMEMessageWrapper","Certificate path valid");
                }
                else {
                    Log.i("SMIMEMessageWrapper","Certificate path invalid");
                }
                Log.i("SMIMEMessageWrapper","Certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","ERROR - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1)
                        .iterator();
                while (notificationsIt.hasNext()) {
                    Log.i("SMIMEMessageWrapper","NOTIFICACION - " + notificationsIt.next().toString());
                }
                // per certificate errors and notifications
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    Log.i("SMIMEMessageWrapper","Certificate " + i + "========");
                    Log.i("SMIMEMessageWrapper","Issuer: " + cert.getIssuerDN().getName());
                    Log.i("SMIMEMessageWrapper","Subject: " + cert.getSubjectDN().getName());
                    Log.i("SMIMEMessageWrapper","Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        Log.i("SMIMEMessageWrapper", errorsIt.next().toString());
                    }
                    // notifications
                    Log.i("SMIMEMessageWrapper","Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        Log.i("SMIMEMessageWrapper",notificationsIt.next().toString());
                    }
                    i++;
                }
            }
        }
        return result;
    }

    /**
     * @return the validationResult
     */
    public SignedMailValidator.ValidationResult getValidationResult() {
        return validationResult;
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

    /**
     * @return the firmantes
     */
    public Set<Firmante> getFirmantes() {
        return firmantes;
    }
    
}
