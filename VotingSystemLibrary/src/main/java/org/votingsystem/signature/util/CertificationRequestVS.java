package org.votingsystem.signature.util;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.VicketTagVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;

import javax.mail.Header;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificationRequestVS {
    
    private static Logger logger = Logger.getLogger(CertificationRequestVS.class);

    private PKCS10CertificationRequest csr;
    private KeyPair keyPair;
    private String signatureMechanism;

    private X509Certificate certificate;
    private SignedMailGenerator signedMailGenerator;

    private CertificationRequestVS(KeyPair keyPair, PKCS10CertificationRequest csr, String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequestVS getVoteRequest(int keySize, String keyName, String signatureMechanism,
            String provider, String accessControlURL, String eventId, String getHashCertVSBase64)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException,
            IOException {
        KeyPair keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +", OU=eventId:" + eventId);
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        Map delegationDataMap = new HashMap<String, String>();
        delegationDataMap.put("accessControlURL", accessControlURL);
        delegationDataMap.put("hashCertVS", getHashCertVSBase64);
        delegationDataMap.put("eventId", eventId);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(delegationDataMap);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.VOTE_TAG, new DERUTF8String(jsonObject.toString())));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getAnonymousDelegationRequest(int keySize, String keyName,
            String signatureMechanism, String provider, String accessControlURL, String hashCertVS,
            String weeksOperationActive) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +
                ", OU=AnonymousRepresentativeDelegation");
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        Map delegationDataMap = new HashMap<String, String>();
        delegationDataMap.put("accessControlURL", accessControlURL);
        delegationDataMap.put("hashCertVS", hashCertVS);
        delegationDataMap.put("weeksOperationActive", weeksOperationActive);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(delegationDataMap);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_TAG,
                new DERUTF8String(jsonObject.toString())));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getVicketRequest(int keySize, String keyName,
              String signatureMechanism, String provider, String vicketServerURL, String hashCertVS,
              String amount, String currency, String tagVS) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        tagVS = (tagVS == null)? VicketTagVS.WILDTAG:tagVS;
        X500Principal subject = new X500Principal("CN=vicketServerURL:" + vicketServerURL +
                ", OU=VICKET_VALUE:" + amount + ", OU=CURRENCY_CODE:" + currency +
                ", OU=TAGVS:" + tagVS + ", OU=DigitalCurrency");
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        Map delegationDataMap = new HashMap<String, String>();
        delegationDataMap.put("vicketServerURL", vicketServerURL);
        delegationDataMap.put("hashCertVS", hashCertVS);
        delegationDataMap.put("vicketValue", amount);
        delegationDataMap.put("currencyCode", currency);
        delegationDataMap.put("tagVS", tagVS);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(delegationDataMap);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.VICKET_TAG,
                new DERUTF8String(jsonObject.toString())));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public void initSigner (byte[] signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(signedCsr);
        logger.debug("initSigner - Num certs: " + certificates.size());
        if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
        certificate = certificates.iterator().next();
        X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
        certificates.toArray(arrayCerts);
        signedMailGenerator = new SignedMailGenerator(keyPair.getPrivate(), arrayCerts, signatureMechanism);
    }
    
    public SMIMEMessage genMimeMessage(String fromUser, String toUser,
            String textToSign, String subject, Header header) throws Exception {
        if (signedMailGenerator == null) throw new Exception (" --- SignedMailGenerator null --- ");
        return signedMailGenerator.genMimeMessage(fromUser, toUser, textToSign, subject);
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() throws Exception {
        return keyPair.getPrivate();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public byte[] getCsrDER() {
        return csr.getEncoded();
    }

    public byte[] getCsrPEM() throws Exception {
        return CertUtil.getPEMEncoded(csr);
    }

}