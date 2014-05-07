package org.votingsystem.applet.validationtool.backup;

import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.model.MetaInf;
import org.votingsystem.applet.validationtool.model.SignedFile;
import org.votingsystem.applet.validationtool.util.DocumentVSValidator;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestBackupValidator implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(ManifestBackupValidator.class);

    private ValidatorListener validatorListener = null;
    private File backupDir = null;
    private List<String> errorList = new ArrayList<String>();
    private KeyStore trustedCertsKeyStore;
    private X509Certificate timeStampServerCert;
    private MetaInf metaInf;
    private String eventURL = null;
    private String manifestFileName = null;
    
    public ManifestBackupValidator(String backupPath, ValidatorListener validatorListener) throws Exception {
        backupDir = new File(backupPath);
        this.validatorListener =  validatorListener;
    }
   
    private String checkByteArraySize (byte[] signedFileBytes) {
        //logger.debug("checkByteArraySize");
        String result = null;
        if (signedFileBytes.length > ContextVS.SIGNED_MAX_FILE_SIZE) {
            result = ContextVS.getInstance().getMessage("fileSizeExceededMsg", ContextVS.SIGNED_MAX_FILE_SIZE_KB,
                    signedFileBytes.length);
        }
        return result;
    }
    
    @Override public ResponseVS call() throws Exception {
        long begin = System.currentTimeMillis();
        manifestFileName = ContextVS.getMessage("manifestFileName");

        String backupPath = backupDir.getAbsolutePath();
        File trustedCertsFile = new File(backupPath + File.separator + "systemTrustedCerts.pem");
        Collection<X509Certificate> trustedCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        trustedCertsKeyStore = KeyStore.getInstance("JKS");
        trustedCertsKeyStore.load(null, null);
        for(X509Certificate certificate:trustedCerts) {
            trustedCertsKeyStore.setCertificateEntry(certificate.getSubjectDN().toString(), certificate);
        }
        File timeStampCertFile = new File(backupPath + File.separator +"timeStampCert.pem");
        Collection<X509Certificate> timeStampCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(timeStampCertFile));   
        timeStampServerCert = timeStampCerts.iterator().next();
            
        File metaInfFile = new File(backupPath + File.separator + "meta.inf");
        if(!metaInfFile.exists()) {
            String message = ContextVS.getMessage("metaInfFileNotFoundErrorMsg", metaInfFile.getAbsolutePath());
            logger.error(message);
            return new ResponseVS(ResponseVS.SC_ERROR, message);
        } else {
            metaInf = MetaInf.parse(FileUtils.getStringFromFile(metaInfFile));
            eventURL= EventVS.getURL(null, metaInf.getServerURL(), metaInf.getId());
        }
        ResponseVS responseVS = validateManifests();
        Integer statusCode = errorList.size() > 0? ResponseVS.SC_ERROR:ResponseVS.SC_OK;
        responseVS.setErrorList(errorList);
        responseVS.setStatusCode(statusCode);
        if(!errorList.isEmpty()) {
            logger.error(" ------- " + errorList.size() + " errors: ");
            String errorsMsg = StringUtils.getFormattedErrorList(errorList);
            logger.error(errorsMsg);
        }
        long finish = System.currentTimeMillis();
        long duration = finish - begin;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        logger.debug("duration: " + durationStr);
        notifyValidationListener(responseVS.getStatusCode(), durationStr, ValidationEvent.MANIFEST_FINISIH);
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
            validatorListener.processValidationEvent(response);
        }
    }
    
    private ResponseVS validateManifests() throws Exception {
        logger.debug("validateManifests");
        File[] batchDirs = backupDir.listFiles();
        int statusCode = ResponseVS.SC_OK;
        int numManifestOK = 0;
        int numManifestERROR = 0;
        Map<String, String> signersNifMap = new HashMap<String, String>();
        for(File batchDir:batchDirs) {
            if(batchDir.isDirectory()) {
                File[] manifests = batchDir.listFiles();
                for(File manifest : manifests) {
                    if(!manifest.getAbsolutePath().contains(manifestFileName)) continue;
                    String errorMessage = null;
                    byte[] accessRequestBytes = FileUtils.getBytesFromFile(manifest);
                    SignedFile signedFile = new SignedFile(accessRequestBytes, manifest.getName());
                    ResponseVS validationResponse = DocumentVSValidator.validateManifestSignature(signedFile,
                            trustedCertsKeyStore, metaInf.getDateInit(),  metaInf.getDateFinish(), timeStampServerCert);
                    statusCode = validationResponse.getStatusCode();
                    if(ResponseVS.SC_OK == validationResponse.getStatusCode()) {
                        boolean repeatedSignature = signersNifMap.containsKey(signedFile.getSignerNif());
                        if(repeatedSignature) {
                            numManifestERROR++;
                            errorMessage = ContextVS.getInstance().getMessage("claimSignatureRepeatedErrorMsg",
                                    signedFile.getSignerNif()) + " - " + manifest.getAbsolutePath() + " - " +
                                    signersNifMap.get(signedFile.getSignerNif());
                        } else {
                            numManifestOK++;
                            signersNifMap.put(signedFile.getSignerNif(), manifest.getAbsolutePath());
                        }
                    } else numManifestERROR++;
                    if(errorMessage != null) {
                        statusCode = ResponseVS.SC_ERROR;
                        logger.error(errorMessage);
                        errorList.add(errorMessage);
                    } 
                    notifyValidationListener(statusCode, validationResponse.getMessage(),ValidationEvent.MANIFEST);
                }
            }
        }
        logger.debug("numManifestOK: " + numManifestOK + " - numManifestERROR: " + numManifestERROR);
        ResponseVS result = null;
        if(metaInf.getNumSignatures() != numManifestOK) {
            result = new ResponseVS(ResponseVS.SC_ERROR,
                    ContextVS.getMessage("numSignaturesErrorMsg", metaInf.getNumSignatures(), numManifestOK));
        } else {
            result = new ResponseVS(ResponseVS.SC_OK, ContextVS.getMessage(
                    "signaturesValidationResultMsg", numManifestOK));
        }
        notifyValidationListener(result.getStatusCode(), result.getMessage(),ValidationEvent.MANIFEST_FINISIH);
        return result;
    }
    
    public static void main(String[] args) throws Exception { }

}