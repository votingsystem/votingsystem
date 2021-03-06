package org.votingsystem.crypto.cms;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;
import org.bouncycastle.tsp.cms.ImprintDigestInvalidException;
import org.bouncycastle.util.Store;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.Signature;
import org.votingsystem.model.User;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CMSSignedMessage extends CMSSignedData {

    private static Logger log = Logger.getLogger(CMSSignedMessage.class.getName());

    private MessageData messageData;

    public CMSSignedMessage(CMSSignedData signedData) throws IOException, CMSException {
        super(signedData.getEncoded());
    }

    public CMSSignedMessage(byte[] messageBytes) throws Exception {
        super(messageBytes);
    }

    public String getSignedContentStr() throws Exception {
        return new String((byte[]) getSignedContent().getContent());
    }

    public <T> T getSignedContent(Class<T> type) throws Exception {
        return new JSON().getMapper().readValue((byte[]) getSignedContent().getContent(), type);
    }

    public <T> T getSignedContent(TypeReference type) throws Exception {
        return new JSON().getMapper().readValue((byte[]) getSignedContent().getContent(), type);
    }

    public static TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        TimeStampToken timeStampToken = null;
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();
        if (unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if (timeStampAttribute != null) {
                CMSSignedData signedData = new CMSSignedData(timeStampAttribute.getAttrValues()
                        .getObjectAt(0).toASN1Primitive().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                return timeStampToken;
            }
        } else log.info("without unsignedAttributes");
        return timeStampToken;
    }

    public TimeStampRequest getTimeStampRequest() throws Exception {
        SignerInformation signerInformation = getSignerInfos().getSigners().iterator().next();
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString) hash.getAttrValues().getObjectAt(0));
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(new ASN1ObjectIdentifier(signerInformation.getDigestAlgOID()), as.getOctets());
    }

    public TimeStampToken getTimeStampToken() throws Exception {
        return getMessageData().getTimeStampToken();
    }

    public TimeStampToken getTimeStampToken(X509Certificate requestCert) throws Exception {
        Store certs = getCertificates();
        SignerInformationStore signerInfos = getSignerInfos();
        Iterator it = signerInfos.getSigners().iterator();
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            Collection certCollection = certs.getMatches(signer.getSID());
            Iterator certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(Constants.PROVIDER).getCertificate(
                    (X509CertificateHolder) certIt.next());
            if (requestCert.getSerialNumber().equals(cert.getSerialNumber())) {
                return checkTimeStampToken(signer);
            }
        }
        return null;
    }

    public byte[] toPEM() throws IOException {
        return PEMUtils.getPEMEncoded(toASN1Structure());
    }

    public CMSSignedMessage checkSignatureInfo() throws Exception {
        getMessageData();
        return this;
    }

    /**
     * Digest for storing unique CMSMessage in database
     */
    public String getContentDigestStr() throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        return Base64.getEncoder().encodeToString(messageDigest.digest((byte[]) getSignedContent().getContent()));
    }

    public static CMSSignedMessage FROM_PEM(byte[] pemBytes) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pemBytes)));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    public static CMSSignedMessage FROM_PEM(InputStream inputStream) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(inputStream));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    private MessageData getMessageData() throws Exception {
        if (messageData == null) messageData = new MessageData();
        return messageData;
    }

    public Signature getFirstSignature() throws Exception {
        return getMessageData().getFirstSignature();
    }

    public Set<Signature> getSignatures() throws Exception {
        return getMessageData().getSignatures();
    }

    public Set<X509Certificate> getSignersCerts() throws Exception {
        Set<X509Certificate> signerCerts = new HashSet<>();
        for (Signature signature : getMessageData().getSignatures()) {
            signerCerts.add(signature.getSigner().getX509Certificate());
        }
        return signerCerts;
    }

    public X509Certificate getCurrencyCert() throws Exception {
        return getMessageData().getCurrencyCert();
    }

    private class MessageData {

        private Signature firstSignature;
        private Set<Signature> signatures;
        private TimeStampToken timeStampToken;
        private X509Certificate currencyCert;

        public MessageData() throws Exception {
            /**
             * verify that the signature is correct and that it was generated when the
             * certificate was current(assuming the cert is contained in the message).
             */
            Store certs = getCertificates();
            SignerInformationStore signerInfos = getSignerInfos();
            Set<X509Certificate> signerCerts = new HashSet<>();
            Iterator it = signerInfos.getSigners().iterator();
            Date firstSignature = null;
            signatures = new HashSet<>();
            int signerNumber = 1;
            while (it.hasNext()) {
                SignerInformation signer = (SignerInformation) it.next();
                byte[] signerDigest = org.votingsystem.crypto.cms.CMSUtils.getSignerDigest(signer);
                Collection certCollection = certs.getMatches(signer.getSID());
                Iterator certIt = certCollection.iterator();
                X509Certificate cert = new JcaX509CertificateConverter().setProvider(Constants.PROVIDER).getCertificate(
                        (X509CertificateHolder) certIt.next());
                log.info(signerNumber + "/" + signerInfos.getSigners().size() + " - cert: " + cert.getSubjectDN() +
                        " - " + certCollection.size() + " match");
                try {
                    signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            Constants.PROVIDER).build(cert));
                } catch (CMSVerifierCertificateNotValidException ex) {
                    log.log(Level.SEVERE, "cert notBefore: " + cert.getNotBefore() + " - NotAfter: " + cert.getNotAfter());
                    throw ex;
                } catch (Exception ex) {
                    throw ex;
                }
                User user = User.FROM_CERT(cert, User.Type.USER);
                user.setSignerInformation(signer);
                TimeStampToken tsToken = org.votingsystem.crypto.cms.CMSUtils.checkTimeStampToken(signer);
                user.setTimeStampToken(tsToken);
                if (tsToken != null) {
                    TimeStampTokenInfo info = tsToken.getTimeStampInfo();
                    byte[] tsrMessageDigest = info.getMessageImprintDigest();
                    if (!org.bouncycastle.util.Arrays.areEqual(signerDigest, tsrMessageDigest)) {
                        throw new ImprintDigestInvalidException(
                                "hash calculated is different from MessageImprintDigest found in TimeStampToken", tsToken);
                    }
                    Date timeStampDate = tsToken.getTimeStampInfo().getGenTime();
                    Signature signature = new Signature(user, Base64.getEncoder().encodeToString(signerDigest),
                            DateUtils.getLocalDateFromUTCDate(timeStampDate)).setSigningCert(cert);
                    signatures.add(signature);
                    if (firstSignature == null || firstSignature.after(timeStampDate)) {
                        firstSignature = timeStampDate;
                        this.firstSignature = signature;
                        timeStampToken = tsToken;
                    }
                } else signatures.add(new Signature(user, Base64.getEncoder().encodeToString(signerDigest), null).setSigningCert(cert));
                if (cert.getExtensionValue(Constants.CURRENCY_OID) != null) {
                    currencyCert = cert;
                }
                signerCerts.add(cert);
            }
        }

        public Signature getFirstSignature() throws ValidationException {
            if(timeStampToken == null)
                throw new ValidationException("CMSMessageReader without timestamp");
            return firstSignature;
        }

        public Set<Signature> getSignatures() {
            return signatures;
        }

        public TimeStampToken getTimeStampToken() {
            return timeStampToken;
        }

        public X509Certificate getCurrencyCert() {
            return currencyCert;
        }
    }

}