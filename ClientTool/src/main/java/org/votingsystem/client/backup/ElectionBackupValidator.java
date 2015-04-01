package org.votingsystem.client.backup;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.client.model.MetaInf;
import org.votingsystem.client.model.RepresentativeData;
import org.votingsystem.client.model.RepresentativesData;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.DocumentVSValidator;
import org.votingsystem.signature.util.SignedFile;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.TypeVS;

import java.io.File;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ElectionBackupValidator implements BackupValidator<ResponseVS> {
    
    private static Logger log = Logger.getLogger(ElectionBackupValidator.class.getSimpleName());

    private ValidatorListener validatorListener = null;
    private File backupDir = null;
    private List<String> errorList = new ArrayList<String>();
    private Set<TrustAnchor> trustAnchors;
    private Set<TrustAnchor> eventTrustedAnchors;
    private X509Certificate timeStampServerCert;
    private MetaInf metaInf;
    private final File accessRequestsDir; 
    private final File votesDir; 
    private String eventURL = null;
    private String representativeVoteFileName = "representativeVote_";
    private String accessRequestFileName = "accessRequest_";
    private Map<Long, Long> optionsMap = new HashMap<Long, Long>();
    private Map<String, Long> representativeVotesMap = new HashMap<String, Long>();
    private AtomicBoolean isCanceled = new AtomicBoolean(false);
    
    public ElectionBackupValidator(String backupPath, ValidatorListener validatorListener) throws Exception {
        if(ContextVS.getInstance() == null) {
            ContextVS.initSignatureClient("clientToolMessages.properties", "es");
        }
        backupDir = new File(backupPath);
        this.validatorListener =  validatorListener;
        accessRequestsDir = new File(backupPath + File.separator + TypeVS.VOTING_EVENT.toString() + "/accessRequest");
        votesDir = new File(backupPath + File.separator + TypeVS.VOTING_EVENT.toString() + "/votes");
    }
   
    private String checkByteArraySize (byte[] signedFileBytes) {
        //log.info("checkByteArraySize");
        String result = null;
        if (signedFileBytes.length > ContextVS.SIGNED_MAX_FILE_SIZE) {
            result = ContextVS.getInstance().getMessage("fileSizeExceededMsg",
                    ContextVS.SIGNED_MAX_FILE_SIZE_KB, signedFileBytes.length);
        }
        return result;
    }

    public void cancel() {
        isCanceled.set(true);
    }

    @Override public ResponseVS call() throws Exception {
        long begin = System.currentTimeMillis();
        String backupPath = backupDir.getAbsolutePath();
        String voteInfoPath = backupPath + File.separator + TypeVS.VOTING_EVENT.toString() + File.separator;
        File trustedCertsFile = new File(voteInfoPath + "systemTrustedCerts.pem");
        Collection<X509Certificate> trustedCerts = CertUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        trustAnchors = new HashSet<TrustAnchor>(trustedCerts.size());
        for(X509Certificate certificate: trustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            trustAnchors.add(anchor);
        }
        File eventTrustedCertsFile = new File(voteInfoPath + "eventTrustedCerts.pem");
        Collection<X509Certificate> eventTrustedCerts = CertUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(eventTrustedCertsFile));
        eventTrustedAnchors = new HashSet<TrustAnchor>(eventTrustedCerts.size());
        for(X509Certificate certificate: eventTrustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            eventTrustedAnchors.add(anchor);
        }
        File timeStampCertFile = new File(voteInfoPath + "timeStampCert.pem");
        Collection<X509Certificate> timeStampCerts = CertUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(timeStampCertFile));   
        timeStampServerCert = timeStampCerts.iterator().next();
            
        File metaInfFile = new File(backupPath + File.separator + "meta.inf");
        File representativeMetaInfFile = new File(backupPath + File.separator + TypeVS.REPRESENTATIVE_DATA.toString()  +
                File.separator + "meta.inf");
        if(!metaInfFile.exists()) {
            log.log(Level.SEVERE, " - metaInfFile: " + metaInfFile.getAbsolutePath() + " not found");
        } else {
            Map<String, Object> metaInfMap = new ObjectMapper().readValue(
                    metaInfFile, new TypeReference<HashMap<String, Object>>() {});
            metaInf = MetaInf.parse(metaInfMap);
            Map<String, Object> representativeDataMap = new ObjectMapper().readValue(
                    representativeMetaInfFile, new TypeReference<HashMap<String, Object>>() {});
            metaInf.loadRepresentativeData(representativeDataMap);
            eventURL= EventVS.getURL(TypeVS.VOTING_EVENT, metaInf.getServerURL(), metaInf.getId());
        }
        for(FieldEventVS option : metaInf.getOptionList()) {
            optionsMap.put(option.getId(), 0L);
        }
        
        ResponseVS responseVS = validateRepresentativeData();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        notifyValidationListener(responseVS.getStatusCode(), responseVS.getMessage(),
                ValidationEvent.REPRESENTATIVE_FINISH);
        String representativeValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        responseVS = validateAccessRequests();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        notifyValidationListener(responseVS.getStatusCode(), 
                responseVS.getMessage(), ValidationEvent.ACCESS_REQUEST_FINISH);
        String accessRequestValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        responseVS = validateVotes();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        String votesValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        notifyValidationListener(responseVS.getStatusCode(),votesValidationDurationStr, ValidationEvent.VOTE_FINISH);
        
        log.info("representativeValidation duration: " + representativeValidationDurationStr);
        log.info("accessrequestValidation duration: " + accessRequestValidationDurationStr);
        log.info("votesValidationDurationStr duration: " + votesValidationDurationStr);

        Integer statusCode = errorList.size() > 0? ResponseVS.SC_ERROR:ResponseVS.SC_OK;
        responseVS.setErrorList(errorList);
        responseVS.setStatusCode(statusCode);
        if(!errorList.isEmpty()) {
            log.log(Level.SEVERE, " ------- " + errorList.size() + " errors: ");
            for(String error : errorList) {
                log.log(Level.SEVERE, error);
            }
        } else log.info("Backup without errors");
        long finish = System.currentTimeMillis();
        long duration = finish - begin;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        log.info("duration: " + durationStr);
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
        log.info("validateRepresentativeData");
        long numRepresentatives = 0;
        long numRepresented = 0;
        long numRepresentativesWithVote = 0;
        long numRepresentedWithAccessRequest = 0;
        long numVotesRepresentedByRepresentatives = 0;
        String repAccreditationsBackupPath = backupDir.getAbsolutePath() + File.separator +
                TypeVS.REPRESENTATIVE_DATA.toString();
        File representativeDataDir = new File(repAccreditationsBackupPath);
        File[] representativeDirList = representativeDataDir.listFiles();
        RepresentativesData representativesData = metaInf.getRepresentativesData();
        for(File file : representativeDirList) {
            log.info("checking representative dir:" + file.getAbsolutePath());
            long numRepresentedWithVote = 0;
            long numRepresentations = 1;//the representative itself
            long numVotesRepresented = 0;
            if(file.isDirectory()) {
                numRepresentatives++;
                File[] batchDirList = file.listFiles();
                String representativeNif = file.getAbsolutePath().split("representative_")[1];
                RepresentativeData representativeDataMetaInf = metaInf.getRepresentativeData(representativeNif);
                log.info("representativeNif: " + representativeNif +
                        " - vote: " + representativeDataMetaInf.getOptionSelectedId());
                for(File batchDir : batchDirList) {
                    log.info("checking dir: " + batchDir.getAbsolutePath());
                    File[] repDocs = batchDir.listFiles();
                    for(File repDoc: repDocs) {
                        if(isCanceled.get()) return new ResponseVS(ResponseVS.SC_CANCELED);
                        numRepresented++;
                        numRepresentations++;
                        byte[] fileBytes = FileUtils.getBytesFromFile(repDoc);
                        SignedFile signedFile = new SignedFile(fileBytes, repDoc.getName(), null);
                        if(repDoc.getAbsolutePath().contains("_delegation_with_vote")) {
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
                                trustAnchors, metaInf.getDateBegin(),  metaInf.getDateFinish(), representativeNif,
                                timeStampServerCert);
                        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                            log.info("ERROR - responseVS.getStatusCode(): " + responseVS.getStatusCode());
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
                        SignedFile vote = new SignedFile(fileBytes, voteFile.getName(), null);
                        ResponseVS representativeVoteResponse = DocumentVSValidator.validateVote(vote,
                                trustAnchors, eventTrustedAnchors,  representativeDataMetaInf.
                                getOptionSelectedId(), eventURL, metaInf.getDateBegin(), metaInf.getDateFinish(),
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
                log.info(msg);
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
            throw new ExceptionVS(message);
        }
        log.info(message);
        return new ResponseVS(statusCode, message);
    }
    
    private ResponseVS validateAccessRequests() throws Exception {
        log.info("validateAccessRequests");
        File[] batchDirList = accessRequestsDir.listFiles();
        int statusCode = ResponseVS.SC_OK;
        int numAccessRequestOK = 0;
        int numAccessRequestERROR = 0;
        Map<String, String> signersNifMap = new HashMap<String, String>();
        for(File batchDir:batchDirList) {
            if(batchDir.isDirectory()) {
                File[] accessRequests = batchDir.listFiles();
                for(File accessRequest : accessRequests) {
                    if(isCanceled.get()) return new ResponseVS(ResponseVS.SC_CANCELED);
                    String errorMessage = null;
                    byte[] accessRequestBytes = FileUtils.getBytesFromFile(accessRequest);
                    SignedFile signedFile = new SignedFile(accessRequestBytes, accessRequest.getName(), null);
                    ResponseVS validationResponse = DocumentVSValidator.validateAccessRequest(signedFile,
                            trustAnchors, eventURL, metaInf.getDateBegin(),
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
                        log.log(Level.SEVERE, errorMessage);
                        errorList.add(errorMessage);
                    } 
                    notifyValidationListener(statusCode, validationResponse.getMessage(),
                            ValidationEvent.ACCESS_REQUEST);
                }
            }
        }
        statusCode = ResponseVS.SC_OK;
        log.info("numAccessRequestOK: " + numAccessRequestOK + " - numAccessRequestERROR: " + numAccessRequestERROR);
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
        log.info("validateVotes");
        int statusCode = ResponseVS.SC_OK;
        File[] batchDirList = votesDir.listFiles();
        int numVotesOK = 0;
        int numVotesERROR = 0;
        int numRepresentativeVotes = 0;
        Map<Long, String> signerCertMap = new HashMap<Long, String>();
        for(File batchDir : batchDirList) {
            if(batchDir.isDirectory()) {
                File[] votes = batchDir.listFiles();
                for(File vote : votes) {
                    if(isCanceled.get()) return new ResponseVS(ResponseVS.SC_CANCELED);
                    byte[] voteBytes = FileUtils.getBytesFromFile(vote);
                    SignedFile signedFile = new SignedFile(voteBytes, vote.getName(), null);
                    ResponseVS<Long> validationResponse = DocumentVSValidator.validateVote(signedFile,
                            trustAnchors, eventTrustedAnchors, null, eventURL,
                            metaInf.getDateBegin(), metaInf.getDateFinish(), timeStampServerCert);
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
                        String msg = "ERROR vote - File: " + vote.getAbsolutePath() + " - msg: " +
                                validationResponse.getMessage();
                        log.log(Level.SEVERE, msg);
                        errorList.add(msg);
                    }
                    notifyValidationListener(statusCode, validationResponse.getMessage(), ValidationEvent.VOTE);
                }
            }
        }
        statusCode = ResponseVS.SC_OK;
        log.info("numVotesOK: " + numVotesOK + " - numVotesERROR: " + numVotesERROR);
        if(numVotesERROR > 0) statusCode = ResponseVS.SC_ERROR;
        if(numVotesOK != metaInf.getNumVotes()) {
            statusCode = ResponseVS.SC_ERROR;
            errorList.add(ContextVS.getMessage("numVotesResultErrorMsg", metaInf.getNumVotes(), numVotesOK));
        }
        for(FieldEventVS option : metaInf.getOptionList()) {
            if(option.getNumVotesResult().longValue() != optionsMap.get(option.getId()).longValue()) {
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
        ElectionBackupValidator backupValidator = new ElectionBackupValidator(args[0], null);
        backupValidator.call();
        System.exit(0);
    }

}