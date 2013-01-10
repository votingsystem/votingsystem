package org.sistemavotacion.seguridad;

import static org.sistemavotacion.Contexto.*;

import iaik.pkcs.pkcs11.*;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.ByteArrayAttribute;
import iaik.pkcs.pkcs11.objects.Object;
import iaik.pkcs.pkcs11.objects.RSAPrivateKey;
import iaik.pkcs.pkcs11.objects.X509PublicKeyCertificate;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import java.io.*;
import java.security.*;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509CRLSelector;
import java.util.Arrays;
import java.util.Collection;
import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.bouncycastle.asn1.*;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Usuario;
import org.sistemavotacion.smime.CMSUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.OSValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DNIePDFSessionHelper extends CMSSignedGenerator {

    private static Logger logger = 
            LoggerFactory.getLogger(DNIePDFSessionHelper.class);

    private static Session pkcs11Session;
    private static Module pkcs11Module;
    private static Token token;
    public static RSAPrivateKey signatureKey;
    public static X509Certificate certificadoUsuario = null;
    public static X509Certificate certificadoIntermedio = null;
    public static X509Certificate certificadoCA = null;
            
    private static Certificate[] chain = new Certificate[3];
    List signerInfs;
    DNIePDFSessionHelper INSTANCIA;

    private class SignerInf {

       private final SignerIdentifier            signerIdentifier;
        private final String                      digestOID = PDF_DIGEST_OID;
        private final String                      encOID = ENCRYPTION_RSA;
        private final CMSAttributeTableGenerator  sAttr;
        private final CMSAttributeTableGenerator  unsAttr;
        private final AttributeTable              baseSignedTable;
        private Session pkcs11Session;

        SignerInf(SignerIdentifier signerIdentifier, Session pkcs11Session, 
                CMSAttributeTableGenerator unsAttr) {
            this.pkcs11Session = pkcs11Session;
            this.signerIdentifier = signerIdentifier;
            this.sAttr = new DefaultSignedAttributeTableGenerator();
            this.unsAttr = unsAttr;
            this.baseSignedTable = null;
        }

        AlgorithmIdentifier getDigestAlgorithmID() {
            return new AlgorithmIdentifier(new DERObjectIdentifier(digestOID), new DERNull());
        }

        SignerInfo toSignerInfo( DERObjectIdentifier contentType, CMSProcessable content,
            SecureRandom random, Provider sigProvider, boolean addDefaultAttributes,
            boolean isCounterSignature) throws Exception {

            AlgorithmIdentifier digAlgId = getDigestAlgorithmID();
            String digestName = CMSUtils.getDigestId(digestOID);
            String signatureName = digestName + "with" + CMSUtils.getEncryptiontId(encOID);
            AlgorithmIdentifier encAlgId = getEncAlgorithmIdentifier(encOID, null);


            ByteArrayOutputStream out = null;
            if (content != null) {
            	out = new ByteArrayOutputStream();
                content.write(out);
                out.close();
            }
            /*byte[] helpBuffer;
             * ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer)) >= 0) {
              if (bytesRead < dataBuffer.length) {
                helpBuffer = new byte[bytesRead]; // we need a buffer that only holds what to send for digesting
                System.arraycopy(dataBuffer, 0, helpBuffer, 0, bytesRead);
                pkcs11Session.digestUpdate(helpBuffer);
              } else pkcs11Session.digestUpdate(dataBuffer);
            }*/
            ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
            MessageDigest softwareDigestEngine = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
            int bytesRead;
            byte[] dataBuffer = new byte[4096];
            while ((bytesRead = bais.read(dataBuffer)) >= 0) {
              softwareDigestEngine.update(dataBuffer, 0, bytesRead);
            }
            byte[] hash = softwareDigestEngine.digest();
            
            digests.put(digestOID, hash.clone());
            
            AttributeTable signed;
            if (addDefaultAttributes) {
                Map parameters = getBaseParameters(contentType, digAlgId, hash);
                signed = (sAttr != null) ? sAttr.getAttributes(Collections.unmodifiableMap(parameters)) : null;
            }
            else signed = baseSignedTable;
            ASN1Set signedAttr = null;
            byte[] tmp;
            if (signed != null) {
                if (isCounterSignature) {
                    Hashtable tmpSigned = signed.toHashtable();
                    tmpSigned.remove(CMSAttributes.contentType);
                    signed = new AttributeTable(tmpSigned);
                }
                // TODO Validate proposed signed attributes
                signedAttr = getAttributeSet(signed);
                // sig must be composed from the DER encoding.
                tmp = signedAttr.getEncoded(ASN1Encodable.DER);
            }
            else {
                // TODO Use raw signature of the hash value instead
                ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                if (content != null) content.write(bOut);
                tmp = bOut.toByteArray();
            }
            // This signing operation is implemented in most of the drivers
            byte[] sigBytes = pkcs11Session.sign(tmp);
            ASN1Set unsignedAttr = null;
            SignerInfo signerInfo = new SignerInfo(signerIdentifier, digAlgId,
                signedAttr, encAlgId, new DEROctetString(sigBytes), unsignedAttr);
            if (unsAttr != null) {
                Map parameters = getBaseParameters(contentType, digAlgId, hash);
                parameters.put(CMSAttributeTableGenerator.SIGNATURE, sigBytes.clone());
                AttributeTable unsigned = unsAttr.getAttributes(Collections.unmodifiableMap(parameters));
                // TODO Validate proposed unsigned attributes
                unsignedAttr = getAttributeSet(unsigned);
            }
            return new SignerInfo(signerIdentifier, digAlgId,
                signedAttr, encAlgId, new DEROctetString(sigBytes), unsignedAttr);
        }
    }

    public DNIePDFSessionHelper (char[] password, Mechanism signatureMechanism) throws Exception {
    	logger.debug("construyendo DNIePDFSessionHelper");
        getSession(password, signatureMechanism);
        INSTANCIA = this;
    }

    public static Certificate[] getCertificateChain() {
        List<Certificate> certList = new ArrayList();
        int numCerts = 0;
        //EL oredn es importante!!!
        if(certificadoUsuario != null) {
            certList.add(certificadoUsuario);
            numCerts++;
        } 
        if(certificadoIntermedio != null) {
            certList.add(certificadoIntermedio);   
            numCerts++;
        } 
        if(certificadoCA != null) {
            certList.add(certificadoCA);
            numCerts++;
        } 
        Certificate[] certificados = new Certificate[numCerts];
        certList.toArray(certificados);
        return certificados;
    }
    


    public CMSSignedData generarCMSSignedData(String eContentType,
            CMSProcessable content, boolean encapsulate, Provider sigProvider,
            boolean addDefaultAttributes, List signerInfs)
            throws NoSuchAlgorithmException, CMSException, Exception {
// TODO if (signerInfs.isEmpty()){
//            /* RFC 3852 5.2
//             * "In the degenerate case where there are no signers, the
//             * EncapsulatedContentInfo value being "signed" is irrelevant.  In this
//             * case, the content type within the EncapsulatedContentInfo value being
//             * "signed" MUST be id-data (as defined in section 4), and the content
//             * field of the EncapsulatedContentInfo value MUST be omitted."
//             */
//            if (encapsulate) {
//                throw new IllegalArgumentException("no signers, encapsulate must be false");
//            } if (!DATA.equals(eContentType)) {
//                throw new IllegalArgumentException("no signers, eContentType must be id-data");
//            }
//        }
//        if (!DATA.equals(eContentType)) {
//            /* RFC 3852 5.3
//             * [The 'signedAttrs']...
//             * field is optional, but it MUST be present if the content type of
//             * the EncapsulatedContentInfo value being signed is not id-data.
//             */
//            // TODO signedAttrs must be present for all signers
//        }
        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();
        ASN1EncodableVector signerInfos = new ASN1EncodableVector();
        digests.clear();  // clear the current preserved digest state
        Iterator it = _signers.iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation)it.next();
            digestAlgs.add(CMSUtils.fixAlgID(signer.getDigestAlgorithmID()));
            signerInfos.add(signer.toSignerInfo());
        }
        boolean isCounterSignature = (eContentType == null);
        ASN1ObjectIdentifier contentTypeOID = isCounterSignature ?
            CMSObjectIdentifiers.data : new ASN1ObjectIdentifier(eContentType);
        it = signerInfs.iterator();
        while (it.hasNext()) {
            
            SignerInf signer = (SignerInf)it.next();
            logger.info("signerInfs -it: " + signer.signerIdentifier.toASN1Object().toString());
            
            try {
                digestAlgs.add(signer.getDigestAlgorithmID());
                signerInfos.add(signer.toSignerInfo(contentTypeOID, content, rand, null, addDefaultAttributes, isCounterSignature));
            } catch (IOException e) {
                throw new CMSException("encoding error.", e);
            } catch (InvalidKeyException e) {
                throw new CMSException("key inappropriate for signature.", e);
            } catch (SignatureException e) {
                throw new CMSException("error creating signature.", e);
            } catch (CertificateEncodingException e) {
                throw new CMSException("error creating sid.", e);
            } catch (PKCS11Exception e) {
                throw new CMSException("No ha aceptado realizar la firma", e);
            }
        }
        ASN1Set certificates = null;
        if (!certs.isEmpty()) certificates = CMSUtils.createBerSetFromList(certs);
        ASN1Set certrevlist = null;
        if (!crls.isEmpty()) certrevlist = CMSUtils.createBerSetFromList(crls);
        ASN1OctetString octs = null;
        if (encapsulate) {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            if (content != null) {
                try {
                    content.write(bOut);
                }
                catch (IOException e) {
                    throw new CMSException("encapsulation error.", e);
                }
            }
            octs = new BERConstructedOctetString(bOut.toByteArray());
        }
        ContentInfo encInfo = new ContentInfo(contentTypeOID, octs);
        SignedData  sd = new SignedData(new DERSet(digestAlgs), encInfo,
            certificates, certrevlist, new DERSet(signerInfos));
        ContentInfo contentInfo = new ContentInfo(
            CMSObjectIdentifiers.signedData, sd);
        return new CMSSignedData(content, contentInfo);
    }

    
    public Session getSession(char[] password, Mechanism signatureMechanism) 
            throws Exception {
        logger.debug("getSession");
        pkcs11Module  = null;
        pkcs11Session = null;
        token = null;
        try {
            if (pkcs11Module == null) {
                pkcs11Module  = Module.getInstance(OSValidator.getPKCS11ModulePath());
                pkcs11Module.initialize(null);
            }
            final LectorSlots lectorSlots = new LectorSlots(
                        pkcs11Module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT));
            lectorSlots.setSlots(pkcs11Module.getSlotList(
                        Module.SlotRequirement.TOKEN_PRESENT));
            token = lectorSlots.obtenerSlotSeleccionado().getToken();
            pkcs11Session = token.openSession(Token.SessionType.SERIAL_SESSION,
                Token.SessionReadWriteBehavior.RO_SESSION, null, null);
            pkcs11Session.login(Session.UserType.USER, password);
            RSAPrivateKey templateSignatureKey = new RSAPrivateKey();
            templateSignatureKey.getSign().setBooleanValue(Boolean.TRUE);
            templateSignatureKey.getLabel().setCharArrayValue(
                LABEL_CLAVE_PRIVADA_FIRMA.toCharArray());
            pkcs11Session.findObjectsInit(templateSignatureKey);
            Object[] foundSignatureKeyObjects = pkcs11Session.findObjects(1); // find first
            if (foundSignatureKeyObjects.length > 0) {
                signatureKey = (RSAPrivateKey) foundSignatureKeyObjects[0];
                pkcs11Session.signInit(signatureMechanism, signatureKey);
            } else {
                throw new Exception (getString("keyNotFoundMsg"));
            }
            pkcs11Session.findObjectsFinal();
            X509PublicKeyCertificate certificateTemplate = new X509PublicKeyCertificate();
            pkcs11Session.findObjectsInit(certificateTemplate);
            Object[] tokenCertificateObjects;
            FileInputStream fis =  new FileInputStream(FileUtils.APPDIR + Contexto.CERT_RAIZ_PATH);
            certificadoCA = CertUtil.loadCertificateFromStream(fis);
            chain[2] = certificadoCA;
            while ((tokenCertificateObjects = pkcs11Session.findObjects(1)).length > 0) {
                iaik.pkcs.pkcs11.objects.Object object = (Object) tokenCertificateObjects[0];
                Hashtable attributes = object.getAttributeTable();
                ByteArrayAttribute valueAttribute = (ByteArrayAttribute) attributes.get(Attribute.VALUE);
                byte[] value = valueAttribute.getByteArrayValue();
                X509PublicKeyCertificate cert =
                    (X509PublicKeyCertificate)tokenCertificateObjects[0];
                if (CERT_SIGN.equals(cert.getLabel().toString())) {
                    certificadoUsuario = (X509Certificate)CMSUtils.getCertificate(value);
                    Contexto.setUsuario(Usuario.getUsuario(certificadoUsuario));
                    chain[0] = certificadoUsuario;
                } else if (CERT_CA.equals(cert.getLabel().toString())) {
                    certificadoIntermedio = (X509Certificate)CMSUtils.getCertificate(value);
                    chain[1] = certificadoIntermedio;
                }
            }
            pkcs11Session.findObjectsFinal();
            CertStore certsAndCRLs = CertStore.getInstance(CERT_STORE_TYPE,
                            new CollectionCertStoreParameters(Arrays.asList(chain)), PROVIDER);
            addCertificatesAndCRLs(certsAndCRLs);
            Collection<X500Principal> collection = new ArrayList<X500Principal>();
            collection.add(certificadoUsuario.getIssuerX500Principal());
            X509CRLSelector selector = new X509CRLSelector();
            selector.setIssuers(collection);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            String mensajeError = ex.getMessage();
            if (ex instanceof ArrayIndexOutOfBoundsException) {
                mensajeError = getString("smartCardReaderErrorMsg");
            }
            if ("CKR_DEVICE_ERROR".equals(ex.getMessage()) || 
                    "CKR_CRYPTOKI_ALREADY_INITIALIZED".equals(ex.getMessage()) ||
                    "CKR_USER_ALREADY_LOGGED_IN".equals(ex.getMessage())) {
                closeSession();
                return getSession(password, signatureMechanism);
            }
            if ("CKR_PIN_INCORRECT".equals(ex.getMessage())) {
                Contexto.setDNIePassword(null);
            }
            if ("CKR_HOST_MEMORY".equals(ex.getMessage())) mensajeError = getString("smartCardReaderErrorMsg");
            throw new Exception(mensajeError);
        }
        //closeSession();
        return pkcs11Session;
    }
    

    /*public String obtenerCadenaFirmada (String textoAFirmar) throws Exception {
        MimeMessage msg = obtenerMimeMessage(textoAFirmar);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out);
        out.close();
        return new String(out.toByteArray());
    }*/

    public CMSSignedData obtenerCMSSignedData(byte[] contentBytes, 
            CMSAttributeTableGenerator unsAttr) throws Exception {
        signerInfs = new ArrayList();
        SignerInf signerInf = new SignerInf(
                CMSUtils.getSignerIdentifier(certificadoUsuario), pkcs11Session, unsAttr);
        //logger.info("***signerInf: " + signerInf.signerIdentifier.toASN1Object().toString());
        signerInfs.add(signerInf);
        //logger.info(" -- certificadoUsuario: " + certificadoUsuario.getSubjectDN().getName());
        CMSProcessable content = new CMSProcessableByteArray(contentBytes);
        CMSSignedData signedData = generarCMSSignedData(CMSSignedGenerator.DATA, 
                content, true, CMSUtils.getProvider("BC"), true, signerInfs);
        //END SIGNED PKCS7
        return signedData;
    }

    public static String getNifUsuario (X509Certificate certificate) {
    	String subjectDN = certificate.getSubjectDN().getName();
    	return subjectDN.split("SERIALNUMBER=")[1].split(",")[0];
    }

    public static void closeSession () {
        logger.debug("closeSession");
        try {
            if (token != null && token.getTokenInfo() != null 
                    && token.getTokenInfo().isTokenInitialized()) {
                token.closeAllSessions();
            }
            if (pkcs11Session != null ) {
                pkcs11Session.closeSession();
                pkcs11Session = null;
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
    
    private class LectorSlots {
        
        Slot[] slots;

        public LectorSlots (Slot[] slots) {
            this.slots = slots;
        }

        public boolean estaVacio () {
            return slots.length == 0;
        }

        public void setSlots (Slot[] slots) {
            this.slots = slots;
        }

        public Slot obtenerSlotSeleccionado () {
            return slots[0];
        }

    }
}
