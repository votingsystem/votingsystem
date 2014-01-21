package org.votingsystem.signature.smime;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.util.PKIXCertPathReviewer;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.util.StringUtils;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SMIMEMessageWrapper extends MimeMessage {
    
    private static Logger logger = Logger.getLogger(SMIMEMessageWrapper.class);

    private String contentType;
    private String signedContent;
    private SMIMESigned smimeSigned = null;

	private Set<UserVS> signers = null;
	private UserVS signerVS;
	private VoteVS voteVS;
	private boolean isValidSignature = false;
	
    public SMIMEMessageWrapper(Session session) throws MessagingException {
        super(session);
        logger.debug("SMIMEMessageWrapper(Session session)");
        String fileName =  StringUtils.randomLowerString(System.currentTimeMillis(), 7);
        setDisposition("attachment; fileName=" + fileName + ".p7m");
        contentType = "application/x-pkcs7-mime; smime-type=signed-data; name=" + fileName + ".p7m";
    }
    
    public SMIMEMessageWrapper (SMIMESigned simeSigned) throws Exception {
    	super(ContextVS.MAIL_SESSION);
    	this.smimeSigned = simeSigned;
        smimeSigned = new SMIMESigned(this); 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ((CMSProcessable)smimeSigned.getSignedContent()).write(baos);
        signedContent = baos.toString(); 
        checkSignature();	
    }
 
    public SMIMEMessageWrapper (InputStream inputStream) throws Exception {
        super(ContextVS.MAIL_SESSION, inputStream);
        init();
    }

    private void init() throws Exception {
        if(getContent() instanceof MimeMultipart){
            logger.debug("content instanceof MimeMultipart");
            smimeSigned = new SMIMESigned((MimeMultipart)getContent());
            MimeBodyPart content = smimeSigned.getContent();
            Object  cont = content.getContent();
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
                        ByteArrayInputStream contentStream = (ByteArrayInputStream)cont;
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buf =new byte[4096];
                        int len;
                        while((len = contentStream.read(buf)) > 0){
                            output.write(buf,0,len);
                        }
                        output.close();
                        contentStream.close();
                        stringBuilder.append(new String(output.toByteArray()));
                    }  else  {
                        logger.debug(" TODO - get content from part instanceof -> " + part.getClass());
                    }
                }
                signedContent = stringBuilder.toString();
            } else if (cont instanceof ByteArrayInputStream) {
                ByteArrayInputStream contentStream = (ByteArrayInputStream)cont;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buf =new byte[2048];
                int len;
                while((len = contentStream.read(buf)) > 0){
                    output.write(buf,0,len);
                }
                output.close();
                contentStream.close();
                signedContent = new String(output.toByteArray());
            }
        } else if(getContent() instanceof String){ 
            logger.error("TODO - content instanceof String -> " + getContent()); 
        }
        checkSignature(); 
    }
    
    public void updateChanges() throws Exception {
    	super.saveChanges();
    	init();
    }

    public void setMessageID(String messageId) throws MessagingException {
        setHeader("Message-ID", messageId);
    }

    /**
     * @return the signedContent
     */
    public String getSignedContent() {
        return signedContent;
    }

    /**
     * Digest for storing unique MessageSMIME in database 
     * @return the contentDigestStr
     * @throws java.security.NoSuchAlgorithmException
     */
    public String getContentDigestStr() throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] resultDigest =  messageDigest.digest(signedContent.getBytes());
        return new String(Base64.encode(resultDigest));
    }

    /**
     * @return the smimeSigned
     */
    public SMIMESigned getSmimeSigned() {
        return smimeSigned;
    }

	private boolean verifySignerCert(SignerInformation signer, X509Certificate cert) {
    	boolean result = false;
        try {
        	if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(
        			ContextVS.PROVIDER).build(cert))){
                result = true;
            } else {logger.debug("signature failed!");}
        } catch(CMSVerifierCertificateNotValidException ex) {
        	logger.debug("-----> cert.getNotBefore(): " + cert.getNotBefore());
        	logger.debug("-----> cert.getNotAfter(): " + cert.getNotAfter());
    		logger.error(ex.getMessage(), ex);
        } finally {
        	return result;
        }
    }

    public boolean isValidSignature()  {
    	return isValidSignature;
    }

    /**
     * verify that the signature is correct and that it was generated when the
     * certificate was current(assuming the cert is contained in the message).
     */
    private void checkSignature() throws Exception {
        Store certs = smimeSigned.getCertificates();
        SignerInformationStore  signerInfos = smimeSigned.getSignerInfos();
        Set<X509Certificate> signerCerts = new HashSet<X509Certificate>();
        logger.debug("checkSignature - document with '" + signerInfos.size() + "' signers");
        Collection c = signerInfos.getSigners();
        Iterator it = c.iterator();
        Date firstSignature = null;
        isValidSignature = false;
        signers = new HashSet<UserVS>();
        while (it.hasNext()) {// check each signer
            SignerInformation   signer = (SignerInformation)it.next();
            Collection          certCollection = certs.getMatches(signer.getSID());
            Iterator        certIt = certCollection.iterator();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider(ContextVS.PROVIDER)
                    .getCertificate((X509CertificateHolder)certIt.next());
            logger.debug("checkSignature - cert: " + cert.getSubjectDN() + " --- " + certCollection.size() + " match");
            isValidSignature = verifySignerCert(signer, cert);
            if(!isValidSignature) return;
            UserVS userVS = UserVS.getUserVS(cert);
            userVS.setSignerInformation(signer);
            TimeStampToken timeStampToken = checkTimeStampToken(signer);//method can only be called after verify.
            userVS.setTimeStampToken(timeStampToken);
            if(timeStampToken != null) {
                Date timeStampDate = timeStampToken.getTimeStampInfo().getGenTime();
                if(firstSignature == null || firstSignature.after(timeStampDate)) {
                    firstSignature = timeStampDate;
                    this.signerVS = userVS;
                }
            }
            signers.add(userVS);
            if (cert.getExtensionValue(ContextVS.VOTE_OID) != null) {
                JSONObject voteJSON = (JSONObject) JSONSerializer.toJSON(signedContent);
                voteVS = VoteVS.getInstance(voteJSON, cert, timeStampToken);
            } else {signerCerts.add(cert);}
        }
        if(voteVS != null) voteVS.setServerCerts(signerCerts);
    }

    public ValidationResult verify(PKIXParameters params) throws Exception {
        SignedMailValidator validator = new SignedMailValidator(this, params);
        // iterate over all signatures and print results
        Iterator it = validator.getSignerInformationStore().getSigners().iterator();
        Locale loc = Locale.ENGLISH;
        //only one signer supposed!!!
        ValidationResult result = null;
        while (it.hasNext()) {
            SignerInformation signer = (SignerInformation) it.next();
            result = validator.getValidationResult(signer);
            if (result.isValidSignature()){
                logger.debug("isValidSignature");
            }
            else {
                logger.debug("sigInvalid");
                logger.debug("Errors:");
                Iterator errorsIt = result.getErrors().iterator();
                while (errorsIt.hasNext()) {
                    logger.debug("ERROR - " + errorsIt.next().toString());
                }
            }
            if (!result.getNotifications().isEmpty()) {
                logger.debug("Notifications:");
                Iterator notIt = result.getNotifications().iterator();
                while (notIt.hasNext()) {
                    logger.debug("NOTIFICACION - " + notIt.next());
                }
            }
            PKIXCertPathReviewer review = result.getCertPathReview();
            if (review != null) {
                if (review.isValidCertPath()) {
                    logger.debug("Certificate path valid");
                }
                else {
                    logger.debug("Certificate path invalid");
                }
                logger.debug("Certificate path validation results:");
                Iterator errorsIt = review.getErrors(-1).iterator();
                while (errorsIt.hasNext()) {
                    logger.debug("ERROR - " + errorsIt.next().toString());
                }
                Iterator notificationsIt = review.getNotifications(-1)
                        .iterator();
                while (notificationsIt.hasNext()) {
                    logger.debug("NOTIFICACION - " + notificationsIt.next().toString());
                }
                // per certificate errors and notifications
                Iterator certIt = review.getCertPath().getCertificates().iterator();
                int i = 0;
                while (certIt.hasNext()) {
                    X509Certificate cert = (X509Certificate) certIt.next();
                    logger.debug("Certificate " + i + " ----------- ");
                    logger.debug("Issuer: " + cert.getIssuerDN().getName());
                    logger.debug("Subject: " + cert.getSubjectDN().getName());
                    logger.debug("Errors:");
                    errorsIt = review.getErrors(i).iterator();
                    while (errorsIt.hasNext())  {
                        logger.debug( errorsIt.next().toString());
                    }
                    // notifications
                    logger.debug("Notifications:");
                    notificationsIt = review.getNotifications(i).iterator();
                    while (notificationsIt.hasNext()) {
                        logger.debug(notificationsIt.next().toString());
                    }
                    i++;
                }
            }
        }
        return result;
    }

    public void setTimeStampToken(TimeStampToken timeStampToken) throws Exception {
        if(timeStampToken == null ) throw new Exception("NULL_TIME_STAMP_TOKEN");
        DERObject derObject = new ASN1InputStream(timeStampToken.
                getEncoded()).readObject();
        DERSet derset = new DERSet(derObject);
        Attribute timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, derset);

        Hashtable hashTable = new Hashtable();
        hashTable.put(PKCSObjectIdentifiers.
                        id_aa_signatureTimeStampToken, timeStampAsAttribute);
        AttributeTable timeStampAsAttributeTable = new AttributeTable(hashTable);

        byte[] hashTokenBytes = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
        //String hashTokenStr = new String(Base64.encode(hashTokenBytes));
        //logger.debug("setTimeStampToken - timeStampToken - hashTokenStr: " +  hashTokenStr);
        SignerInformationStore  signers = smimeSigned.getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        List<SignerInformation> newSigners = new ArrayList<SignerInformation>();
        while (it.hasNext()) {
            SignerInformation signer = it.next();
            byte[] digestBytes = signer.getContentDigest();//method can only be called after verify.
            //String digestStr = new String(Base64.encode(digestBytes));
            //logger.debug("setTimeStampToken - hash signerVS: " +  digestStr +
            //        " - hash token: " + hashTokenStr);
            if(Arrays.equals(hashTokenBytes, digestBytes)) {
                logger.debug("setTimeStampToken - signerVS");
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable != null) {
                    logger.debug("setTimeStampToken - signer with UnsignedAttributes");
                    hashTable = attributeTable.toHashtable();
                    hashTable.put(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, timeStampAsAttribute);
                    attributeTable = new AttributeTable(hashTable);
                }
                updatedSigner = signer.replaceUnsignedAttributes(
                            signer, timeStampAsAttributeTable);
                newSigners.add(updatedSigner);
            } else newSigners.add(signer);
        }
        //logger.debug("setTimeStampToken - num. signers: " + newSigners.size());
        SignerInformationStore newSignersStore = new SignerInformationStore(newSigners);
        CMSSignedData cmsdata = smimeSigned.replaceSigners(smimeSigned, newSignersStore);
        replaceSigners(cmsdata);
    }

    private void replaceSigners(CMSSignedData cmsdata) throws Exception {
        logger.debug("replaceSigners");
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        gen.addAttributeCertificates(cmsdata.getAttributeCertificates());
        gen.addCertificates(cmsdata.getCertificates());
        gen.addSigners(cmsdata.getSignerInfos());
        MimeMultipart mimeMultipart = gen.generate(smimeSigned.getContent(), smimeSigned.getContent().getFileName());
        setContent(mimeMultipart, mimeMultipart.getContentType());
        saveChanges();
    }

    public TimeStampRequest getTimeStampRequest() {
        if(smimeSigned == null) return null;
        SignerInformation signerInformation = ((SignerInformation)
                        smimeSigned.getSignerInfos().getSigners().iterator().next());
        if(signerInformation == null) {
            logger.debug("signerInformation null");
            return null;
        }
        AttributeTable table = signerInformation.getSignedAttributes();
        Attribute hash = table.get(CMSAttributes.messageDigest);
        ASN1OctetString as = ((ASN1OctetString)hash.getAttrValues().getObjectAt(0));
        //byte[] digest = Base64.encode(as.getOctets());
        //logger.debug(" - digest: " + new String(digest));
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        //reqgen.setReqPolicy(m_sPolicyOID);
        return reqgen.generate(signerInformation.getDigestAlgOID(), as.getOctets(),
                BigInteger.valueOf(VotingSystemKeyGenerator.INSTANCE.getNextRandomInt()));
    }

    public byte[] getBytes () throws Exception {
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

    //Call this method after isValidSignature()!!!
    private TimeStampToken checkTimeStampToken(SignerInformation signer) throws Exception {
        TimeStampToken timeStampToken = null;
        byte[] digestBytes = signer.getContentDigest();//method can only be called after verify.
        String digestStr = new String(Base64.encode(digestBytes));
        AttributeTable  unsignedAttributes = signer.getUnsignedAttributes();
        if(unsignedAttributes != null) {
            Attribute timeStampAttribute = unsignedAttributes.get(
                    PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if(timeStampAttribute != null) {
                DEREncodable dob = timeStampAttribute.getAttrValues().getObjectAt(0);
                CMSSignedData signedData =
                        new CMSSignedData(dob.getDERObject().getEncoded());
                timeStampToken = new TimeStampToken(signedData);
                byte[] hashToken = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
                String digestTokenStr = new String(Base64.encode(hashToken));
                Calendar cal = new GregorianCalendar();
                cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                logger.debug("checkTimeStampToken - timeStampToken: " +  cal.getTime());
                //logger.debug("checkTimeStampToken - digestStr: " + digestStr 
                //		+ " - digestTokenStr " + digestTokenStr);
                return timeStampToken;
            }
        } else logger.debug("checkTimeStampToken - without unsignedAttributes"); 
        return timeStampToken;
    }
    
}
