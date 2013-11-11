package org.votingsystem.applet.validationtool.backup;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.votingsystem.applet.validationtool.ValidationToolContext;
import org.votingsystem.applet.validationtool.model.MetaInf;
import org.votingsystem.applet.validationtool.model.SignedFile;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ClaimBackupValidator implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(ClaimBackupValidator.class);

    private ValidatorListener validatorListener = null;
    private File backupDir = null;
    private List<String> errorList = new ArrayList<String>();
    private Set<X509Certificate> systemTrustedCerts;
    private X509Certificate timeStampServerCert;
    private MetaInf metaInf;
    private String eventURL = null;
    private String claimFileName = null;
    
    public ClaimBackupValidator(String backupPath, 
            ValidatorListener validatorListener) throws Exception {
        ValidationToolContext.init(null, "log4jValidationTool.properties", 
            "validationToolMessages_", "es");
        backupDir = new File(backupPath);
        this.validatorListener =  validatorListener;
    }
   
    private String checkByteArraySize (byte[] signedFileBytes) {
        //logger.debug("checkByteArraySize");
        String result = null;
        if (signedFileBytes.length > ContextVS.SIGNED_MAX_FILE_SIZE) {
            result = ContextVS.INSTANCE.getString("fileSizeExceededMsg", 
                        ContextVS.SIGNED_MAX_FILE_SIZE_KB, signedFileBytes.length);
        }
        return result;
    }
    
    @Override public ResponseVS call() throws Exception {
        long begin = System.currentTimeMillis();
        claimFileName = ContextVS.INSTANCE.
                getString("claimFileName");

        String backupPath = backupDir.getAbsolutePath();
        File trustedCertsFile = new File(backupPath + File.separator + 
                "systemTrustedCerts.pem");
        Collection<X509Certificate> trustedCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        systemTrustedCerts = new HashSet(trustedCerts);

        File timeStampCertFile = new File(backupPath + File.separator + 
                "timeStampCert.pem");
        Collection<X509Certificate> timeStampCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(timeStampCertFile));   
        timeStampServerCert = timeStampCerts.iterator().next();
            
        File metaInfFile = new File(backupPath + File.separator + 
                "meta.inf");
        if(!metaInfFile.exists()) {
            logger.error(" - metaInfFile: " + metaInfFile.getAbsolutePath() + " not found");
        } else {
            metaInf = MetaInf.parse(FileUtils.getStringFromFile(metaInfFile));
            String serverURL = metaInf.getServerURL();
            if (!serverURL.endsWith("/")) serverURL = serverURL + "/";
            eventURL= serverURL + "evento/" + metaInf.getId();
        }

        ResponseVS responseVS = validateClaims();


        Integer statusCode = errorList.size() > 0? ResponseVS.SC_ERROR:ResponseVS.SC_OK;
        responseVS.setErrorList(errorList);
        responseVS.setStatusCode(statusCode);
        if(!errorList.isEmpty()) {
            logger.error(" ------- " + errorList.size() + " errors: ");
            for(String error : errorList) {
                logger.error(error);
            }
        } else logger.debug("Backup without errors");
        long finish = System.currentTimeMillis();
        long duration = finish - begin;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        logger.debug("duration: " + durationStr);
        notifyValidationListener(responseVS.getStatusCode(), 
                durationStr, ValidationEvent.CLAIM_FINISH);
        
        responseVS.setMessage(durationStr);
        responseVS.setData(metaInf);
        return responseVS;
    }
    
    private void notifyValidationListener(Integer statusCode, 
            String message, ValidationEvent validationEvent) {
        if(validatorListener != null) {
            ResponseVS response = new ResponseVS(statusCode, message);
            response.setData(validationEvent);
            response.setErrorList(errorList);
            validatorListener.
                processValidationEvent(response);
        }
    }
    
    private ResponseVS validateClaims() throws Exception {
        logger.debug("validateClaims");
        File[] batchDirs = backupDir.listFiles();
        int statusCode = ResponseVS.SC_OK;
        int numClaimsOK = 0;
        int numClaimsERROR = 0;
        Map<String, String> signersNifMap = new HashMap<String, String>();
        for(File batchDir:batchDirs) {
            if(batchDir.isDirectory()) {
                File[] claims = batchDir.listFiles();
                for(File claim : claims) {
                    if(!claim.getAbsolutePath().contains(claimFileName)) {
                        continue;
                    }
                    String errorMessage = null;
                    byte[] accessRequestBytes = FileUtils.
                            getBytesFromFile(claim);
                    SignedFile signedFile = new SignedFile(
                            accessRequestBytes, claim.getName());
                    ResponseVS validationResponse = signedFile.validateAsClaim(
                            systemTrustedCerts, eventURL, metaInf.getDateInit(), 
                            metaInf.getDateFinish(), timeStampServerCert);
                    statusCode = validationResponse.getStatusCode();
                    if(ResponseVS.SC_OK == validationResponse.getStatusCode()) {
                        boolean repeatedSignature = signersNifMap.containsKey(
                                signedFile.getSignerNif());
                        if(repeatedSignature) {
                            numClaimsERROR++;
                            errorMessage = ContextVS.INSTANCE.getString(
                                    "claimSignatureRepeatedErrorMsg", 
                                    signedFile.getSignerNif()) + " - " + 
                                    claim.getAbsolutePath() + " - " + 
                                    signersNifMap.get(signedFile.getSignerNif());
                        } else {
                            numClaimsOK++;
                            signersNifMap.put(signedFile.getSignerNif(), 
                                    claim.getAbsolutePath());
                        } 
                    } else {
                        numClaimsERROR++;
                        errorMessage = "ERROR CLAIM SIGNATURE - File: " + 
                                claim.getAbsolutePath() + " - msg: " +
                                validationResponse.getMessage();
                    }
                    if(errorMessage != null) {
                        statusCode = ResponseVS.SC_ERROR;
                        logger.error(errorMessage);
                        errorList.add(errorMessage);
                    } 
                    notifyValidationListener(statusCode, 
                            validationResponse.getMessage(), 
                            ValidationEvent.ACCESS_REQUEST);
                }
            }
        }
        statusCode = ResponseVS.SC_OK;
        logger.debug("numClaimsOK: " + numClaimsOK + 
                " - numClaimsERROR: " + numClaimsERROR);
        String message = null;
        if(metaInf.getNumSignatures() != numClaimsOK) {
            statusCode = ResponseVS.SC_ERROR;
            message = ContextVS.INSTANCE.getString("numClaimSignaturesErrorMsg", 
                    metaInf.getNumAccessRequest(), numClaimsOK);
        } else message =  ContextVS.INSTANCE.getString(
                "claimsValidationResultMsg", metaInf.getNumAccessRequest());
        return new ResponseVS(statusCode, message);
    }
    
    public static void main(String[] args) throws Exception {
        
        ClaimBackupValidator dirBackupValidator = new ClaimBackupValidator(
                "./Descargas/ReclamacionesEvento_5", null);
        dirBackupValidator.call();

        System.exit(0);
    }

}