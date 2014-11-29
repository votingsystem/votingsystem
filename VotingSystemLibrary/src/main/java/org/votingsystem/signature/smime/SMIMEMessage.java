package org.votingsystem.signature.smime;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.KeyGeneratorVS;
import org.votingsystem.signature.util.PKIXCertPathReviewer;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.FileUtils;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SMIMEMessage extends MimeMessage {

    private static Logger log = Logger.getLogger(SMIMEMessage.class);

    public static final String CONTENT_TYPE_VS = "CONTENT_TYPE_VS";

    //"application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + ".p7m";
    private ContentTypeVS contentTypeVS;
    private SMIMESigned smimeSigned;
    private String signedContent;
    private UserVS signerVS;
    private Set<UserVS> signers;
    private TimeStampToken timeStampToken;
    private SMIMEContentInfo contentInfo;
    private VoteVS voteVS;
    private X509Certificate cooinCert;

    public SMIMEMessage(MimeMultipart mimeMultipart, Header... headers) throws Exception {
        super(ContextVS.MAIL_SESSION);
        setContent(mimeMultipart);
        if (headers != null) {
            for(Header header : headers) {
                if (header != null) setHeader(header.getName(), header.getValue());
            }
        }
    }

    public SMIMEMessage(InputStream inputStream) throws Exception {
        super(ContextVS.MAIL_SESSION, inputStream);
    }

    public boolean isValidSignature() throws Exception {
        if(contentInfo == null) contentInfo = new SMIMEContentInfo(getContent(), getHeader(CONTENT_TYPE_VS));
        return true;
    }

    public ContentTypeVS getContentTypeVS() {
        return contentTypeVS;
    }

    public void setMessageID(String messageId) throws MessagingException {
        setHeader("Message-ID", messageId);
    }

    public String getSignedContent() {
        return signedContent;
    }

    public SMIMESigned getSmimeSigned() throws Exception {
        if(smimeSigned == null) isValidSignature();
        return smimeSigned;
    }

    public TimeStampToken getTimeStampToken () {
        return timeStampToken;
    }

    public X509Certificate getCooinCert() {return cooinCert;}

    public Set<X509Certificate> getSignersCerts() {
        Set<X509Certificate> signerCerts = new HashSet<X509Certificate>();
        for(UserVS userVS : signers) {
            signerCerts.add(userVS.getCertificate());
        }
        return signerCerts;
    }

    public ValidationResult verify(PKIXParameters params) throws Exception {
        SignedMailValidator validator = new SignedMailValidator(this, params);
        Iterator it = validator.getSignerInformationStore().getSigners().iterator();
        ValidationResult result = null;//only one signer supposed!!!
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            result = validator.getValidationResult(signer);
            if (result.isValidSignature()) log.debug("isValidSignature");
            else {
                log.debug("sigInvalid - Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    log.debug("error - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                log.debug("notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    log.debug("notification: " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) log.debug("certificate path valid");
                else log.debug("certificate path invalid");
                log.debug("certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    log.debug("error - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1).iterator();
                while (notificationsIt.hasNext()) {
                    log.debug("notification: " + notificationsIt.next().toString());
                }
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    log.debug("Certificate " + i + " ----------- ");
                    log.debug("Issuer: " + cert.getIssuerDN().getName());
                    log.debug("Subject: " + cert.getSubjectDN().getName());
                    log.debug("Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        log.debug( errorsIt.next().toString());
                    }
                    log.debug("Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        log.debug(notificationsIt.next().toString());
                    }
                    i++;
                }
            }
        }
        return result;
    }

    public void setTimeStampToken(TimeStampToken timeStampToken) throws Exception {
        if(timeStampToken == null ) throw new Exception("timestamp token null");
        DERObject derObject = new ASN1InputStream(timeStampToken.getEncoded()).readObject();
        DERSet derset = new DERSet(derObject);
        Attribute timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, derset);
        Hashtable hashTable = new Hashtable();
        hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
        AttributeTable timeStampAsAttributeTable = new AttributeTable(hashTable);
        byte[] timeStampTokenHash = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
        Iterator<SignerInformation> it = smimeSigned.getSignerInfos().getSigners().iterator();
        List<SignerInformation> newSigners = new ArrayList<SignerInformation>();
        while (it.hasNext()) {
            SignerInformation signer = it.next();
            byte[] digestBytes = CMSUtils.getSignerDigest(signer);
            if(Arrays.equals(timeStampTokenHash, digestBytes)) {
                log.debug("setTimeStampToken - found signer");
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable != null) {
                    log.debug("setTimeStampToken - signer with UnsignedAttributes");
                    hashTable = attributeTable.toHashtable();
                    hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
                    timeStampAsAttributeTable = new AttributeTable(hashTable);
                }
                updatedSigner = signer.replaceUnsignedAttributes(signer, timeStampAsAttributeTable);
                newSigners.add(updatedSigner);
            } else newSigners.add(signer);
        }
        SignerInformationStore newSignersStore = new SignerInformationStore(newSigners);
        CMSSignedData cmsdata =  smimeSigned.replaceSigners(smimeSigned, newSignersStore);
        replaceSigners(cmsdata);
    }

    private void replaceSigners(CMSSignedData cmsdata) throws Exception {
        log.debug("replaceSigners");
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        gen.addAttributeCertificates(cmsdata.getAttributeCertificates());
        gen.addCertificates(cmsdata.getCertificates());
        gen.addSigners(cmsdata.getSignerInfos());
        MimeMultipart mimeMultipart = gen.generate(smimeSigned.getContent(),smimeSigned.getContent().getFileName());
        setContent(mimeMultipart, mimeMultipart.getContentType());
        saveChanges();
    }

    public byte[] getBytes () throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        baos.close();
        return baos.toByteArray();
    }

    public VoteVS getVoteVS() {
        return voteVS;
    }

    public Set<UserVS> getSigners() {
        return signers;
    }

    public void setSigners(Set<UserVS> signers) {
        this.signers = signers;
    }

    public UserVS getSigner() {
        if(signerVS == null && !signers.isEmpty()) {
            signerVS = signers.iterator().next();
        }
        return signerVS;
    }

    private TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        TimeStampToken timeStampToken = null;
        AttributeTable  unsignedAttributes = signer.getUnsignedAttributes();
        if(unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if(timeStampAttribute != null) {
                DEREncodable dob = timeStampAttribute.getAttrValues().getObjectAt(0);
                CMSSignedData signedData = new CMSSignedData(dob.getDERObject().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                return timeStampToken;
            }
        } else log.debug("checkTimeStampToken - without unsignedAttributes");
        return timeStampToken;
    }

    public TimeStampRequest getTimeStampRequest() throws Exception {
        SignerInformation signerInformation =
                ((SignerInformation)getSmimeSigned().getSignerInfos().getSigners().iterator().next());
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(signerInformation.getDigestAlgOID(), as.getOctets(),
                BigInteger.valueOf(KeyGeneratorVS.INSTANCE.getNextRandomInt()));
    }

    //this is for multisigned messages
    public TimeStampRequest getTimeStampRequest(AlgorithmIdentifier digestAlgorithmIdentifier)
            throws NoSuchAlgorithmException, IOException, CMSException {
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        MessageDigest digestCalculator = MessageDigest.getInstance(
                CMSUtils.getDigestId(digestAlgorithmIdentifier.getAlgorithm().getId()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        smimeSigned.getSignedContent().write(baos);
        byte[] messageDigest = digestCalculator.digest(baos.toByteArray());
        return reqgen.generate(digestAlgorithmIdentifier.getAlgorithm(), messageDigest,
                BigInteger.valueOf(KeyGeneratorVS.INSTANCE.getNextRandomInt()));
    }

    /**
     * Digest for storing unique MessageSMIME in database
     */
    public String getContentDigestStr() throws Exception {
        if(contentInfo == null) isValidSignature();
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        return Base64.getEncoder().encodeToString(messageDigest.digest(signedContent.getBytes()));
    }

    public Collection checkSignerCert(X509Certificate x509Cert) throws Exception {
        if(smimeSigned == null) isValidSignature();
        Store certs = smimeSigned.getCertificates();
        X509CertificateHolder holder = new X509CertificateHolder(x509Cert.getEncoded());
        SignerId signerId = new SignerId(holder.getIssuer(), x509Cert.getSerialNumber());
        return certs.getMatches(signerId);
    }

    public class SMIMEContentInfo {

        public SMIMEContentInfo(Object content, String[] headerValues) throws Exception {
            if(content instanceof MimeMultipart){
                smimeSigned = new SMIMESigned((MimeMultipart)content);
                Object  cont = ((MimeMultipart)content).getBodyPart(0).getContent();
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
                        } else if (part instanceof ByteArrayInputStream) {
                            stringBuilder.append(new String(FileUtils.getBytesFromInputStream(
                                    (ByteArrayInputStream) cont), "UTF-8"));
                        } else new ExceptionVS(" TODO - get content from part instanceof -> " + part.getClass());
                    }
                    signedContent = stringBuilder.toString();
                } else if (cont instanceof ByteArrayInputStream) {
                    signedContent = new String(FileUtils.getBytesFromInputStream((ByteArrayInputStream) cont), "UTF-8");
                }
            } else throw new ExceptionVS("TODO - content instanceof String");
            checkSignature();
            if(headerValues != null && headerValues.length > 0) contentTypeVS = ContentTypeVS.getByName(headerValues[0]);
        }

        /**
         * verify that the signature is correct and that it was generated when the
         * certificate was current(assuming the cert is contained in the message).
         */
        private boolean checkSignature() throws Exception {
            Store certs = smimeSigned.getCertificates();
            SignerInformationStore signerInfos = smimeSigned.getSignerInfos();
            Set<X509Certificate> signerCerts = new HashSet<X509Certificate>();
            log.debug("checkSignature - document with '" + signerInfos.size() + "' signers");
            Iterator it = signerInfos.getSigners().iterator();
            Date firstSignature = null;
            signers = new HashSet<UserVS>();
            while (it.hasNext()) {// check each signer
                SignerInformation   signer = (SignerInformation)it.next();
                Collection certCollection = certs.getMatches(signer.getSID());
                Iterator certIt = certCollection.iterator();
                X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER).getCertificate(
                        (X509CertificateHolder) certIt.next());
                log.debug("checkSignature - cert: " + cert.getSubjectDN() + " - " + certCollection.size() + " match");
                try {
                    verifySigner(signer, new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            ContextVS.PROVIDER).build(cert));
                    //concurrency issues ->
                    //signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(ContextVS.PROVIDER).build(cert));
                } catch(CMSVerifierCertificateNotValidException ex) {
                    log.error("checkSignature - cert.getNotBefore(): " + cert.getNotBefore());
                    log.error("checkSignature - cert.getNotAfter(): " + cert.getNotAfter());
                    throw ex;
                } catch (Exception ex) {
                    throw ex;
                }
                UserVS userVS = UserVS.getUserVS(cert);
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
                    JSONObject voteJSON = (JSONObject) JSONSerializer.toJSON(signedContent);
                    voteVS = VoteVS.getInstance(voteJSON, cert, timeStampToken);
                } else if (cert.getExtensionValue(ContextVS.COOIN_OID) != null) {
                    cooinCert = cert;
                } else {signerCerts.add(cert);}
            }
            if(voteVS != null) voteVS.setServerCerts(signerCerts);
            return true;
        }

        private byte[] verifySigner(SignerInformation signer, SignerInformationVerifier verifier) throws CMSException,
                OperatorCreationException, IOException, MessagingException {
            DERObject derObject = CMSUtils.getSingleValuedSignedAttribute(signer.getSignedAttributes(),
                    CMSAttributes.signingTime, "signing-time");
            Time signingTime = Time.getInstance(derObject);
            X509CertificateHolder dcv = verifier.getAssociatedCertificate();
            if (!dcv.isValidOn(signingTime.getDate()))  {
                throw new CMSVerifierCertificateNotValidException("verifier not valid at signingTime");
            }
            // RFC 3852 11.1 Check the content-type attribute is correct -> missing
            //verify digest
            DigestCalculator calc = verifier.getDigestCalculator(signer.getDigestAlgorithmID());
            OutputStream digOut = calc.getOutputStream();
            smimeSigned.getSignedContent().write(digOut);
            digOut.close();
            byte[] resultDigest = calc.getDigest();
            byte[] signerDigest = CMSUtils.getSignerDigest(signer);
            if (!Arrays.equals(resultDigest, signerDigest)) throw new CMSSignerDigestMismatchException(
                    "message-digest attribute value does not match calculated value");
            return resultDigest;
        }
    }

}