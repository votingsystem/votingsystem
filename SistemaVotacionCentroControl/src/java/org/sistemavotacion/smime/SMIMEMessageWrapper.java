package org.sistemavotacion.smime;

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
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.sistemavotacion.seguridad.PKIXCertPathReviewer;
import org.sistemavotacion.centrocontrol.modelo.*;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class SMIMEMessageWrapper extends MimeMessage {
	
	public enum Tipo {INDEFINIDO, VOTO}
    
    private static Logger logger = LoggerFactory.getLogger(SMIMEMessageWrapper.class);
    
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
    public static final String SIGNED_FILE_EXTENSION = "p7m";

    private String messageId;
    private String fileName;
    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;
    private SignedMailValidator.ValidationResult validationResult = null;
    private boolean isValidSignature = false;
    
	private Set<Usuario> firmantes = new HashSet<Usuario>();
	private Usuario firmante;
	private Tipo tipoMensaje = Tipo.INDEFINIDO;
	private InformacionVoto informacionVoto;
    
    public SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        fileName =  RandomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + ".p7m");
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + ".p7m";
    }

    public static SMIMEMessageWrapper build(InputStream inputStream, String name, Tipo tipo) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        //Properties props = System.getProperties();
        //return new DNIeMimeMessage (Session.getDefaultInstance(props, null), fileInputStream);
        SMIMEMessageWrapper dnieMimeMessage = null;
        try {
            dnieMimeMessage = new SMIMEMessageWrapper (null, inputStream, name, tipo);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        } 
        return dnieMimeMessage;
    }
    
    public static SMIMEMessageWrapper build(InputStream inputStream, String name) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
    	return build(inputStream, name, Tipo.INDEFINIDO);
    }

    public SMIMEMessageWrapper (Session session, InputStream inputStream, String fileName, Tipo tipo) 
            throws IOException, MessagingException, CMSException, SMIMEException, Exception {
        super(session, inputStream);
        this.fileName = fileName;
        this.tipoMensaje = tipo;
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
        if (Tipo.VOTO.equals(tipoMensaje)) {
        	initVoto();
        } else checkSignature();
    }

    @Override
    public void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
    }

    public void setMessageID(String messageId) throws MessagingException {
    	this.messageId = messageId;
    	updateMessageID();
    }
    
    public void setTo(String to) throws MessagingException {
    	setHeader("To", to);
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
    public static boolean isValidSignature(SMIMESigned smimeSigned) throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        logger.debug("signers.size(): " + signers.size());;
        Collection c = signers.getSigners();
        Iterator it = c.iterator();
        boolean result = false;
        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
            logger.debug("Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC)
                    .getCertificate((X509CertificateHolder)certIt.next());
            logger.debug("cert.getSubjectDN(): " + cert.getSubjectDN());
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))){
                logger.debug("signature verified");
                result = true;
            } else {
                logger.debug("signature failed!");
                result = false;
            }
        }
        return result;
    }
    
    public boolean isValidSignature() throws Exception {
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
        logger.debug("signers.size(): " + signers.size());;
        Collection c = signers.getSigners();
        Iterator it = c.iterator();
        boolean result = false;
        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
            logger.debug("Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC)
                    .getCertificate((X509CertificateHolder)certIt.next());
            Usuario usuario = Usuario.getUsuario(cert);
            firmantes.add(usuario);
            setFirmante(usuario);
            logger.debug("cert.getSubjectDN(): " + cert.getSubjectDN());
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))){
                logger.debug("signature verified");
                result = true;
            } else {
                logger.debug("signature failed!");
                result = false;
            }
        }
        isValidSignature = result;
        return result;
    }
    
    public void initVoto() throws Exception {
        // certificates and crls passed in the signature
        Store certs = smimeSigned.getCertificates();
        informacionVoto = new InformacionVoto();
        // SignerInfo blocks which contain the signatures
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        logger.debug("initVoto() - signers.size(): " + signers.size());
        Collection c = signers.getSigners();
        Iterator it = c.iterator();
        boolean result = false;
        // check each signer
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
            logger.debug("Collection matches: " + certCollection.size());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC)
                    .getCertificate((X509CertificateHolder)certIt.next());
            Usuario usuario = Usuario.getUsuario(cert);
            firmantes.add(usuario);
            if (cert.getSubjectDN().toString().contains("OU=hashCertificadoVotoHEX:")) {
            	informacionVoto.setCertificadoVoto(cert);
            } else {informacionVoto.setCertificadoCentroControl(cert);}
            if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert))){
                logger.debug("signature verified");
                result = true;
            } else {
                logger.debug("signature failed!");
                result = false;
            }
        }
        isValidSignature = result;
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
                    logger.debug("Certificate " + i + " ------------- ");
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

	public Tipo getTipoMensaje() {
		return tipoMensaje;
	}

	public void setTipoMensaje(Tipo tipoMensaje) {
		this.tipoMensaje = tipoMensaje;
	}

	public Usuario getFirmante() {
		return firmante;
	}

	public void setFirmante(Usuario firmante) {
		this.firmante = firmante;
	}
	
    public byte[] getBytes () throws IOException, MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        byte[] resultado = baos.toByteArray();
        baos.close();
        return resultado;
    }
    
}
