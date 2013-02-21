package org.sistemavotacion.seguridad;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.ASN1ObjectIdentifier;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.ASN1Set;
import org.bouncycastle2.asn1.BERConstructedOctetString;
import org.bouncycastle2.asn1.DERNull;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle2.asn1.cms.ContentInfo;
import org.bouncycastle2.asn1.cms.SignedData;
import org.bouncycastle2.asn1.cms.SignerInfo;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cms.CMSAttributeTableGenerator;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSProcessable;
import org.bouncycastle2.cms.CMSProcessableByteArray;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSSignedDataGenerator;
import org.bouncycastle2.cms.CMSSignedGenerator;
import org.bouncycastle2.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.smime.CMSUtils;

import android.util.Base64;
import android.util.Log;

public class PDF_CMSSignedGenerator extends CMSSignedGenerator {
	
	public static final String TAG = "PDF_CMSSignedGenerator";

    public static final String PDF_DIGEST_OID          = CMSSignedDataGenerator.DIGEST_SHA1;;
    public static final String PDF_SIGNATURE_MECHANISM = "SHA1withRSA";
    public static final String PDF_SIGNATURE_DIGEST    = "SHA1";
    public static String CERT_STORE_TYPE = "Collection";

    public static X509Certificate certificadoUsuario = null;
    public static X509Certificate certificadoIntermedio = null;
    public static X509Certificate certificadoCA = null;
            
    private String pdfSignatureDigest = null;

    public PDF_CMSSignedGenerator (String pdfSignatureDigest) throws Exception {
    	this.pdfSignatureDigest = pdfSignatureDigest;
    }

    public CMSSignedData generarCMSSignedData(String eContentType,
            CMSProcessable content, boolean encapsulate, Provider sigProvider,
            boolean addDefaultAttributes, List<SignerInfo> signerInfoList)
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
        for(SignerInfo signerInfo : signerInfoList) {
            try {
                digestAlgs.add(signerInfo.getDigestAlgorithm());
                signerInfos.add(signerInfo);
            } catch (Exception ex) {
                ex.printStackTrace();
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

    public CMSSignedData obtenerCMSSignedData(byte[] signatureHash, 
            CMSAttributeTableGenerator unsAttr, PrivateKey privateKey, 
            X509Certificate certificadoUsuario, Certificate[] signerCertsChain) throws Exception {
    	
        CMSProcessable content = new CMSProcessableByteArray(signatureHash);
        ByteArrayOutputStream out = null;
        if (content != null) {
        	out = new ByteArrayOutputStream();
            content.write(out);
            out.close();
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
        MessageDigest softwareDigestEngine = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        int bytesRead;
        byte[] dataBuffer = new byte[4096];
        while ((bytesRead = bais.read(dataBuffer)) >= 0) {
          softwareDigestEngine.update(dataBuffer, 0, bytesRead);
        }
        byte[] hash = softwareDigestEngine.digest();
    	
        CertStore certsAndCRLs = CertStore.getInstance(CERT_STORE_TYPE,
                new CollectionCertStoreParameters(Arrays.asList(signerCertsChain)), Aplicacion.PROVIDER);
        addCertificatesAndCRLs(certsAndCRLs);
    	
    	CMSAttributeTableGenerator sAttr = new DefaultSignedAttributeTableGenerator();
    	
        ASN1ObjectIdentifier contentTypeOID = new ASN1ObjectIdentifier(CMSSignedGenerator.DATA);
        Map parameters = getBaseParameters(contentTypeOID, 
        		new AlgorithmIdentifier(new DERObjectIdentifier(PDF_DIGEST_OID), new DERNull()), hash);
        AttributeTable attributeTable = sAttr.getAttributes(Collections.unmodifiableMap(parameters));
    	
        String signatureHashStr = Base64.encodeToString(signatureHash, Base64.DEFAULT);
    	
        JcaSimpleSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder =  new JcaSimpleSignerInfoGeneratorBuilder();
        jcaSignerInfoGeneratorBuilder = jcaSignerInfoGeneratorBuilder.setProvider(Aplicacion.PROVIDER);
        jcaSignerInfoGeneratorBuilder.setSignedAttributeGenerator(attributeTable);
        jcaSignerInfoGeneratorBuilder.setUnsignedAttributeGenerator(unsAttr);
        SignerInfoGenerator signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(
        		PDF_SIGNATURE_MECHANISM, privateKey, certificadoUsuario);
    	
        SignerInfo signerInfo = signerInfoGenerator.generate(contentTypeOID);
    	
        List<SignerInfo> signerInfoList = new ArrayList<SignerInfo>();
        signerInfoList.add(signerInfo);

        Log.d(TAG, " -- certificadoUsuario: " + certificadoUsuario.getSubjectDN().getName());
        CMSSignedData signedData = generarCMSSignedData(CMSSignedGenerator.DATA, 
                content, true, CMSUtils.getProvider("BC"), true, signerInfoList);
        //END SIGNED PKCS7
        return signedData;
    }

    public static String getNifUsuario (X509Certificate certificate) {
    	String subjectDN = certificate.getSubjectDN().getName();
    	return subjectDN.split("SERIALNUMBER=")[1].split(",")[0];
    }


}
