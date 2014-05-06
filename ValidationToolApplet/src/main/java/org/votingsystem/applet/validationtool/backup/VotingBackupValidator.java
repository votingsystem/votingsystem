package org.votingsystem.applet.validationtool.backup;

import org.apache.log4j.Logger;
import org.votingsystem.applet.validationtool.model.MetaInf;
import org.votingsystem.applet.validationtool.model.RepresentativeData;
import org.votingsystem.applet.validationtool.model.RepresentativesData;
import org.votingsystem.applet.validationtool.model.SignedFile;
import org.votingsystem.applet.validationtool.util.DocumentVSValidator;
import org.votingsystem.model.*;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingBackupValidator implements Callable<ResponseVS>, AppHostVS {
    
    private static Logger logger = Logger.getLogger(VotingBackupValidator.class);

    private ValidatorListener validatorListener = null;
    private File backupDir = null;
    private List<String> errorList = new ArrayList<String>();
    private Set<X509Certificate> systemTrustedCerts;
    private Set<X509Certificate> eventTrustedCertsSet;
    private X509Certificate timeStampServerCert;
    private MetaInf metaInf;
    private final File accessRequestsDir; 
    private final File votesDir; 
    private String eventURL = null;
    private String representativeVoteFileName = null;
    private String accessRequestFileName = null;
    private String representativeReportFileName = null;
    private Map<Long, Long> optionsMap = new HashMap<Long, Long>();
    private Map<String, Long> representativeVotesMap = new HashMap<String, Long>();
    
    public VotingBackupValidator(String backupPath, ValidatorListener validatorListener) throws Exception {
        if(ContextVS.getInstance() == null) {
            ContextVS.init(null, "log4jValidationTool.properties", "validationToolMessages.properties", "es");
        }
        backupDir = new File(backupPath);
        this.validatorListener =  validatorListener;
        accessRequestsDir = new File(backupPath + "/accessRequest");
        votesDir = new File(backupPath + "/votes");
    }
   
    private String checkByteArraySize (byte[] signedFileBytes) {
        //logger.debug("checkByteArraySize");
        String result = null;
        if (signedFileBytes.length > ContextVS.SIGNED_MAX_FILE_SIZE) {
            result = ContextVS.getInstance().getMessage("fileSizeExceededMsg",
                    ContextVS.SIGNED_MAX_FILE_SIZE_KB, signedFileBytes.length);
        }
        return result;
    }
    
    @Override public ResponseVS call() throws Exception {
        long begin = System.currentTimeMillis();
        representativeReportFileName = ContextVS.getMessage("representativeReportFileName");
        accessRequestFileName = ContextVS.getMessage("accessRequestFileName");
        representativeVoteFileName = ContextVS.getMessage("representativeVoteFileName");

        String backupPath = backupDir.getAbsolutePath();
        File trustedCertsFile = new File(backupPath + File.separator + "systemTrustedCerts.pem");
        Collection<X509Certificate> trustedCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        systemTrustedCerts = new HashSet(trustedCerts);
        File eventTrustedCertsFile = new File(backupPath + File.separator + "eventTrustedCerts.pem");
        Collection<X509Certificate> eventTrustedCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(eventTrustedCertsFile));        
        eventTrustedCertsSet = new HashSet(eventTrustedCerts);
        File timeStampCertFile = new File(backupPath + File.separator + "timeStampCert.pem");
        Collection<X509Certificate> timeStampCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(timeStampCertFile));   
        timeStampServerCert = timeStampCerts.iterator().next();
            
        File metaInfFile = new File(backupPath + File.separator + "meta.inf");
        if(!metaInfFile.exists()) {
            logger.error(" - metaInfFile: " + metaInfFile.getAbsolutePath() + " not found");
        } else {
            metaInf = MetaInf.parse(FileUtils.getStringFromFile(metaInfFile));
            eventURL= EventVS.getURL(TypeVS.VOTING_EVENT, metaInf.getServerURL(), metaInf.getId());
        }
        for(FieldEventVS option : metaInf.getOptionList()) {
            optionsMap.put(option.getId(), new Long(0));
        }
        
        ResponseVS responseVS = validateRepresentativeData();
        notifyValidationListener(responseVS.getStatusCode(), responseVS.getMessage(),
                ValidationEvent.REPRESENTATIVE_FINISH);
        String representativeValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        responseVS = validateAccessRequests();
        notifyValidationListener(responseVS.getStatusCode(), 
                responseVS.getMessage(), ValidationEvent.ACCESS_REQUEST_FINISH);
        String accessRequestValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        responseVS = validateVotes();
        String votesValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        notifyValidationListener(responseVS.getStatusCode(),votesValidationDurationStr, ValidationEvent.VOTE_FINISH);
        
        logger.debug("representativeValidation duration: " + representativeValidationDurationStr);
        logger.debug("accessrequestValidation duration: " + accessRequestValidationDurationStr);
        logger.debug("votesValidationDurationStr duration: " + votesValidationDurationStr);

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
        responseVS.setMessage(durationStr);
        responseVS.setData(metaInf);
        return responseVS;
    }
    
    private void notifyValidationListener(Integer statusCode,  String message, ValidationEvent validationEvent) {
        if(validatorListener != null) {
            ResponseVS response = new ResponseVS(statusCode, message);
            response.setData(validationEvent);
            response.setErrorList(errorList);
            validatorListener.processValidationEvent(response);
        }
    }
    
    private ResponseVS validateRepresentativeData() throws Exception {
        logger.debug("validateRepresentativeData");
        long numRepresentatives = 0;
        long numRepresented = 0;
        long numRepresentativesWithVote = 0;
        long numRepresentedWithAccessRequest = 0;
        long numVotesRepresentedByRepresentatives = 0;
        
        String repAccreditationsBackupPath = backupDir.getAbsolutePath() + 
            File.separator + ContextVS.getInstance().getMessage("repAccreditationsBackupPartPath");
        File representativesDir = new File(repAccreditationsBackupPath);
        File[] representativesDirs = representativesDir.listFiles();
        RepresentativesData representativesData = metaInf.getRepresentativesData();
        for(File file:representativesDirs) {
            logger.debug("checking representative dir.:" + file.getAbsolutePath());
            numRepresentatives++;
            long numRepresentedWithVote = 0;
            long numRepresentations = 1;//the representative itself
            long numVotesRepresented = 0;
            if(file.isDirectory()) {
                File[] representativesBatchDirs = file.listFiles();
                String representativeNif = file.getAbsolutePath().split("_representative_")[1];
                RepresentativeData representativeDataMetaInf = metaInf.getRepresentativeData(representativeNif);
                logger.debug("representativeNif: " + representativeNif + 
                        " - vote: " + representativeDataMetaInf.getOptionSelectedId());
                for(File batchDir : representativesBatchDirs) {
                    logger.debug("checking dir: " + batchDir.getAbsolutePath());
                    File[] repDocs = batchDir.listFiles();
                    for(File repDoc: repDocs) {
                        numRepresented++;
                        numRepresentations++;
                        byte[] fileBytes = FileUtils.getBytesFromFile(repDoc);
                        SignedFile signedFile = new SignedFile(fileBytes, repDoc.getName());
                        if(repDoc.getAbsolutePath().contains("_WithRequest_RepDoc_")) {
                            List<File> result = FileUtils.findRecursively(
                                    accessRequestsDir, signedFile.getNifFromRepresented());
                            if(!result.isEmpty()) {
                                File resultFile = result.iterator().next();
                                numRepresentedWithAccessRequest++;
                                numRepresentedWithVote++;
                            } else {
                                String errorMsg = ContextVS.getInstance().getMessage("representedAccessRequestNotFound",
                                        signedFile.getNifFromRepresented(), representativeNif);
                                errorList.add(errorMsg);
                            }
                        }
                        ResponseVS responseVS = DocumentVSValidator.validateRepresentationDocument(signedFile,
                                systemTrustedCerts, metaInf.getDateFinish(), representativeNif, timeStampServerCert);
                        logger.debug("responseVS.getStatusCode(): " +  responseVS.getStatusCode());
                        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                            errorList.add(responseVS.getMessage());
                        }
                        notifyValidationListener(responseVS.getStatusCode(), 
                                responseVS.getMessage(), ValidationEvent.REPRESENTATIVE);
                    }
                }
                if(representativeDataMetaInf.getOptionSelectedId() != null) {
                    List<File> result = FileUtils.findRecursively(votesDir, "_" + representativeNif);
                    if(!result.isEmpty()) {
                        File voteFile = result.iterator().next();
                        byte[] fileBytes = FileUtils.getBytesFromFile(voteFile);
                        SignedFile vote = new SignedFile(fileBytes, voteFile.getName());
                        ResponseVS representativeVoteResponse = DocumentVSValidator.validateVote(vote,
                                systemTrustedCerts, eventTrustedCertsSet,  representativeDataMetaInf.
                                getOptionSelectedId(), eventURL, metaInf.getDateInit(), metaInf.getDateFinish(),
                                timeStampServerCert);
                        if (ResponseVS.SC_OK != representativeVoteResponse.getStatusCode()) {
                            errorList.add(representativeVoteResponse.getMessage());
                        } else {
                            numRepresentativesWithVote++;    
                            numVotesRepresented = numRepresentations - numRepresentedWithVote;
                            numVotesRepresentedByRepresentatives += numVotesRepresented;
                        } 
                    } else {
                        String errorMsg = ContextVS.getInstance().getMessage(
                                "representativeVoteNotFound", representativeNif);
                        errorList.add(errorMsg);
                    }
                }        
                String msg = "NIF: " + representativeNif + " - numRepresentations: " + numRepresentations +
                        " - numRepresentedWithVote: " + numRepresentedWithVote +
                        " - numVotesRepresented: " + numVotesRepresented;
                logger.debug(msg);
                if(representativeDataMetaInf.getNumRepresentations() != numRepresentations ||
                    representativeDataMetaInf.getNumVotesRepresented() != numVotesRepresented ||
                    representativeDataMetaInf.getNumRepresentedWithVote() != numRepresentedWithVote) {           
                    String errorMsg = ContextVS.getMessage("representativeDataErrorMsg",representativeNif,
                            representativeDataMetaInf.getString(), msg);
                    errorList.add(errorMsg);
                } else representativeVotesMap.put(representativeNif, numVotesRepresented);
            }
        }
        int statusCode = ResponseVS.SC_OK;
        numRepresented += numRepresentatives;
        String message = "numRepresentatives: " + numRepresentatives + " - numRepresented: " + numRepresented +
                " - numRepresentativesWithVote: " + numRepresentativesWithVote + 
                " - numRepresentedWithAccessRequest: " +  numRepresentedWithAccessRequest +
                " - numVotesRepresentedByRepresentatives: " + numVotesRepresentedByRepresentatives;
        if(representativesData.getNumRepresentatives() != numRepresentatives ||
                representativesData.getNumRepresentativesWithVote() != numRepresentativesWithVote ||
                representativesData.getNumRepresentedWithAccessRequest() != numRepresentedWithAccessRequest ||
                representativesData.getNumRepresented() != numRepresented ||
                representativesData.getNumVotesRepresentedByRepresentatives() 
                != numVotesRepresentedByRepresentatives) {
            statusCode = ResponseVS.SC_ERROR;
            message = ContextVS.getMessage("representativesDataErrorMsg", representativesData.getString(), message);
            errorList.add(message);
        }
        logger.debug(message);
        return new ResponseVS(statusCode, message);
    }
    
    private ResponseVS validateAccessRequests() throws Exception {
        logger.debug("validateAccessRequests");
        File[] batchDirs = accessRequestsDir.listFiles();
        int statusCode = ResponseVS.SC_OK;
        int numAccessRequestOK = 0;
        int numAccessRequestERROR = 0;
        Map<String, String> signersNifMap = new HashMap<String, String>();
        for(File batchDir:batchDirs) {
            if(batchDir.isDirectory()) {
                File[] accessRequests = batchDir.listFiles();
                for(File accessRequest : accessRequests) {
                    String errorMessage = null;
                    byte[] accessRequestBytes = FileUtils.getBytesFromFile(accessRequest);
                    SignedFile signedFile = new SignedFile(accessRequestBytes, accessRequest.getName());
                    ResponseVS validationResponse = DocumentVSValidator.validateAccessRequest(signedFile,
                            systemTrustedCerts, eventURL, metaInf.getDateInit(), 
                            metaInf.getDateFinish(), timeStampServerCert);
                    statusCode = validationResponse.getStatusCode();
                    if(ResponseVS.SC_OK == validationResponse.getStatusCode()) {
                        boolean repeatedAccessrequest = signersNifMap.containsKey(signedFile.getSignerNif());
                        if(repeatedAccessrequest) {
                            numAccessRequestERROR++;
                            errorMessage = ContextVS.getInstance().getMessage("accessRequetsRepeatedErrorMsg",
                                    signedFile.getSignerNif()) + " - " + accessRequest.getAbsolutePath() + " - " +
                                    signersNifMap.get(signedFile.getSignerNif());
                        } else {
                            numAccessRequestOK++;
                            signersNifMap.put(signedFile.getSignerNif(), accessRequest.getAbsolutePath());
                        } 
                    } else {
                        numAccessRequestERROR++;
                        errorMessage = "ERROR ACCES REQUEST - File: " + accessRequest.getAbsolutePath() + " - msg: " +
                                validationResponse.getMessage();
                    }
                    if(errorMessage != null) {
                        statusCode = ResponseVS.SC_ERROR;
                        logger.error(errorMessage);
                        errorList.add(errorMessage);
                    } 
                    notifyValidationListener(statusCode, validationResponse.getMessage(),
                            ValidationEvent.ACCESS_REQUEST);
                }
            }
        }
        statusCode = ResponseVS.SC_OK;
        logger.debug("numAccessRequestOK: " + numAccessRequestOK + " - numAccessRequestERROR: " +numAccessRequestERROR);
        String message = null;
        if(metaInf.getNumAccessRequest() != numAccessRequestOK) {
            statusCode = ResponseVS.SC_ERROR;
            message = ContextVS.getInstance().getMessage("numAccessRequestErrorMsg", metaInf.getNumAccessRequest(),
                    numAccessRequestOK);
        } else message =  ContextVS.getInstance().getMessage("accessRequestValidationResultMsg",
                metaInf.getNumAccessRequest());
        return new ResponseVS(statusCode, message);
    }
    
    private ResponseVS validateVotes() throws Exception {
        logger.debug("validateVotes");
        int statusCode = ResponseVS.SC_OK;
        File[] batchDirs = votesDir.listFiles();
        int numVotesOK = 0;
        int numVotesERROR = 0;
        int numRepresentativeVotes = 0;
        Map<Long, String> signerCertMap = new HashMap<Long, String>();
        for(File batchDir:batchDirs) {
            if(batchDir.isDirectory()) {
                File[] votes = batchDir.listFiles();
                for(File vote : votes) {
                    byte[] voteBytes = FileUtils.getBytesFromFile(vote);
                    SignedFile signedFile = new SignedFile(voteBytes, vote.getName());
                    ResponseVS<Long> validationResponse = DocumentVSValidator.validateVote(signedFile,
                            systemTrustedCerts, eventTrustedCertsSet, null, eventURL,
                            metaInf.getDateInit(), metaInf.getDateFinish(), timeStampServerCert);
                    statusCode = validationResponse.getStatusCode();
                    if(ResponseVS.SC_OK == validationResponse.getStatusCode()) {
                        boolean repeatedVote = signerCertMap.containsKey(signedFile.getSignerCertSerialNumber());
                        if(repeatedVote){
                            numVotesERROR++;
                            statusCode = ResponseVS.SC_ERROR;
                            String msg = ContextVS.getInstance().getMessage(
                                    "voteRepeatedErrorMsg", signedFile.getSignerCertSerialNumber()) + " - " +
                                    vote.getAbsolutePath() + " - " +
                                    signerCertMap.get(signedFile.getSignerCertSerialNumber());
                            errorList.add(msg);
                        } else {
                            numVotesOK++;
                            Long optionSelectedId = validationResponse.getData();
                            signerCertMap.put(signedFile.getSignerCertSerialNumber(), vote.getAbsolutePath());
                            if(vote.getAbsolutePath().contains(representativeVoteFileName)) {
                                String representativeNIF = vote.getName().split("_")[1].split("\\.")[0];
                                numRepresentativeVotes++;
                                optionsMap.put(optionSelectedId, optionsMap.get(optionSelectedId) +
                                        representativeVotesMap.get(representativeNIF));
                            } else optionsMap.put(optionSelectedId, optionsMap.get(optionSelectedId) + 1);
                        }
                    } else {
                        numVotesERROR++;
                        String msg = "ERROR ACCES REQUEST - File: " + vote.getAbsolutePath() + " - msg: " +
                                validationResponse.getMessage();
                        logger.error(msg);
                        errorList.add(msg);
                    }
                    notifyValidationListener(statusCode, validationResponse.getMessage(), ValidationEvent.VOTE);
                }
            }
        }
        statusCode = ResponseVS.SC_OK;
        logger.debug("numVotesOK: " + numVotesOK +  " - numVotesERROR: " + numVotesERROR);
        if(numVotesERROR > 0) statusCode = ResponseVS.SC_ERROR;
        if(numVotesOK != metaInf.getNumVotes()) {
            statusCode = ResponseVS.SC_ERROR;
            errorList.add(ContextVS.getMessage("numVotesResultErrorMsg", metaInf.getNumVotes(), numVotesOK));
        }
        for(FieldEventVS option : metaInf.getOptionList()) {
            if(option.getNumVotesResult() != optionsMap.get(option.getId())) {
                statusCode = ResponseVS.SC_ERROR;
                errorList.add(ContextVS.getMessage("numVotesOptionErrorMsg", option.getContent(),
                        option.getNumVotesResult(), optionsMap.get(option.getId())));
            }
        }

        if(metaInf.getRepresentativesData().getNumRepresentativesWithVote() != numRepresentativeVotes) {
            statusCode = ResponseVS.SC_ERROR;
            errorList.add(ContextVS.getInstance().getMessage("numRepresentativesVotesErrorMsg",
                    metaInf.getRepresentativesData().getNumRepresentativesWithVote(), numRepresentativeVotes));
        }
        return new ResponseVS(statusCode);
    }
    
    public static void main(String[] args) throws Exception {
        VotingBackupValidator dirBackupValidator = new VotingBackupValidator("./Descargas/VotosEvento_4", null);
        dirBackupValidator.call();
        System.exit(0);
    }

    @Override public void sendMessageToHost(OperationVS operation) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

}