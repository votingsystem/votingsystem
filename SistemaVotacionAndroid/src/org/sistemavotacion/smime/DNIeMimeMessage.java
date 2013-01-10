package org.sistemavotacion.smime;

import android.util.Log;
import com.sun.mail.util.BASE64DecoderStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSProcessable;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.bouncycastle2.mail.smime.SMIMEException;
import org.bouncycastle2.mail.smime.SMIMESigned;
import org.bouncycastle2.util.Store;
import org.bouncycastle2.x509.PKIXCertPathReviewer;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class DNIeMimeMessage extends MimeMessage {
    
    private static final String RESOURCE_NAME = "org.sistemavotacion.DNIeSignedMailValidatorMessages.properties";
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
    public static final String SIGNED_FILE_EXTENSION = "p7m";

    private String messageId;
    private String fileName;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;
    private DNIeSignedMailValidator.ValidationResult validationResult = null;

    protected DNIeMimeMessage(Session session) throws MessagingException {
        super(session);
        fileName =  RandomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + ".p7m");
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + ".p7m";
    }

    public static DNIeMimeMessage build(InputStream inputStream, String name) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        //Properties props = System.getProperties();
        //return new DNIeMimeMessage (Session.getDefaultInstance(props, null), fileInputStream);
        DNIeMimeMessage dnieMimeMessage = null;
        try {
            dnieMimeMessage = new DNIeMimeMessage (null, inputStream, name);
        } catch (Exception ex) {
            Log.e("DNIeMimeMessage", ex.getMessage(), ex);
            return null;
        } 
        return dnieMimeMessage;
    }

    public DNIeMimeMessage (Session session, InputStream inputStream, String fileName) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        super(session, inputStream);
        this.fileName = fileName;
        if (getContent() instanceof BASE64DecoderStream) {
            smimeSigned = new SMIMESigned(this); 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
            signedContent = baos.toString(); 
        } else {
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
                        Log.i("DNIeMimeMessage","IMPOSIBLE EXTRAER CONTENIDO DE SECCION");
                    }
                }
                signedContent = stringBuilder.toString();
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
            messageId = fileName + "@" + nifUsuario;
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
    public boolean isValidSignature() throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        Collection c = signers.getSigners();
        Iterator it = c.iterator();
        boolean result = false;
        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC)
                    .getCertificate((X509CertificateHolder)certIt.next());
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))){
                Log.i("DNIeMimeMessage","signature verified");
                result = true;
            } else {
                Log.i("DNIeMimeMessage","signature failed!");
                result = false;
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
    
    public DNIeSignedMailValidator.ValidationResult verify(
            PKIXParameters params) throws Exception {
        DNIeSignedMailValidator validator = new DNIeSignedMailValidator(this, params);
        // iterate over all signatures and print results
        Iterator it = validator.getSignerInformationStore().getSigners().iterator();
        Locale loc = Locale.ENGLISH;
        //only one signer supposed!!!
        DNIeSignedMailValidator.ValidationResult result = null;
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            result = validator.getValidationResult(signer);
            if (result.isValidSignature()){
                Log.i("DNIeMimeMessage","isValidSignature");
            }
            else {
                Log.i("DNIeMimeMessage","sigInvalid");
                Log.i("DNIeMimeMessage","Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    Log.i("DNIeMimeMessage","ERROR - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                Log.i("DNIeMimeMessage","Notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    Log.i("DNIeMimeMessage","NOTIFICACION - " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) {
                    Log.i("DNIeMimeMessage","Certificate path valid");
                }
                else {
                    Log.i("DNIeMimeMessage","Certificate path invalid");
                }
                Log.i("DNIeMimeMessage","Certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    Log.i("DNIeMimeMessage","ERROR - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1)
                        .iterator();
                while (notificationsIt.hasNext()) {
                    Log.i("DNIeMimeMessage","NOTIFICACION - " + notificationsIt.next().toString());
                }
                // per certificate errors and notifications
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    Log.i("DNIeMimeMessage","Certificate " + i + "========");
                    Log.i("DNIeMimeMessage","Issuer: " + cert.getIssuerDN().getName());
                    Log.i("DNIeMimeMessage","Subject: " + cert.getSubjectDN().getName());
                    Log.i("DNIeMimeMessage","Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        Log.i("DNIeMimeMessage", errorsIt.next().toString());
                    }
                    // notifications
                    Log.i("DNIeMimeMessage","Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        Log.i("DNIeMimeMessage",notificationsIt.next().toString());
                    }
                    i++;
                }
            }
        }
        validationResult = result;
        return result;
    }

    /**
     * @return the validationResult
     */
    public DNIeSignedMailValidator.ValidationResult getValidationResult() {
        return validationResult;
    }
    
    public File copyContentToFile (File destFile) throws IOException, MessagingException {
        FileOutputStream fos = new FileOutputStream(destFile);
        writeTo(fos);
        fos.close();
        return destFile;
    }
    
}
