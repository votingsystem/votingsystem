package org.votingsystem.crypto.cms;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.throwable.ValidationException;

import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class CMSUtils {

    private static Logger log = Logger.getLogger(CMSUtils.class.getName());


    public static final String  DIGEST_SHA1 = OIWObjectIdentifiers.idSHA1.getId();
    public static final String  DIGEST_SHA224 = NISTObjectIdentifiers.id_sha224.getId();
    public static final String  DIGEST_SHA256 = NISTObjectIdentifiers.id_sha256.getId();
    public static final String  DIGEST_SHA384 = NISTObjectIdentifiers.id_sha384.getId();
    public static final String  DIGEST_SHA512 = NISTObjectIdentifiers.id_sha512.getId();
    public static final String  DIGEST_MD5 = PKCSObjectIdentifiers.md5.getId();
    public static final String  DIGEST_GOST3411 = CryptoProObjectIdentifiers.gostR3411.getId();
    public static final String  DIGEST_RIPEMD128 = TeleTrusTObjectIdentifiers.ripemd128.getId();
    public static final String  DIGEST_RIPEMD160 = TeleTrusTObjectIdentifiers.ripemd160.getId();
    public static final String  DIGEST_RIPEMD256 = TeleTrusTObjectIdentifiers.ripemd256.getId();

    public static final String  ENCRYPTION_RSA = PKCSObjectIdentifiers.rsaEncryption.getId();
    public static final String  ENCRYPTION_DSA = X9ObjectIdentifiers.id_dsa_with_sha1.getId();
    public static final String  ENCRYPTION_ECDSA = X9ObjectIdentifiers.ecdsa_with_SHA1.getId();
    public static final String  ENCRYPTION_RSA_PSS = PKCSObjectIdentifiers.id_RSASSA_PSS.getId();
    public static final String  ENCRYPTION_GOST3410 = CryptoProObjectIdentifiers.gostR3410_94.getId();
    public static final String  ENCRYPTION_ECGOST3410 = CryptoProObjectIdentifiers.gostR3410_2001.getId();


    public static String getDigestId (String digestAlgOID) {
        if (DIGEST_SHA1.equals(digestAlgOID)) return "SHA1";
        if (DIGEST_SHA224.equals(digestAlgOID)) return "SHA224";
        if (DIGEST_SHA256.equals(digestAlgOID)) return "SHA256";
        if (DIGEST_SHA384.equals(digestAlgOID)) return "SHA384";
        if (DIGEST_SHA512.equals(digestAlgOID)) return "SHA512";
        if (DIGEST_MD5.equals(digestAlgOID)) return "MD5";
        if (DIGEST_GOST3411.equals(digestAlgOID)) return "GOST3411";
        if (DIGEST_RIPEMD128.equals(digestAlgOID)) return "RIPEMD128";
        if (DIGEST_RIPEMD160.equals(digestAlgOID)) return "RIPEMD160";
        if (DIGEST_RIPEMD256.equals(digestAlgOID)) return "RIPEMD256";
        return null;
    }

    public static String getEncryptiontId (String encryptionAlgOID) {
        if (ENCRYPTION_RSA.equals(encryptionAlgOID)) return "RSA";
        if (ENCRYPTION_DSA.equals(encryptionAlgOID)) return "DSA";
        if (ENCRYPTION_ECDSA.equals(encryptionAlgOID)) return "ECDSA";
        if (ENCRYPTION_RSA_PSS.equals(encryptionAlgOID)) return "RSA_PSS";
        if (ENCRYPTION_GOST3410.equals(encryptionAlgOID)) return "GOST3410";
        if (ENCRYPTION_ECGOST3410.equals(encryptionAlgOID)) return "ECGOST3410";
        return null;
    }

    public static CMSSignedData addTimeStampToUnsignedAttributes(CMSSignedData cmsdata,
                                         TimeStampToken timeStampToken) throws Exception {
        DERSet derset = new DERSet(timeStampToken.toCMSSignedData().toASN1Structure());
        Attribute timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, derset);
        Hashtable hashTable = new Hashtable();
        hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
        AttributeTable timeStampAsAttributeTable = new AttributeTable(hashTable);
        byte[] timeStampTokenHash = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
        Iterator<SignerInformation> it = cmsdata.getSignerInfos().getSigners().iterator();
        List<SignerInformation> newSigners = new ArrayList<>();
        while (it.hasNext()) {
            SignerInformation signer = it.next();
            byte[] digestBytes = CMSUtils.getSignerDigest(signer);
            if(Arrays.equals(timeStampTokenHash, digestBytes)) {
                log.info("setTimeStampToken - found signer");
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable != null) {
                    log.info("setTimeStampToken - signer with UnsignedAttributes");
                    hashTable = attributeTable.toHashtable();
                    if(!hashTable.contains(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken)) {
                        hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
                    }
                    timeStampAsAttributeTable = new AttributeTable(hashTable);
                }
                updatedSigner = signer.replaceUnsignedAttributes(signer, timeStampAsAttributeTable);
                newSigners.add(updatedSigner);
            } else newSigners.add(signer);
        }
        SignerInformationStore newSignersStore = new SignerInformationStore(newSigners);
        return  CMSSignedData.replaceSigners(cmsdata, newSignersStore);
    }

    public static TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        TimeStampToken timeStampToken = null;
        AttributeTable signedAttributes = signer.getSignedAttributes();
        Attribute timeStampAttribute = signedAttributes.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
        if(timeStampAttribute != null) {
            CMSSignedData signedData = new CMSSignedData(timeStampAttribute.getAttrValues()
                    .getObjectAt(0).toASN1Primitive().getEncoded());
            timeStampToken = new TimeStampToken(signedData);
        }
        return timeStampToken;
    }

    public static TimeStampToken getTimeStampToken(String signatureAlgorithm, byte[] contentToSign,
            String timestampServiceURL) throws Exception {
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        MessageDigest digest = MessageDigest.getInstance(digAlgId.getAlgorithm().getId());
        byte[]  digestBytes = digest.digest(contentToSign);
        TimeStampRequest timeStampRequest = reqgen.generate(digAlgId.getAlgorithm(), digestBytes);
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(timeStampRequest.getEncoded(),
                MediaType.TIMESTAMP_QUERY, timestampServiceURL);
        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
            byte[] bytesToken = responseDto.getMessageBytes();
            TimeStampResponse timeStampResponse = new TimeStampResponse(bytesToken);
            return timeStampResponse.getTimeStampToken();
        } else throw new ValidationException(responseDto.getMessage());
    }

    public static ASN1Encodable getSingleValuedAttribute(AttributeTable signedAttrTable,
                                             ASN1ObjectIdentifier attrOID, String printableName) throws CMSException {
        if (signedAttrTable == null) return null;
        ASN1EncodableVector vector = signedAttrTable.getAll(attrOID);
        switch (vector.size()) {
            case 0:
                return null;
            case 1:
                Attribute t = (Attribute)vector.get(0);
                ASN1Set attrValues = t.getAttrValues();
                if (attrValues.size() != 1)
                    throw new CMSException("A " + printableName + " attribute MUST have a single attribute value");
                return attrValues.getObjectAt(0);
            default:
                throw new CMSException("The SignedAttributes in a signerInfo MUST NOT include multiple instances of the "
                        + printableName + " attribute");
        }
    }

    public static byte[] getSignerDigest(SignerInformation signer) throws CMSException {
        return ((ASN1OctetString) getSingleValuedAttribute(signer.getSignedAttributes(),
                CMSAttributes.messageDigest, "message-digest")).getOctets();
    }

}