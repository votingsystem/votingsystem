package org.votingsystem.applet.validationtool.model;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignedFile {
        
    private static Logger logger = Logger.getLogger(SignedFile.class);
    
    private byte[] signedFileBytes = null;
    private String name = null;
    private TimeStampToken timeStampToken = null;
    private SMIMEMessageWrapper smimeMessageWraper = null;
    private boolean signatureVerified = false;
    private PdfPKCS7 pdfPKCS7 = null;

    
    public SignedFile(byte[] signedFileBytes, String name) throws Exception {
        this.name = name;
        this.signedFileBytes = signedFileBytes;
        if(name.toLowerCase().endsWith(".pdf")) {
            PdfReader reader = new PdfReader(signedFileBytes);
            AcroFields acroFields = reader.getAcroFields();
            ArrayList<String> names = acroFields.getSignatureNames();
            for (String sigName : names) {
                //logger.debug(" - SignedFile - covers whole document: " + 
                //        acroFields.signatureCoversWholeDocument(sigName));
                pdfPKCS7 = acroFields.verifySignature(sigName, "BC");
                timeStampToken = pdfPKCS7.getTimeStampToken();
                
                if(!pdfPKCS7.verify()) {
                    //logger.error(" - checkSignature - VERIFICATION FAILED!!!");
                    signatureVerified = false;
                } else {
                    //logger.error("checkSignature - OK");
                    signatureVerified = true;
                } 
                //X509Certificate signingCert = pk.getSigningCertificate();
                //logger.debug(" checkSignature - signingCert: " + signingCert.getSubjectDN());
                //Calendar signDate = pk.getSignDate();
                //logger.debug(" - checkSignature - signingCert: " + signDate.getTime().toString());
                //X509Certificate[] pkc = (X509Certificate[])pk.getSignCertificateChain();
                timeStampToken = pdfPKCS7.getTimeStampToken();
                //logger.debug(" - checkSignature - timeStampToken: " + 
                //        timeStampToken.getTimeStampInfo().getGenTime().toString());
                //KeyStore keyStore = signatureVSService.getTrustedCertsKeyStore()
                //Object[] fails = PdfPKCS7.verifyCertificates(pkc, keyStore, null, signDate);
                //if(fails != null) {...}
            }
            
        } else if(name.toLowerCase().endsWith(".p7m")){
            smimeMessageWraper = new SMIMEMessageWrapper(
                new ByteArrayInputStream(signedFileBytes));
            signatureVerified = smimeMessageWraper.isValidSignature();
            if(signatureVerified) timeStampToken = smimeMessageWraper.
                    getSigner().getTimeStampToken();
        } else {
            logger.error(" #### UNKNOWN FILE TYPE -> " + name + 
                    " trying with SMIMEMessageWrapper");
            smimeMessageWraper = new SMIMEMessageWrapper(
                new ByteArrayInputStream(signedFileBytes));
            signatureVerified = smimeMessageWraper.isValidSignature();
        }
    }
    
    public byte[] getDigestToken(TimeStampToken timeStampToken) {
        if(timeStampToken == null) return null;
        CMSSignedData tokenCMSSignedData = timeStampToken.toCMSSignedData();		
        Collection signers = tokenCMSSignedData.getSignerInfos().getSigners();
        SignerInformation tsaSignerInfo = (SignerInformation)signers.iterator().next();

        AttributeTable signedAttrTable = tsaSignerInfo.getSignedAttributes();
        ASN1EncodableVector v = signedAttrTable.getAll(CMSAttributes.messageDigest);
        Attribute t = (Attribute)v.get(0);
        ASN1Set attrValues = t.getAttrValues();
        DERObject validMessageDigest = attrValues.getObjectAt(0).getDERObject();

        ASN1OctetString signedMessageDigest = (ASN1OctetString)validMessageDigest;			
        byte[] digestToken = signedMessageDigest.getOctets();  
        String digestTokenStr = new String(Base64.encode(digestToken));
        logger.debug("============= digestTokenStr: " + digestTokenStr);
        return digestToken;
    }
    
    public boolean isValidSignature() {
        return signatureVerified;
    }
    
    /**
     * @return the signedFileBytes
     */
    public byte[] getSignedFileBytes() {
        return signedFileBytes;
    }

    /**
     * @param signedFileBytes the signedFileBytes to set
     */
    public void setSignedFileBytes(byte[] signedFileBytes) {
        this.signedFileBytes = signedFileBytes;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public SMIMEMessageWrapper getSMIMEMessageWraper() {
        return smimeMessageWraper;
    }
    
    public JSONObject getContent() throws Exception {
        JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(
                    smimeMessageWraper.getSignedContent());
        return contentJSON;
    }    
    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isPDF() {
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".pdf") && 
                signatureVerified) return true;
        else return false;
    }
    
    public boolean isSMIME() {
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7m") && 
                signatureVerified) return true;
        else return false;
    }
    
    public String getSignerNif() {
        return smimeMessageWraper.getSigner().getNif();
    }
    
    public byte[] getFileBytes() {
        return signedFileBytes;
    }
    
    public String getNifFromRepresented() {
        if(name != null && name.contains("_RepDoc_")) {
            String nifParts = name.split("_RepDoc_")[1];
            return nifParts.split(".p7m")[0];
        } else return null;
    }
    
    public File getFile() throws Exception {
        File file = File.createTempFile("signedFile", ".votingSystem");
        file.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(signedFileBytes), file);
        return file;
    }

    public Long getSelectedOptionId() {
        if(smimeMessageWraper == null) return null;
        String signedContent = smimeMessageWraper.getSignedContent();
        Object content = JSONSerializer.toJSON(signedContent);
        if(content instanceof JSONObject) {
            JSONObject contentJSON = (JSONObject)content;
            if(contentJSON.containsKey("optionSelectedId")) {
                return contentJSON.getLong("optionSelectedId");
            }
        } else {
            logger.error(" File '" + name + "' content is instance of " + 
                    content.getClass());
        }
        return null;
    }
    
    public Long getNumSerieSignerCert() {
        if(smimeMessageWraper == null) return null;
        UserVS userVS = smimeMessageWraper.getSigner();
        return userVS.getCertificate().getSerialNumber().longValue();
    }
        
    //{"representativeNif":"00000002W","operation":"REPRESENTATIVE_SELECTION","UUID":"dcfacb17-a323-4853-b446-8e28d8f2d0a4"}   
    public ResponseVS validateAsRepresentationDocument(
            Set<X509Certificate> systemTrustedCerts, 
            Date dateFinish, String representativeNif, 
            X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "signatureErrorMsg", name));    
        }
        if(representativeNif == null) return new ResponseVS(
                ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("badRequestMsg") +
                " - " + ContextVS.getInstance().getMessage("missingRepresentativeNifErrorMsg"));
        UserVS userVS = smimeMessageWraper.getSigner();
        try {
            PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                    userVS.getCertificate(), systemTrustedCerts, false);
            //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "certificateErrorMsg", userVS.getNif(), name));
        } 
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("timestampValidationErrorMsg", name));
        }
        Date tokenDate = timeStampToken.getTimeStampInfo().getGenTime();
        if(!tokenDate.before(dateFinish)) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "dateErrorMsg", DateUtils.getStringFromDate(dateFinish),
                    DateUtils.getStringFromDate(tokenDate), name));
        }
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("representativeNif")) {
            String jsonNIF = contentJSON.getString("representativeNif");
            if(!representativeNif.trim().equals(jsonNIF)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("nifErrorMsg", representativeNif, jsonNIF, name));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingRepresentativeNifErrorMsg", name));

        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.REPRESENTATIVE_SELECTION != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("operationErrorMsg", TypeVS.REPRESENTATIVE_SELECTION.toString(),
                                operationType.toString(), name));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", name));
        
        return new ResponseVS(ResponseVS.SC_OK);
    }
    
    //{"operation":"SEND_SMIME_VOTE","optionSelectedId":2,"UUID":"cfbeec4a-f87c-4e4f-b442-4b127259fbd5",
    //"optionSelectedContent":"depende","eventURL":"http://192.168.1.20:8080/AccessControl/eventVSElection/1"}
    public ResponseVS<Long> validateAsVote(Set<X509Certificate> systemTrustedCerts,
        Set<X509Certificate> eventTrustedCerts, Long optionSelectedId, String eventURL, Date dateInit,
        Date dateFinish, X509Certificate timeStampServerCert) throws Exception {
        Long signedFileOptionSelectedId = null;
        if(!signatureVerified) {            
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "signatureErrorMsg", name));
            
        }
        Set<UserVS> signersVS = smimeMessageWraper.getSigners();
        for(UserVS signerVS:signersVS) {
            try {
                if(signerVS.getTimeStampToken() != null) {//user signature
                    PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                        signerVS.getCertificate(), eventTrustedCerts, false);
                    //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
                } else {//server signature
                    PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                        signerVS.getCertificate(), systemTrustedCerts, false);
                } 
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                        "certificateErrorMsg", signerVS.getNif(), name));
            } 
        }
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("timestampValidationErrorMsg", name));
        }
        Date tokenDate = smimeMessageWraper.getSigner().getTimeStampToken().
                getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                        "tokenDateErrorMsg", name, DateUtils.getStringFromDate(tokenDate),
                        DateUtils.getStringFromDate(dateInit),
                        DateUtils.getStringFromDate(dateFinish)));
        }
        
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.SEND_SMIME_VOTE != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("operationErrorMsg", TypeVS.SEND_SMIME_VOTE.toString(),
                                operationType.toString(), name));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", name));
        if(contentJSON.containsKey("optionSelectedId")) {
            signedFileOptionSelectedId = contentJSON.getLong("optionSelectedId");
            if(optionSelectedId != null && signedFileOptionSelectedId != optionSelectedId) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("optionSelectedErrorMsg", name,
                                optionSelectedId, signedFileOptionSelectedId));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage(
                "missingOptionSelectedErrorMsg", name));

        if(contentJSON.containsKey("eventURL")) {
            String documentEventURL = contentJSON.getString("eventURL");
            if(!eventURL.equals(documentEventURL)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("eventURLErrorMsg", name, eventURL, documentEventURL));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                getMessage("jsonErrorMsg") + " - " + ContextVS.getInstance().
                getMessage("missingEventURLErrorMsg", name));
        return new ResponseVS(ResponseVS.SC_OK, signedFileOptionSelectedId);
    }
    
    public ResponseVS validateAsAccessRequest(
        Set<X509Certificate> systemTrustedCerts, 
        String eventURL, Date dateInit, Date dateFinish, 
        X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "signatureErrorMsg", name));
            
        }
        UserVS signer = smimeMessageWraper.getSigner();        try {
        PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                    signer.getCertificate(), systemTrustedCerts, false);
            //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "certificateErrorMsg", signer.getNif(), name));
        } 
        
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.ACCESS_REQUEST != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("operationErrorMsg", TypeVS.ACCESS_REQUEST.toString(),
                                operationType.toString(), name));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", name));
        
        if(contentJSON.containsKey("eventURL")) {
            String  documentEventURL = contentJSON.getString("eventURL");
            if(!eventURL.equals(documentEventURL)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("eventURLErrorMsg", name, eventURL, documentEventURL));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage(
                "missingEventURLErrorMsg", name));
        
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("timestampValidationErrorMsg", name));
        }
        Date tokenDate = timeStampToken.getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                        "tokenDateErrorMsg", name, DateUtils.getStringFromDate(tokenDate),
                        DateUtils.getStringFromDate(dateInit),
                        DateUtils.getStringFromDate(dateFinish)));
        }
        return new ResponseVS(ResponseVS.SC_OK);
    }
    
    public ResponseVS validateAsManifestSignature(
        KeyStore trustedKeyStore, Date dateInit, Date dateFinish, 
        X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "signatureErrorMsg", name));
        }
        
        
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("timestampValidationErrorMsg", name));
        }
        
        getDigestToken(timeStampToken);
        Calendar signDate = pdfPKCS7.getSignDate();
        X509Certificate[] signCertificateChain = (X509Certificate[])pdfPKCS7.
                getSignCertificateChain();
        
        Object[] fails = PdfPKCS7.verifyCertificates(signCertificateChain, 
                trustedKeyStore, null, signDate);
        if(fails != null) {
            logger.error("signature fail");
            for(Object object:fails) {
                logger.error("signature fail - " + object);
            }
            for(X509Certificate cert:signCertificateChain) {
                    String notAfter = DateUtils.getStringFromDate(cert.getNotAfter());
                    String notBefore = DateUtils.getStringFromDate(cert.getNotBefore());
                    logger.debug("pdf checkSignature - fails - Cert:" + cert.getSubjectDN()
                            + " - NotBefore: " + notBefore + " - NotAfter:" + notAfter);
            }
            return new ResponseVS (ResponseVS.SC_ERROR_REQUEST);	
        }
        return new ResponseVS (ResponseVS.SC_OK);
    }

    public ResponseVS validateAsClaim(Set<X509Certificate> systemTrustedCerts, 
            String eventURL, Date dateInit, Date dateFinish, 
            X509Certificate timeStampServerCert) throws Exception {
        if(!signatureVerified) {            
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "signatureErrorMsg", name));
            
        }
        UserVS signer = smimeMessageWraper.getSigner();        
        try {
            PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
                    signer.getCertificate(), systemTrustedCerts, false);
            //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "certificateErrorMsg", signer.getNif(), name));
        } 
        
        JSONObject contentJSON = getContent(); 
        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.SMIME_CLAIM_SIGNATURE != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("operationErrorMsg",
                                TypeVS.SMIME_CLAIM_SIGNATURE.toString(),
                                operationType.toString(), name));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", name));
        
        if(contentJSON.containsKey("URL")) {
            String  documentEventURL = contentJSON.getString("URL");
            if(!eventURL.equals(documentEventURL)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("eventURLErrorMsg", name, eventURL, documentEventURL));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage(
                "missingEventURLErrorMsg", name));
        
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("timestampValidationErrorMsg", name));
        }
        Date tokenDate = timeStampToken.getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                        "tokenDateErrorMsg", name, DateUtils.getStringFromDate(tokenDate),
                        DateUtils.getStringFromDate(dateInit),
                        DateUtils.getStringFromDate(dateFinish)));
        }
        return new ResponseVS(ResponseVS.SC_OK);
    }
    
}
