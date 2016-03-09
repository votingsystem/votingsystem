package org.votingsystem.cms;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.crypto.CMSUtils;
import org.votingsystem.util.crypto.KeyGeneratorVS;
import org.votingsystem.util.crypto.PEMUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
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
        return new String((byte[])getSignedContent().getContent());
    }

    public <T> T getSignedContent(Class<T> type) throws Exception {
        return JSON.getMapper().readValue((byte[])getSignedContent().getContent(), type);
    }

    public <T> T getSignedContent(TypeReference type) throws Exception {
        return JSON.getMapper().readValue((byte[])getSignedContent().getContent(), type);
    }

    public static TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        TimeStampToken timeStampToken = null;
        AttributeTable unsignedAttributes = signer.getUnsignedAttributes();
        if(unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if(timeStampAttribute != null) {
                CMSSignedData signedData = new CMSSignedData(timeStampAttribute.getAttrValues()
                        .getObjectAt(0).toASN1Primitive().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                return timeStampToken;
            }
        } else log.info("checkTimeStampToken - without unsignedAttributes");
        return timeStampToken;
    }

    public TimeStampRequest getTimeStampRequest() throws Exception {
        SignerInformation signerInformation = (SignerInformation) getSignerInfos().getSigners().iterator().next();
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(signerInformation.getDigestAlgOID(), as.getOctets(),
                BigInteger.valueOf(KeyGeneratorVS.INSTANCE.getNextRandomInt()));
    }

    public TimeStampToken getTimeStampToken() throws Exception {
        return getMessageData().getTimeStampToken();
    }

    public TimeStampToken getTimeStampToken(X509Certificate requestCert) throws Exception {
        Store certs = getCertificates();
        SignerInformationStore signerInfos = getSignerInfos();
        Iterator it = signerInfos.getSigners().iterator();
        while (it.hasNext()) {
            SignerInformation   signer = (SignerInformation)it.next();
            Collection certCollection = certs.getMatches(signer.getSID());
            Iterator certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER).getCertificate(
                    (X509CertificateHolder) certIt.next());
            if(requestCert.getSerialNumber().equals(cert.getSerialNumber())) {
                return checkTimeStampToken(signer);
            }
        }
        return null;
    }

    public byte[] toPEM() throws IOException {
        return PEMUtils.getPEMEncoded(toASN1Structure());
    }

    public String toPEMStr() throws IOException {
        return new String(PEMUtils.getPEMEncoded(toASN1Structure()));
    }

    public boolean isValidSignature() throws Exception {
        getMessageData();
        return true;
    }

    public static CMSSignedMessage addTimeStamp(CMSSignedMessage signedMessage, TimeStampToken timeStampToken) throws Exception {
        CMSSignedData timeStampedSignedData = CMSUtils.addTimeStamp(signedMessage, timeStampToken);
        return new CMSSignedMessage(timeStampedSignedData.getEncoded());
    }

    public static CMSSignedMessage addTimeStamp(CMSSignedMessage signedData, String timeStampServerURL) throws Exception {
        TimeStampRequest tspRequest = signedData.getTimeStampRequest();
        ResponseVS responseVS = HttpHelper.getInstance().sendData(tspRequest.getEncoded(), ContentTypeVS.TIMESTAMP_QUERY,
                timeStampServerURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(responseVS.getMessageBytes()));
            signedData = signedData.addTimeStamp(signedData, timeStampToken);
        } else throw new ExceptionVS(responseVS.getMessage());
        return signedData;
    }

    /**
     * Digest for storing unique MessageCMS in database
     */
    public String getContentDigestStr() throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        return Base64.getEncoder().encodeToString(messageDigest.digest((byte[])getSignedContent().getContent()));
    }

    public Collection checkSignerCert(X509Certificate x509Cert) throws Exception {
        Store certs = getCertificates();
        X509CertificateHolder holder = new X509CertificateHolder(x509Cert.getEncoded());
        SignerId signerId = new SignerId(holder.getIssuer(), x509Cert.getSerialNumber());
        return certs.getMatches(signerId);
    }

    public static CMSSignedMessage FROM_PEM(String pkcs7PEMData) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pkcs7PEMData.getBytes())));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
            log.info("CMSObjectIdentifiers - envelopedData");
        }
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    public static CMSSignedMessage FROM_PEM(byte[] pemBytes) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pemBytes)));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
            log.info("CMSObjectIdentifiers - envelopedData");
        }
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    public static CMSSignedMessage FROM_PEM(InputStream inputStream) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(inputStream));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        if (!contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
            log.info("CMSObjectIdentifiers - envelopedData");
        }
        return new CMSSignedMessage(contentInfo.getEncoded());
    }

    private MessageData getMessageData() throws Exception {
        if(messageData == null) messageData = new MessageData();
        return messageData;
    }

    public UserVS getSigner() throws Exception {
        return  getMessageData().getSignerVS();
    }

    public Set<UserVS> getSigners() throws Exception {
        return getMessageData().getSigners();
    }

    public VoteVS getVoteVS() throws Exception {
        return getMessageData().getVoteVS();
    }

    public Set<X509Certificate> getSignersCerts() throws Exception {
        Set<X509Certificate> signerCerts = new HashSet<>();
        for(UserVS userVS : getMessageData().getSigners()) {
            signerCerts.add(userVS.getCertificate());
        }
        return signerCerts;
    }

    public X509Certificate getCurrencyCert() throws Exception {
        return getMessageData().getCurrencyCert();
    }

    private class MessageData {

        private UserVS signerVS;
        private Set<UserVS> signers;
        private TimeStampToken timeStampToken;
        private VoteVS voteVS;
        private X509Certificate currencyCert;


        public MessageData() throws Exception {
            checkSignature();
        }

        /**
         * verify that the signature is correct and that it was generated when the
         * certificate was current(assuming the cert is contained in the message).
         */
        private boolean checkSignature() throws Exception {
            Store certs = getCertificates();
            SignerInformationStore signerInfos = getSignerInfos();
            Set<X509Certificate> signerCerts = new HashSet<>();
            log.info("checkSignature - cms document with '" + signerInfos.size() + "' signers");
            Iterator it = signerInfos.getSigners().iterator();
            Date firstSignature = null;
            signers = new HashSet<>();
            while (it.hasNext()) {
                SignerInformation   signer = (SignerInformation)it.next();
                Collection certCollection = certs.getMatches(signer.getSID());
                Iterator certIt = certCollection.iterator();
                X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER).getCertificate(
                        (X509CertificateHolder) certIt.next());
                log.info("checkSignature - cert: " + cert.getSubjectDN() + " - " + certCollection.size() + " match");
                try {
                    signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            ContextVS.PROVIDER).build(cert));
                    //concurrency issues ->
                    //signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(ContextVS.PROVIDER).build(cert));
                } catch(CMSVerifierCertificateNotValidException ex) {
                    log.log(Level.SEVERE, "checkSignature - cert notBefore: " + cert.getNotBefore() + " - NotAfter: " +
                            cert.getNotAfter());
                    throw ex;
                } catch (Exception ex) {
                    throw ex;
                }
                UserVS userVS = UserVS.FROM_X509_CERT(cert);
                userVS.setSignerInformation(signer);
                TimeStampToken tsToken = checkTimeStampToken(signer);
                userVS.setTimeStampToken(tsToken);
                if(tsToken != null) {
                    Date timeStampDate = tsToken.getTimeStampInfo().getGenTime();
                    if(firstSignature == null || firstSignature.after(timeStampDate)) {
                        firstSignature = timeStampDate;
                        signerVS = userVS;
                    }
                    timeStampToken = tsToken;
                }
                signers.add(userVS);
                if (cert.getExtensionValue(ContextVS.VOTE_OID) != null) {
                    VoteVSDto dto = getSignedContent(VoteVSDto.class);
                    voteVS = new VoteVS(dto).loadSignatureData(cert, timeStampToken);
                } else if (cert.getExtensionValue(ContextVS.CURRENCY_OID) != null) {
                    currencyCert = cert;
                } else {signerCerts.add(cert);}
            }
            if(voteVS != null) voteVS.setServerCerts(signerCerts);
            return true;
        }

        public UserVS getSignerVS() {
            return signerVS;
        }

        public Set<UserVS> getSigners() {
            return signers;
        }

        public TimeStampToken getTimeStampToken() {
            return timeStampToken;
        }

        public VoteVS getVoteVS() {
            return voteVS;
        }

        public X509Certificate getCurrencyCert() {
            return currencyCert;
        }

    }
}
