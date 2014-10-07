package org.votingsystem.client.util;

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.votingsystem.client.model.SignedFile;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.DateUtils;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DocumentVSValidator {

    private static Logger logger = Logger.getLogger(DocumentVSValidator.class);

    //{"operation":"SEND_SMIME_VOTE","optionSelectedId":2,"UUID":"cfbeec4a-f87c-4e4f-b442-4b127259fbd5",
    //"optionSelectedContent":"option A","eventURL":"http://sistemavotacion.org/AccessControl/eventVSElection/1"}
    public static ResponseVS<Long> validateVote(SignedFile signedFile, Set<TrustAnchor> trustAnchors,
               Set<TrustAnchor> eventTrustedAnchors, Long optionSelectedId, String eventURL, Date dateInit,
               Date dateFinish, X509Certificate timeStampServerCert) throws Exception {
        Long signedFileOptionSelectedId = null;
        if(!signedFile.isValidSignature()) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("signatureErrorMsg",
                    signedFile.getName()));

        }
        Set<UserVS> signersVS = signedFile.getSMIMEMessageWraper().getSigners();
        for(UserVS signerVS:signersVS) {
            try {
                if(signerVS.getTimeStampToken() != null) {//user signature
                    ResponseVS validationResponse = CertUtil.verifyCertificate(eventTrustedAnchors, false, Arrays.asList(
                            signerVS.getCertificate()));
                } else {//server signature
                    ResponseVS validationResponse = CertUtil.verifyCertificate(trustAnchors, false, Arrays.asList(signerVS.getCertificate()));
                }
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                        "certificateErrorMsg", signerVS.getNif(), signedFile.getName()));
            }
        }
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert);
            signedFile.getSMIMEMessageWraper().getSigner().getTimeStampToken().validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                    getMessage("timestampValidationErrorMsg", signedFile.getName()));
        }
        Date tokenDate = signedFile.getSMIMEMessageWraper().getSigner().getTimeStampToken().
                getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "tokenDateErrorMsg", signedFile.getName(), DateUtils.getDateStr(tokenDate),
                    DateUtils.getDateStr(dateInit), DateUtils.getDateStr(dateFinish)));
        }

        JSONObject contentJSON = signedFile.getContent();
        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.SEND_SMIME_VOTE != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("operationErrorMsg", TypeVS.SEND_SMIME_VOTE.toString(),
                                operationType.toString(), signedFile.getName()));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", signedFile.getName()));
        if(contentJSON.containsKey("optionSelected")) {
            JSONObject optionSelected = contentJSON.getJSONObject("optionSelected");
            signedFileOptionSelectedId = optionSelected.getLong("id");
            if(optionSelectedId != null && signedFileOptionSelectedId != optionSelectedId) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("optionSelectedErrorMsg",
                        signedFile.getName(), optionSelectedId, signedFileOptionSelectedId));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOptionSelectedErrorMsg", signedFile.getName()));

        if(contentJSON.containsKey("eventURL")) {
            String documentEventURL = contentJSON.getString("eventURL");
            if(!eventURL.equals(documentEventURL)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("eventURLErrorMsg", signedFile.getName(), eventURL, documentEventURL));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                getMessage("jsonErrorMsg") + " - " + ContextVS.getInstance().
                getMessage("missingEventURLErrorMsg", signedFile.getName()));
        return new ResponseVS(ResponseVS.SC_OK, null, signedFileOptionSelectedId);
    }

    //{"representativeNif":"00000002W","operation":"REPRESENTATIVE_SELECTION","UUID":"dcfacb17-a323-4853-b446-8e28d8f2d0a4"}
    public static ResponseVS validateRepresentationDocument(SignedFile signedFile,
            Set<TrustAnchor> trustAnchors, Date dateFinish, String representativeNif,
            X509Certificate timeStampServerCert) throws Exception {
        if(!signedFile.isValidSignature()) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("signatureErrorMsg",
                    signedFile.getName()));
        }
        UserVS userVS = signedFile.getSMIMEMessageWraper().getSigner();
        if(representativeNif == null) return new ResponseVS(
                ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("badRequestMsg") +
                " - " + ContextVS.getInstance().getMessage("missingRepresentativeNifErrorMsg"));
        try {
            ResponseVS validationResult = CertUtil.verifyCertificate(trustAnchors, false, Arrays.asList(userVS.getCertificate()));
            //logger.debug(" - pkixResult.toString(): " + pkixResult.toString());
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "certificateErrorMsg", userVS.getNif(), signedFile.getName()));
        }
        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert);
            userVS.getTimeStampToken().validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("timestampValidationErrorMsg",
                    signedFile.getName()));
        }
        Date tokenDate = userVS.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(!tokenDate.before(dateFinish)) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("dateErrorMsg",
                    DateUtils.getDateStr(dateFinish),DateUtils.getDateStr(tokenDate),
                    signedFile.getName()));
        }
        JSONObject contentJSON = signedFile.getContent();
        if(contentJSON.containsKey("representativeNif")) {
            String jsonNIF = contentJSON.getString("representativeNif");
            if(!representativeNif.trim().equals(jsonNIF)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("nifErrorMsg", representativeNif, jsonNIF, signedFile.getName()));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingRepresentativeNifErrorMsg", signedFile.getName()));

        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.REPRESENTATIVE_SELECTION != operationType ||
                    TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("operationErrorMsg", TypeVS.REPRESENTATIVE_SELECTION.toString(),
                                operationType.toString(), signedFile.getName()));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", signedFile.getName()));

        return new ResponseVS(ResponseVS.SC_OK);
    }


    public static ResponseVS validateAccessRequest(SignedFile signedFile, Set<TrustAnchor> trustAnchors,
            String eventURL, Date dateInit, Date dateFinish, X509Certificate timeStampServerCert) throws Exception {
        if(!signedFile.isValidSignature()) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "signatureErrorMsg", signedFile.getName()));
        }
        UserVS signer = signedFile.getSMIMEMessageWraper().getSigner();
        try {
            ResponseVS validationResponse = CertUtil.verifyCertificate(trustAnchors, false, Arrays.asList(signer.getCertificate()));
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR_REQUEST, ContextVS.getInstance().getMessage(
                    "certificateErrorMsg", signer.getNif(), signedFile.getName()));
        }

        JSONObject contentJSON = signedFile.getContent();
        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.ACCESS_REQUEST != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("operationErrorMsg",
                        TypeVS.ACCESS_REQUEST.toString(), operationType.toString(), signedFile.getName()));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", signedFile.getName()));

        if(contentJSON.containsKey("eventURL")) {
            String  documentEventURL = contentJSON.getString("eventURL");
            if(!eventURL.equals(documentEventURL)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("eventURLErrorMsg", signedFile.getName(), eventURL, documentEventURL));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingEventURLErrorMsg", signedFile.getName()));

        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new
                    JcaSimpleSignerInfoVerifierBuilder().build(timeStampServerCert);
            signer.getTimeStampToken().validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                    getMessage("timestampValidationErrorMsg", signedFile.getName()));
        }
        Date tokenDate = signer.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "tokenDateErrorMsg", signedFile.getName(), DateUtils.getDateStr(tokenDate),
                    DateUtils.getDateStr(dateInit), DateUtils.getDateStr(dateFinish)));
        }
        return new ResponseVS(ResponseVS.SC_OK);
    }

    public static ResponseVS validateClaim(SignedFile signedFile, Set<TrustAnchor> trustAnchors,
            String eventURL, Date dateInit, Date dateFinish, X509Certificate timeStampServerCert) throws Exception {
        if(!signedFile.isValidSignature()) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("signatureErrorMsg",
                    signedFile.getName()));
        }
        UserVS signer = signedFile.getSMIMEMessageWraper().getSigner();
        try {
            ResponseVS validationResult = CertUtil.verifyCertificate(trustAnchors, false, Arrays.asList(signer.getCertificate()));
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "certificateErrorMsg", signer.getNif(), signedFile.getName()));
        }
        JSONObject contentJSON = signedFile.getContent();
        if(contentJSON.containsKey("operation")) {
            TypeVS operationType = TypeVS.valueOf(contentJSON.getString("operation"));
            if(TypeVS.SMIME_CLAIM_SIGNATURE != operationType) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("operationErrorMsg",
                        TypeVS.SMIME_CLAIM_SIGNATURE.toString(), operationType.toString(), signedFile.getName()));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingOperationErrorMsg", signedFile.getName()));

        if(contentJSON.containsKey("URL")) {
            String  documentEventURL = contentJSON.getString("URL");
            if(!eventURL.equals(documentEventURL)) {
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                        getMessage("eventURLErrorMsg", signedFile.getName(), eventURL, documentEventURL));
            }
        } else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage("jsonErrorMsg") +
                " - " + ContextVS.getInstance().getMessage("missingEventURLErrorMsg", signedFile.getName()));

        try {
            SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().build(
                    timeStampServerCert);
            signer.getTimeStampToken().validate(timeStampSignerInfoVerifier);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().
                    getMessage("timestampValidationErrorMsg", signedFile.getName()));
        }
        Date tokenDate = signer.getTimeStampToken().getTimeStampInfo().getGenTime();
        if(tokenDate.before(dateInit) || tokenDate.after(dateFinish)) {
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getInstance().getMessage(
                    "tokenDateErrorMsg", signedFile.getName(), DateUtils.getDateStr(tokenDate),
                    DateUtils.getDateStr(dateInit), DateUtils.getDateStr(dateFinish)));
        }
        return new ResponseVS(ResponseVS.SC_OK);
    }

}
