package org.votingsystem.signature.util;

import android.util.Log;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.json.JSONObject;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SignedMailGenerator;
import org.votingsystem.util.DeviceUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Header;
import javax.security.auth.x500.X500Principal;

import static org.votingsystem.model.ContextVS.ANDROID_PROVIDER;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class CertificationRequestVS implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    
	public static final String TAG = "CertificationRequestVS";
    private transient PKCS10CertificationRequest csr;
    private transient SignedMailGenerator signedMailGenerator;
    private String hashPin;
    private KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] signedCsr;

    private CertificationRequestVS(KeyPair keyPair, PKCS10CertificationRequest csr,
            String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequestVS getVoteRequest(int keySize, String keyName,
            String signatureMechanism, String provider, String accessControlURL, String eventId,
            String getHashCertVSBase64) throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=accessControlURL:" + accessControlURL +", OU=eventId:" + eventId);
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        Map delegationDataMap = new HashMap<String, String>();
        delegationDataMap.put("accessControlURL", accessControlURL);
        delegationDataMap.put("hashCertVS", getHashCertVSBase64);
        delegationDataMap.put("eventId", eventId);
        JSONObject jsonObject = new JSONObject(delegationDataMap);
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
        JSONObject jsonObject = new JSONObject(delegationDataMap);
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
        tagVS = (tagVS == null)? TagVS.WILDTAG:tagVS;
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
        JSONObject jsonObject = new JSONObject(delegationDataMap);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.VICKET_TAG,
                new DERUTF8String(jsonObject.toString())));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public static CertificationRequestVS getUserRequest (int keySize, String keyName,
               String signatureMechanism, String provider, String nif, String email,
               String phone, String deviceId, String givenName, String surName) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {

        KeyPair keyPair = VotingSystemKeyGenerator.INSTANCE.genKeyPair();
        String principal = "SERIALNUMBER=" + nif + ", GIVENNAME=" + givenName + ", SURNAME=" + surName;

        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        Map extensionDataMap = new HashMap<String, String>();
        extensionDataMap.put("deviceId", deviceId);
        extensionDataMap.put("deviceName", DeviceUtils.getDeviceName());
        if (email != null) extensionDataMap.put("email", email);
        if (phone != null) extensionDataMap.put("mobilePhone", phone);
        JSONObject jsonObject = new JSONObject(extensionDataMap);
        asn1EncodableVector.add(new DERTaggedObject(ContextVS.DEVICEVS_TAG,
                new DERUTF8String(jsonObject.toString())));
        X500Principal subject = new X500Principal(principal);

        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequestVS(keyPair, csr, signatureMechanism);
    }

    public void initSigner (byte[] signedCsr) {
        this.signedCsr = signedCsr;
    }

    public SMIMEMessage genMimeMessage(String fromUser, String toUser,
          String textToSign, String subject, Header header) throws Exception {
        return getSignedMailGenerator().genMimeMessage(fromUser, toUser, textToSign, subject);
    }

    private SignedMailGenerator getSignedMailGenerator() throws Exception {
        if (signedMailGenerator == null) {
            Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(signedCsr);
            Log.d(TAG + "getSignedMailGenerator()", "Num certs: " + certificates.size());
            if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
            certificate = certificates.iterator().next();
            X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
            certificates.toArray(arrayCerts);
            signedMailGenerator = new SignedMailGenerator(keyPair.getPrivate(), arrayCerts,
                    signatureMechanism, ANDROID_PROVIDER);
            signedMailGenerator = new SignedMailGenerator(keyPair.getPrivate(), arrayCerts,
                    signatureMechanism, ANDROID_PROVIDER);
        }
        return signedMailGenerator;
    }

    public X509Certificate getCertificate() {
        if(certificate == null && signedCsr != null) {
            try {
                Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(signedCsr);
                Log.d(TAG + "getSignedMailGenerator()", "Num certs: " + certificates.size());
                if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
                certificate = certificates.iterator().next();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
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
        if(getCsr() == null && signedCsr != null) {
            csr = getCsr();
        }
        return getCsr().getEncoded();
    }

    public byte[] getCsrPEM() throws Exception {
        return CertUtil.getPEMEncoded(getCsr());
    }

    public byte[] getSignedCsr() {
        return signedCsr;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(certificate != null) s.writeObject(certificate.getEncoded());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        byte[] certificateBytes = (byte[]) s.readObject();
        if(certificateBytes != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(certificateBytes);
                certificate = CertUtil.loadCertificateFromStream (bais);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private PKCS10CertificationRequest getCsr() {
        if(csr == null && signedCsr != null) {
            csr = new PKCS10CertificationRequest(signedCsr);
        }
        return csr;
    }

    public String getHashPin() {
        return hashPin;
    }

    public void setHashPin(String hashPin) {
        this.hashPin = hashPin;
    }
}