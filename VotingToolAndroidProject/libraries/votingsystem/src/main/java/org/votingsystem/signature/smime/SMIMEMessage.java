package org.votingsystem.signature.smime;

import android.util.Log;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1InputStream;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.DEREncodable;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.asn1.cms.Time;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSSignerDigestMismatchException;
import org.bouncycastle2.cms.CMSVerifierCertificateNotValidException;
import org.bouncycastle2.cms.SignerId;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.SignerInformationVerifier;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle2.mail.smime.SMIMESigned;
import org.bouncycastle2.operator.DigestCalculator;
import org.bouncycastle2.operator.OperatorCreationException;
import org.bouncycastle2.util.Store;
import org.json.JSONObject;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.util.KeyGeneratorVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class SMIMEMessage extends MimeMessage implements Serializable {

    public static final String TAG = SMIMEMessage.class.getSimpleName();

    private static final long serialVersionUID = 1L;
    public static final String CONTENT_TYPE_VS = "CONTENT_TYPE_VS";
	
    static {
        MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();

        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        
        mc.addMailcap("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.pkcs7_signature");
        mc.addMailcap("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.pkcs7_mime");
        mc.addMailcap("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.x_pkcs7_signature");
        mc.addMailcap("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.x_pkcs7_mime");
        mc.addMailcap("multipart/signed;; x-java-content-handler=org.bouncycastle2.mail.smime.handlers.multipart_signed");

        CommandMap.setDefaultCommandMap(mc);
    }

    private ContentTypeVS contentTypeVS;
    private String signedContent;
    private SMIMESigned smimeSigned = null;
    private Set<UserVS> signers = null;
    private UserVS signerVS;
    private VoteVS voteVS;
    private X509Certificate cooinCert;
    private SMIMEContentInfo contentInfo;
    private TimeStampToken timeStampToken;


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

    public void setMessageID(String messageId) throws MessagingException {
        setHeader("Message-ID", messageId);
    }

    public String getMessageID() throws MessagingException {
        String result = null;
        String[] headers = getHeader("Message-ID");
        if(headers != null && headers.length > 0) result = headers[0];
        return result;
    }

    public String getSignedContent() {
        return signedContent;
    }

    public SMIMESigned getSmimeSigned() throws Exception {
        if(smimeSigned == null) isValidSignature();
        return smimeSigned;
    }

    public X509Certificate getCooinCert() {return cooinCert;}

    public boolean isValidSignature() throws Exception {
        if(contentInfo == null) contentInfo = new SMIMEContentInfo(getContent(), getHeader(CONTENT_TYPE_VS));
        return true;
    }
    
    public boolean hasTimeStampToken() throws Exception {
        //Call this method after isValidSignature()
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        boolean result = false;
        while (it.hasNext()) {
            TimeStampToken timeStampToken = checkTimeStampToken(it.next());
            if(timeStampToken != null) result = true;
        }
        return result;
    }
    
    private TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        //Call this method after isValidSignature()
        TimeStampToken timeStampToken = null;
        //byte[] digestBytes = signer.getContentDigest();//method can only be called after verify.
        //String digestStr = new String(Base64.encode(digestBytes));
        //Log.d(TAG, "checkTimeStampToken - digestStr: " + digestStr);
        AttributeTable  unsignedAttributes = signer.getUnsignedAttributes();
        if(unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(
                    PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if(timeStampAttribute != null) {
                DEREncodable dob = timeStampAttribute.getAttrValues().getObjectAt(0);
                org.bouncycastle2.cms.CMSSignedData signedData = 
                        new org.bouncycastle2.cms.CMSSignedData(dob.getDERObject().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                //byte[] hashToken = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
                //String hashTokenStr = new String(Base64.encode(hashToken));
                //Log.d(TAG, "checkTimeStampToken - timeStampToken - hashTokenStr: " +  hashTokenStr);
                Calendar cal = new GregorianCalendar();
                cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                Log.d(TAG, "checkTimeStampToken - timeStampToken - date: "
                        +  DateUtils.getDateStr(cal.getTime()));
                return timeStampToken;
            }
        } else Log.d(TAG, "--- without unsignedAttributes");
        return timeStampToken;
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
                Log.d(TAG + ".setTimeStampToken" , "found signer");
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable != null) {
                    Log.d(TAG + "setTimeStampToken", "signer with UnsignedAttributes");
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

    private void replaceSigners(CMSSignedData cmsdata) throws Exception {
        SMIMESignedGenerator gen =  new SMIMESignedGenerator();
        gen.addAttributeCertificates(cmsdata.getAttributeCertificates());
        gen.addCertificates(cmsdata.getCertificates());
        gen.addSigners(cmsdata.getSignerInfos());
        MimeMultipart mimeMultipart = gen.generate(smimeSigned.getContent(), 
                smimeSigned.getContent().getFileName());
        setContent(mimeMultipart, mimeMultipart.getContentType());
        saveChanges();
    }

    public byte[] getBytes () throws IOException, MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        byte[] messageBytes = baos.toByteArray();
        baos.close();
        return messageBytes;
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

    public Collection checkSignerCert(X509Certificate x509Cert) throws Exception {
        if(smimeSigned == null) isValidSignature();
        Store certs = smimeSigned.getCertificates();
        X509CertificateHolder holder = new X509CertificateHolder(x509Cert.getEncoded());
        SignerId signerId = new SignerId(holder.getIssuer(), x509Cert.getSerialNumber());
        return certs.getMatches(signerId);
    }

    public TimeStampToken getTimeStampToken(X509Certificate requestCert) throws Exception {
        Store certs = smimeSigned.getCertificates();
        SignerInformationStore signerInfos = smimeSigned.getSignerInfos();
        Iterator it = signerInfos.getSigners().iterator();
        while (it.hasNext()) {// check each signer
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
            Log.d(TAG, "checkSignature - document with '" + signerInfos.size() + "' signers");
            Iterator it = signerInfos.getSigners().iterator();
            Date firstSignature = null;
            signers = new HashSet<UserVS>();
            while (it.hasNext()) {// check each signer
                SignerInformation   signer = (SignerInformation)it.next();
                Collection certCollection = certs.getMatches(signer.getSID());
                Iterator certIt = certCollection.iterator();
                X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER).getCertificate(
                        (X509CertificateHolder) certIt.next());
                Log.d(TAG, "checkSignature - cert: " + cert.getSubjectDN() + " - " + certCollection.size() + " match");
                try {
                    verifySigner(signer, new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                            ContextVS.PROVIDER).build(cert));
                    //concurrency issues ->
                    //signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(ContextVS.PROVIDER).build(cert));
                } catch(CMSVerifierCertificateNotValidException ex) {
                    Log.d(TAG, "checkSignature - cert.getNotBefore(): " + cert.getNotBefore());
                    Log.d(TAG, "checkSignature - cert.getNotAfter(): " + cert.getNotAfter());
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
                    JSONObject voteJSON = new JSONObject(signedContent);
                    voteVS = VoteVS.getInstance(JsonUtils.toMap(voteJSON), cert, timeStampToken);
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