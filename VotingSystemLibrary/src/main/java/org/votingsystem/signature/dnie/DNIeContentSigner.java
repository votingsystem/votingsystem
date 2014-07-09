package org.votingsystem.signature.dnie;

import iaik.pkcs.pkcs11.*;
import iaik.pkcs.pkcs11.objects.*;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.util.Store;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SMIMESignedGenerator;
import org.votingsystem.signature.smime.SimpleSignerInfoGeneratorBuilder;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.VotingSystemException;
import org.votingsystem.util.OSValidator;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import static org.votingsystem.model.ContextVS.DNIe_SIGN_MECHANISM;
import static org.votingsystem.model.ContextVS.PROVIDER;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class DNIeContentSigner implements ContentSigner {
    
    private static Logger logger = Logger.getLogger(DNIeContentSigner.class);


    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN = "CertFirmaDigital";
    public static final String CERT_CA = "CertCAIntermediaDGP";
    public static final String DNIe_AUTH_PRIVATE_KEY_LABEL = "KprivAutenticacion";
    public static final String DNIe_SIGN_PRIVATE_KEY_LABEL = "KprivFirmaDigital";

    private RSAPrivateKey signatureKey;
    private X509Certificate certUser;
    private X509Certificate certIntermediate;
    private X509Certificate certCA;

    public static String CERT_STORE_TYPE = "Collection";

    
    private String signatureAlgorithm = null;
    private SignatureOutputStream stream = null;
    private Session pkcs11Session = null;
    private Token pkcs11Token = null;
    private Module pkcs11Module  = null;

    private static Mechanism instanceSignatureMechanism;
    private static String instanceSignatureAlgorithm;

    private DNIeContentSigner(String signatureAlgorithm, Session pkcs11Session, Token pkcs11Token,
                              Module pkcs11Module, RSAPrivateKey signatureKey,
                              X509Certificate certUser, X509Certificate certIntermediate, X509Certificate certCA) {
        stream = new SignatureOutputStream();
        this.signatureAlgorithm = signatureAlgorithm;
        this.pkcs11Session = pkcs11Session;
        this.signatureKey = signatureKey;
        this.certUser = certUser;
        this.certIntermediate = certIntermediate;
        this.certCA = certCA;
        this.pkcs11Token = pkcs11Token;
        this.pkcs11Module = pkcs11Module;
    }

    public static DNIeContentSigner getInstance(char[] password, Mechanism signatureMechanism,
                        String signatureAlgorithm) throws Exception {
        DNIeContentSigner instance = null;
        instanceSignatureMechanism = signatureMechanism;
        instanceSignatureAlgorithm = signatureAlgorithm;
        Module pkcs11Module  = null;
        Session pkcs11Session = null;
        Token token = null;
        RSAPrivateKey signatureKey = null;
        X509Certificate certUser = null;
        X509Certificate certIntermediate = null;
        X509Certificate certCA = null;
        try {
            pkcs11Module  = Module.getInstance(OSValidator.getPKCS11ModulePath());
            pkcs11Module.initialize(null);
            final SlotReader slotReader = new SlotReader(pkcs11Module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT));
            Slot slot = slotReader.getSelected();
            if(slot == null) throw new VotingSystemException(
                    ContextVS.getInstance().getMessage("smartCardReaderErrorMsg"));
            token = slot.getToken();
            if(token == null) throw new VotingSystemException (
                    ContextVS.getInstance().getMessage("missingPKCS11ErrorMsg"));
            pkcs11Session = token.openSession(Token.SessionType.SERIAL_SESSION,
                    Token.SessionReadWriteBehavior.RO_SESSION, null, null);
            pkcs11Session.login(Session.UserType.USER, password);
            RSAPrivateKey templateSignatureKey = new RSAPrivateKey();
            templateSignatureKey.getSign().setBooleanValue(Boolean.TRUE);
            templateSignatureKey.getLabel().setCharArrayValue(DNIe_SIGN_PRIVATE_KEY_LABEL.toCharArray());
            pkcs11Session.findObjectsInit(templateSignatureKey);
            iaik.pkcs.pkcs11.objects.Object[] foundSignatureKeyObjects = pkcs11Session.findObjects(1); // find first
            if (foundSignatureKeyObjects.length > 0) {
                signatureKey = (RSAPrivateKey) foundSignatureKeyObjects[0];
                pkcs11Session.signInit(signatureMechanism, signatureKey);
            } else {
                throw new Exception ("### SIGNATURE KEY NOT FOUND ###");
            }
            pkcs11Session.findObjectsFinal();
            X509PublicKeyCertificate certificateTemplate = new X509PublicKeyCertificate();
            pkcs11Session.findObjectsInit(certificateTemplate);
            Object[] tokenCertificateObjects;
            FileInputStream fis =  new FileInputStream(ContextVS.APPDIR + ContextVS.CERT_RAIZ_PATH);
            certCA = CertUtil.loadCertificateFromStream(fis);
            while ((tokenCertificateObjects = pkcs11Session.findObjects(1)).length > 0) {
                iaik.pkcs.pkcs11.objects.Object object = (Object) tokenCertificateObjects[0];
                Hashtable attributes = object.getAttributeTable();
                ByteArrayAttribute valueAttribute = (ByteArrayAttribute) attributes.get(Attribute.VALUE);
                byte[] value = valueAttribute.getByteArrayValue();
                X509PublicKeyCertificate cert = (X509PublicKeyCertificate)tokenCertificateObjects[0];
                if (CERT_SIGN.equals(cert.getLabel().toString())) {
                    certUser = (X509Certificate)CertUtil.loadCertificate(value);
                    ContextVS.getInstance().setSessionUser(UserVS.getUserVS(certUser));
                } else if (CERT_CA.equals(cert.getLabel().toString())) {
                    certIntermediate = (X509Certificate)CertUtil.loadCertificate(value);
                }
            }
            pkcs11Session.findObjectsFinal();
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new VotingSystemException(ContextVS.getInstance().getMessage("smartCardReaderErrorMsg"));
        } catch (PKCS11Exception ex) {
            if ("CKR_DEVICE_ERROR".equals(ex.getMessage()) ||
                    "CKR_CRYPTOKI_ALREADY_INITIALIZED".equals(ex.getMessage()) ||
                    "CKR_USER_ALREADY_LOGGED_IN".equals(ex.getMessage())) {
                logger.error(ex.getMessage(), ex);
                if (token != null && token.getTokenInfo() != null && token.getTokenInfo().isTokenInitialized()) {
                    token.closeAllSessions();
                }
                if (pkcs11Session != null ) {
                    //pkcs11Session.closeSession(); -> exception
                    pkcs11Session = null;
                    System.gc();
                }
                if (pkcs11Module != null ) {
                    pkcs11Module.finalize(null);
                    pkcs11Module = null;
                }
                System.gc();
            }
            if ("CKR_PIN_INCORRECT".equals(ex.getMessage())) {
                throw new VotingSystemException(ContextVS.getInstance().getMessage("passwordErrorMsg"));
            }
            if ("CKR_HOST_MEMORY".equals(ex.getMessage())) {
                throw new VotingSystemException(ContextVS.getInstance().getMessage("smartCardReaderErrorMsg"));
            }
            throw ex;
        }
        instance = new DNIeContentSigner(signatureAlgorithm, pkcs11Session, token, pkcs11Module,
                signatureKey, certUser, certIntermediate, certCA);
        return instance;
    }

    public Store getCertificates() throws CertificateEncodingException {
        List certList = new ArrayList();
        certList.add(certCA);
        certList.add(certIntermediate);
        certList.add(certUser);
        Store certs = new JcaCertStore(certList);
        return certs;
    }

    public X509Certificate getUserCert() {
         return certUser;
    }

    @Override public AlgorithmIdentifier getAlgorithmIdentifier() {
        return new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
    }

    @Override public OutputStream getOutputStream() {
        return stream;
    }

    @Override public byte[] getSignature() {
        return stream.getSignature();
    }


    /**
     * @return the pkcs11Session
     */
    public Session getPkcs11Session() {
        return pkcs11Session;
    }

    /**
     * @param pkcs11Session the pkcs11Session to set
     */
    public void setPkcs11Session(Session pkcs11Session) {
        this.pkcs11Session = pkcs11Session;
    }

    private static class SlotReader {

        Slot[] slots;

        public SlotReader (Slot[] slots) {
            this.slots = slots;
        }

        public Slot getSelected () {
            if(slots == null || slots.length == 0) return null;
            return slots[0];
        }

    }

    public void closeSession () {
        logger.debug("closeSession");
        try {
            if (pkcs11Token != null && pkcs11Token.getTokenInfo() != null
                    && pkcs11Token.getTokenInfo().isTokenInitialized()) {
                pkcs11Token.closeAllSessions();
            }
            if (pkcs11Session != null ) {
                //pkcs11Session.closeSession(); -> exeption
                pkcs11Session = null;
                System.gc();
            }
            if (pkcs11Module != null ) {
                pkcs11Module.finalize(null);
                pkcs11Module = null;
            }
            System.gc();
        } catch (TokenException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private class SignatureOutputStream extends OutputStream {
        
        //private Signature sig;
        ByteArrayOutputStream bOut;

        //SignatureOutputStream(Signature sig) {
        SignatureOutputStream() {
            bOut = new ByteArrayOutputStream();
        }

        public void write(byte[] bytes, int off, int len) throws IOException {
            bOut.write(bytes, off, len);
        }

        public void write(byte[] bytes) throws IOException {
            bOut.write(bytes);
        }

        public void write(int b) throws IOException {
            bOut.write(b);
        }

        byte[] getSignature() {
            byte[] sigBytes = null;
            try {
               byte[] hashValue = bOut.toByteArray();
               //String hashValueStr = new String(Base64.encode(hashValue));
               //logger.debug(" ------- hashValueStr: " + hashValueStr);               
                sigBytes = pkcs11Session.sign(bOut.toByteArray());
                //String sigBytesStr = new String(Base64.encode(sigBytes));
                //logger.debug(" ------- sigBytesStr: " + sigBytesStr);
                closeSession();
            } catch (Exception ex) {       
                logger.error(ex.getMessage(), ex);            
            }
            return sigBytes;
        }

    }

    public static SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, String textToSign,
                     char[] password, String subject, Header header) throws Exception {
        if (subject == null) subject = "";
        if (textToSign == null) textToSign = "";
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        DNIeContentSigner dnieContentSigner = null;
        dnieContentSigner = DNIeContentSigner.getInstance(
                password, ContextVS.DNIe_SESSION_MECHANISM, DNIe_SIGN_MECHANISM);
        SimpleSignerInfoGeneratorBuilder dnieSignerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        dnieSignerInfoGeneratorBuilder = dnieSignerInfoGeneratorBuilder.setProvider(PROVIDER);
        dnieSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = dnieSignerInfoGeneratorBuilder.build(dnieContentSigner);

        gen.addSignerInfoGenerator(signerInfoGenerator);
        gen.addCertificates(dnieContentSigner.getCertificates());
        // create the base for our message
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textToSign);
        // extract the multipart object from the SMIMESigned object.
        MimeMultipart mimeMultipart = gen.generate(msg, "");

        // Get a Session object and create the mail message
        Properties props = System.getProperties();
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, null);
        String userNIF = null;
        if (ContextVS.getInstance().getSessionUser() != null) userNIF =
                ContextVS.getInstance().getSessionUser().getNif();
        Address fromUserAddress = null;
        if(userNIF != null) fromUserAddress = new InternetAddress(userNIF);
        Address toUserAddress = null;
        if(toUser != null) toUserAddress = new InternetAddress(toUser.replace(" ", ""));

        SMIMEMessageWrapper body = new SMIMEMessageWrapper(session);
        if (header != null) body.setHeader(header.getName(), header.getValue());
        body.setHeader("Content-Type", "text/plain; charset=UTF-8");
        if(fromUserAddress != null) body.setFrom(fromUserAddress);
        if(toUserAddress != null) body.setRecipient(Message.RecipientType.TO, toUserAddress);
        body.setSubject(subject, "UTF-8");
        body.setContent(mimeMultipart, mimeMultipart.getContentType());
        body.updateChanges();
        return body;
    }

}



