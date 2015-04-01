package org.votingsystem.signature.dnie;

import iaik.pkcs.pkcs11.*;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.*;
import iaik.pkcs.pkcs11.objects.Object;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.*;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.ContentSignerVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.OSValidator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.util.ContextVS.*;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class DNIePDFContentSigner extends CMSSignedGenerator implements ContentSignerVS {

    private static Logger log = Logger.getLogger(DNIePDFContentSigner.class.getSimpleName());

    private static final int  BUFFER_SIZE = 4096;

    private static Mechanism instanceSignatureMechanism;

    private Session pkcs11Session;
    private Module pkcs11Module;
    private Token token;
    private RSAPrivateKey signatureKey;
    private X509Certificate certUser = null;
    private X509Certificate certIntermediate = null;
    private X509Certificate certCA = null;

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
            byte[] dataBuffer = new byte[BUFFER_SIZE];
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

    public DNIePDFContentSigner(Session pkcs11Session, Module pkcs11Module, Token token, RSAPrivateKey signatureKey,
            X509Certificate certUser, X509Certificate certIntermediate, X509Certificate certCA) throws Exception {
        this.pkcs11Session = pkcs11Session;
        this.pkcs11Module = pkcs11Module;
        this.token = token;
        this.signatureKey = signatureKey;
        this.certCA = certCA;
        this.certUser = certUser;
        this.certIntermediate = certIntermediate;
        CertStore certsAndCRLs = CertStore.getInstance(CERT_STORE_TYPE,
                new CollectionCertStoreParameters(Arrays.asList(getCertificateChain())), PROVIDER);;
        addCertificatesAndCRLs(certsAndCRLs);
    }

    public Certificate[] getCertificateChain() {
        List<Certificate> certList = new ArrayList();
        //The order is important
        if(certUser != null) certList.add(certUser);
        if(certIntermediate != null) certList.add(certIntermediate);
        if(certCA != null) certList.add(certCA);
        return certList.toArray(new Certificate[certList.size()]);
    }


    public CMSSignedData getCMSSignedData(String eContentType, CMSProcessable content, boolean encapsulate,
            Provider sigProvider, boolean addDefaultAttributes, List signerInfs) throws  Exception {
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
            log.info("signer.signerIdentifier: " + signer.signerIdentifier.toASN1Object().toString());
            digestAlgs.add(signer.getDigestAlgorithmID());
            signerInfos.add(signer.toSignerInfo(contentTypeOID, content, rand, null, addDefaultAttributes, isCounterSignature));
        }
        ASN1Set certificates = null;
        if (!certs.isEmpty()) certificates = CMSUtils.createBerSetFromList(certs);
        ASN1Set certrevlist = null;
        if (!crls.isEmpty()) certrevlist = CMSUtils.createBerSetFromList(crls);
        ASN1OctetString octs = null;
        if (encapsulate && content != null) {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            content.write(bOut);
            octs = new BERConstructedOctetString(bOut.toByteArray());
        }
        ContentInfo encInfo = new ContentInfo(contentTypeOID, octs);
        SignedData  sd = new SignedData(new DERSet(digestAlgs), encInfo, certificates,
                certrevlist, new DERSet(signerInfos));
        ContentInfo contentInfo = new ContentInfo(CMSObjectIdentifiers.signedData, sd);
        return new CMSSignedData(content, contentInfo);
    }


    public static DNIePDFContentSigner getInstance(char[] password, Mechanism signatureMechanism) throws Exception {
        log.info("getInstance");
        DNIePDFContentSigner instance = null;
        instanceSignatureMechanism = signatureMechanism;
        Session pkcs11Session;
        Module pkcs11Module;
        Token token;
        RSAPrivateKey signatureKey;
        X509Certificate certUser = null;
        X509Certificate certIntermediate = null;
        X509Certificate certCA = null;
        try {
            pkcs11Module  = Module.getInstance(OSValidator.getPKCS11ModulePath());
            pkcs11Module.initialize(null);
            final SlotReader lectorSlots = new SlotReader(pkcs11Module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT));
            token = lectorSlots.getSelected().getToken();
            pkcs11Session = token.openSession(Token.SessionType.SERIAL_SESSION,
                    Token.SessionReadWriteBehavior.RO_SESSION, null, null);
            pkcs11Session.login(Session.UserType.USER, password);
            RSAPrivateKey templateSignatureKey = new RSAPrivateKey();
            templateSignatureKey.getSign().setBooleanValue(Boolean.TRUE);
            templateSignatureKey.getLabel().setCharArrayValue(DNIe_SIGN_PRIVATE_KEY_LABEL.toCharArray());
            pkcs11Session.findObjectsInit(templateSignatureKey);
            Object[] foundSignatureKeyObjects = pkcs11Session.findObjects(1); // find first
            if (foundSignatureKeyObjects.length > 0) {
                signatureKey = (RSAPrivateKey) foundSignatureKeyObjects[0];
                pkcs11Session.signInit(signatureMechanism, signatureKey);
            } else {
                throw new Exception (ContextVS.getInstance().getMessage("keyNotFoundMsg"));
            }
            pkcs11Session.findObjectsFinal();
            X509PublicKeyCertificate certificateTemplate = new X509PublicKeyCertificate();
            pkcs11Session.findObjectsInit(certificateTemplate);
            Object[] tokenCertificateObjects;
            certCA = CertUtils.loadCertificate(FileUtils.getBytesFromFile(
                    new File(ContextVS.APPDIR + ContextVS.CERT_RAIZ_PATH)));
            while ((tokenCertificateObjects = pkcs11Session.findObjects(1)).length > 0) {
                iaik.pkcs.pkcs11.objects.Object object = (Object) tokenCertificateObjects[0];
                Hashtable attributes = object.getAttributeTable();
                ByteArrayAttribute valueAttribute = (ByteArrayAttribute) attributes.get(Attribute.VALUE);
                byte[] value = valueAttribute.getByteArrayValue();
                X509PublicKeyCertificate cert =
                        (X509PublicKeyCertificate)tokenCertificateObjects[0];
                if (CERT_SIGN.equals(cert.getLabel().toString())) {
                    certUser = (X509Certificate) CertUtils.loadCertificate(value);
                    ContextVS.getInstance().setSessionUser(UserVS.getUserVS(certUser));
                } else if (CERT_CA.equals(cert.getLabel().toString())) {
                    certIntermediate = (X509Certificate) CertUtils.loadCertificate(value);
                }
            }
            pkcs11Session.findObjectsFinal();
            //Collection<X500Principal> collection = new ArrayList<X500Principal>();
            //collection.add(certUser.getIssuerX500Principal());
            //X509CRLSelector selector = new X509CRLSelector();
            //selector.setIssuers(collection);
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
            if (ex instanceof ArrayIndexOutOfBoundsException) {
                throw new ExceptionVS(ContextVS.getInstance().getMessage("smartCardReaderErrorMsg"));
            }
            if ("CKR_PIN_INCORRECT".equals(ex.getMessage())) {
                throw new ExceptionVS(ContextVS.getInstance().getMessage("passwordErrorMsg"));
            }
            if ("CKR_HOST_MEMORY".equals(ex.getMessage())) {
                throw new ExceptionVS(ContextVS.getInstance().getMessage("smartCardReaderErrorMsg"));
            }
            throw ex;
        }
        instance = new DNIePDFContentSigner(pkcs11Session, pkcs11Module, token, signatureKey,
                certUser, certIntermediate, certCA);
        return instance;
    }


    public CMSSignedData genSignedData(byte[] contentBytes, CMSAttributeTableGenerator unsAttr) throws Exception {
        List signerInfs = new ArrayList();
        SignerInf signerInf = new SignerInf(CMSUtils.getSignerIdentifier(certUser), pkcs11Session, unsAttr);
        signerInfs.add(signerInf);
        CMSProcessable content = new CMSProcessableByteArray(contentBytes);
        CMSSignedData signedData = getCMSSignedData(CMSSignedGenerator.DATA, content, true,
                CMSUtils.getProvider("BC"), true, signerInfs);
        return signedData;
    }

    public void closeSession () {
        log.info("closeSession");
        try {
            if (token != null && token.getTokenInfo() != null && token.getTokenInfo().isTokenInitialized()) {
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
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static class SlotReader {

        Slot[] slots;

        public SlotReader (Slot[] slots) {
            this.slots = slots;
        }

        public Slot getSelected () {
            return slots[0];
        }
    }

}
