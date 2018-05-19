package org.votingsystem.client.backup;

import org.votingsystem.client.util.SignatureValidationResult;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.voting.ElectionOptionDto;
import org.votingsystem.dto.voting.ElectionStatsDto;
import org.votingsystem.util.*;
import org.votingsystem.xml.XML;

import java.io.File;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ElectionBackupValidator implements BackupValidator<ResponseDto> {
    
    private static Logger log = Logger.getLogger(ElectionBackupValidator.class.getName());

    private ValidatorListener validatorListener = null;
    private File backupDir = null;
    private List<String> errorList = new ArrayList<String>();
    private Set<TrustAnchor> trustAnchors;
    private Set<TrustAnchor> eventTrustedAnchors;
    private X509Certificate timeStampServerCert;
    private ElectionStatsDto electionStats;
    private final File identityRequestsDir;
    private final File votesDir;
    private Map<String, Long> optionsMap = new HashMap<>();
    private AtomicBoolean isCanceled = new AtomicBoolean(false);
    
    public ElectionBackupValidator(String backupPath, ValidatorListener validatorListener) throws Exception {
        backupDir = new File(backupPath);
        this.validatorListener =  validatorListener;
        identityRequestsDir = new File(backupPath + File.separator + OperationType.VALIDATE_IDENTITY.toString());
        votesDir = new File(backupPath + File.separator + OperationType.SEND_VOTE.toString());
    }
   
    private String checkByteArraySize (byte[] signedFileBytes) {
        String result = null;
        if (signedFileBytes.length > Constants.SIGNED_MAX_FILE_SIZE) {
            result = Messages.currentInstance().get("fileSizeExceededMsg",
                    Constants.SIGNED_MAX_FILE_SIZE_KB, signedFileBytes.length);
        }
        return result;
    }

    public void cancel() {
        isCanceled.set(true);
    }

    @Override public ResponseDto call() throws Exception {
        LocalDateTime begin = LocalDateTime.now();
        String backupPath = backupDir.getAbsolutePath();

        File trustedCertsFile = new File(backupPath + File.separator + "systemTrustedCerts.pem");
        Collection<X509Certificate> trustedCerts = PEMUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        trustAnchors = new HashSet<>(trustedCerts.size());
        for(X509Certificate certificate: trustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            trustAnchors.add(anchor);
        }
        File eventTrustedCertsFile = new File(backupPath + "eventTrustedCerts.pem");
        Collection<X509Certificate> eventTrustedCerts = PEMUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(eventTrustedCertsFile));
        eventTrustedAnchors = new HashSet<>(eventTrustedCerts.size());
        for(X509Certificate certificate: eventTrustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            eventTrustedAnchors.add(anchor);
        }
        File timeStampCertFile = new File(backupPath + "timestampCert.pem");
        Collection<X509Certificate> timeStampCerts = PEMUtils.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(timeStampCertFile));   
        timeStampServerCert = timeStampCerts.iterator().next();
            
        File statsFile = new File(backupPath + File.separator + "stats.xml");
        ElectionStatsDto electionStats = new XML().getMapper().readValue(
                FileUtils.getBytesFromFile(statsFile), ElectionStatsDto.class);
        for(ElectionOptionDto electionOption : electionStats.getElectionOptions()) {
            optionsMap.put(electionOption.getContent(), electionOption.getNumVotes());
        }

        ResponseDto response = validateIdentityRequests();
        if(ResponseDto.SC_OK != response.getStatusCode()) return response;
        notifyValidationListener(new ValidationEvent(response.getStatusCode(), ValidationEvent.Type.ACCESS_REQUEST_FINISH,
                response.getMessage()));
        String accessRequestValidationDurationStr = DateUtils.
                getElapsedHoursMinutesSeconds(LocalDateTime.now(), begin);
        response = validateVotes();
        if(ResponseDto.SC_OK != response.getStatusCode()) return response;
        String votesValidationDurationStr = DateUtils.
                getElapsedHoursMinutesSeconds(LocalDateTime.now(), begin);
        notifyValidationListener(new ValidationEvent(response.getStatusCode(), ValidationEvent.Type.VOTE_FINISH,
                votesValidationDurationStr));

        log.info("accessrequestValidation duration: " + accessRequestValidationDurationStr);
        log.info("votesValidationDurationStr duration: " + votesValidationDurationStr);

        Integer statusCode = errorList.size() > 0 ? ResponseDto.SC_ERROR : ResponseDto.SC_OK;
        response.setStatusCode(statusCode);
        if(!errorList.isEmpty()) {
            log.log(Level.SEVERE, "Num.  errors: " + errorList.size());
            for(String error : errorList) {
                log.log(Level.SEVERE, error);
            }
        } else log.info("Backup without errors");

        String durationStr = DateUtils.getElapsedHoursMinutesSeconds(LocalDateTime.now(), begin);
        log.info("duration: " + durationStr);
        response.setMessage(durationStr);
        response.setData(electionStats);
        return response;
    }
    
    private void notifyValidationListener(ValidationEvent validationEvent) {
        if(validatorListener != null) {
            validatorListener.processValidationEvent(validationEvent);
        }
    }

    private SignatureValidationResult validateIdentityRequest(byte[] identityRequest) {
        log.severe("========= TODO");
        return new SignatureValidationResult();
    }

    private SignatureValidationResult validateVote(byte[] identityRequest) {
        log.severe("========= TODO");
        return new SignatureValidationResult();
    }

    private ResponseDto validateIdentityRequests() throws Exception {
        File[] batchDirList = identityRequestsDir.listFiles();
        int statusCode;
        int numAccessRequestOK = 0;
        int numAccessRequestERROR = 0;
        Map<String, String> signersNifMap = new HashMap<>();
        for(File batchDir:batchDirList) {
            if(batchDir.isDirectory()) {
                File[] identityRequests = batchDir.listFiles();
                log.info("batch dir: " + batchDir.getAbsolutePath() + " has " + identityRequests.length + " identity requests");
                for(File identityRequest : identityRequests) {
                    if(isCanceled.get())
                        return new ResponseDto(ResponseDto.SC_CANCELED);
                    String errorMessage = null;
                    SignatureValidationResult validationResult = validateIdentityRequest(FileUtils.getBytesFromFile(identityRequest));
                    statusCode = validationResult.getStatusCode();
                    if(ResponseDto.SC_OK == validationResult.getStatusCode()) {
                        boolean repeatedAccessrequest = signersNifMap.containsKey(validationResult.getSigner().getNumIdAndType());
                        if(repeatedAccessrequest) {
                            numAccessRequestERROR++;
                            errorMessage = Messages.currentInstance().get("accessRequetsRepeatedErrorMsg",
                                    validationResult.getSigner().getNumIdAndType()) + " - " +
                                    identityRequest.getAbsolutePath() + " - " +
                                    signersNifMap.get(validationResult.getSigner().getNumIdAndType());
                        } else {
                            numAccessRequestOK++;
                            signersNifMap.put(validationResult.getSigner().getNumIdAndType(),
                                    identityRequest.getAbsolutePath());
                        } 
                    } else {
                        numAccessRequestERROR++;
                        errorMessage = "ERROR ACCES REQUEST - File: " + identityRequest.getAbsolutePath() + " - msg: " +
                                validationResult.getMessage();
                    }
                    if(errorMessage != null) {
                        statusCode = ResponseDto.SC_ERROR;
                        log.log(Level.SEVERE, errorMessage);
                        errorList.add(errorMessage);
                    }
                    notifyValidationListener(new ValidationEvent(statusCode, ValidationEvent.Type.ACCESS_REQUEST,
                            validationResult.getMessage()));
                }
            }
        }
        statusCode = ResponseDto.SC_OK;
        log.info("numAccessRequestOK: " + numAccessRequestOK + " - numAccessRequestERROR: " + numAccessRequestERROR);
        String message = null;
        if(electionStats.getNumIdentityRequests().intValue() != numAccessRequestOK) {
            statusCode = ResponseDto.SC_ERROR;
            message = Messages.currentInstance().get("numAccessRequestErrorMsg",
                    electionStats.getNumIdentityRequests().intValue(), numAccessRequestOK);
        } else message =  Messages.currentInstance().get("accessRequestValidationResultMsg",
                electionStats.getNumIdentityRequests().intValue());
        return new ResponseDto(statusCode, message);
    }
    
    private ResponseDto validateVotes() throws Exception {
        log.info("validateVotes");
        int statusCode = ResponseDto.SC_OK;
        File[] batchDirList = votesDir.listFiles();
        int numVotesOK = 0;
        int numVotesERROR = 0;
        int numRepresentativeVotes = 0;
        Map<Long, String> signerCertMap = new HashMap<Long, String>();
        for(File batchDir : batchDirList) {
            if(batchDir.isDirectory()) {
                File[] votes = batchDir.listFiles();
                for(File vote : votes) {
                    if(isCanceled.get()) return new ResponseDto(ResponseDto.SC_CANCELED);
                    SignatureValidationResult validationResult = validateVote(FileUtils.getBytesFromFile(vote));
                    statusCode = validationResult.getStatusCode();
                    if(ResponseDto.SC_OK == validationResult.getStatusCode()) {
                        Long certSerialNumber = validationResult.getSigningCert().getSerialNumber().longValue();
                        boolean repeatedVote = signerCertMap.containsKey(certSerialNumber);
                        if(repeatedVote){
                            numVotesERROR++;
                            statusCode = ResponseDto.SC_ERROR;
                            String msg = Messages.currentInstance().get(
                                    "voteRepeatedErrorMsg", validationResult.getSigningCert()
                                    .getSerialNumber().longValue()) + " - " + vote.getAbsolutePath() + " - " +
                                    signerCertMap.get(certSerialNumber);
                            errorList.add(msg);
                        } else {
                            numVotesOK++;
                            String optionSelected = validationResult.getVote().getOptionSelected().getContent();
                            signerCertMap.put(certSerialNumber, vote.getAbsolutePath());
                            optionsMap.put(optionSelected, optionsMap.get(optionSelected) + 1);
                        }
                    } else {
                        numVotesERROR++;
                        String msg = "ERROR vote - File: " + vote.getAbsolutePath() + " - msg: " +
                                validationResult.getMessage();
                        log.log(Level.SEVERE, msg);
                        errorList.add(msg);
                    }
                    notifyValidationListener(new ValidationEvent(statusCode, ValidationEvent.Type.VOTE,
                            validationResult.getMessage()));
                }
            }
        }
        statusCode = ResponseDto.SC_OK;
        log.info("numVotesOK: " + numVotesOK + " - numVotesERROR: " + numVotesERROR);
        if(numVotesERROR > 0)
            statusCode = ResponseDto.SC_ERROR;
        if(numVotesOK != electionStats.getNumVotes().intValue()) {
            statusCode = ResponseDto.SC_ERROR;
            errorList.add(Messages.currentInstance().get("numVotesResultErrorMsg", electionStats.getNumVotes(), numVotesOK));
        }
        for(ElectionOptionDto option : electionStats.getElectionOptions()) {
            if(option.getNumVotes().longValue() != optionsMap.get(option.getContent()).longValue()) {
                statusCode = ResponseDto.SC_ERROR;
                errorList.add(Messages.currentInstance().get("numVotesOptionErrorMsg", option.getContent(),
                        option.getNumVotes(), optionsMap.get(option.getContent())));
            }
        }
        return new ResponseDto(statusCode);
    }
    
    public static void main(String[] args) throws Exception {
        new ElectionBackupValidator(args[0], null).call();
        System.exit(0);
    }

}