package org.sistemavotacion.herramientavalidacion.backup;

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
import java.util.concurrent.atomic.AtomicLong;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.herramientavalidacion.ContextoHerramienta;
import org.sistemavotacion.herramientavalidacion.modelo.MetaInf;
import org.sistemavotacion.herramientavalidacion.modelo.RepresentativeData;
import org.sistemavotacion.herramientavalidacion.modelo.RepresentativesData;
import org.sistemavotacion.herramientavalidacion.modelo.SignedFile;
import org.sistemavotacion.modelo.OpcionEvento;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotingBackupValidator implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(VotingBackupValidator.class);

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
    private Map<Long, AtomicLong> optionsMap = new HashMap<Long, AtomicLong>();
    
    public VotingBackupValidator(String backupPath, 
            ValidatorListener validatorListener) throws Exception {
        ContextoHerramienta.INSTANCE.init();
        backupDir = new File(backupPath);
        this.validatorListener =  validatorListener;
        accessRequestsDir = new File(backupPath + "/accessRequest");
        votesDir = new File(backupPath + "/votes");
    }
   
    private String checkByteArraySize (byte[] signedFileBytes) {
        //logger.debug("checkByteArraySize");
        String result = null;
        if (signedFileBytes.length > Contexto.SIGNED_MAX_FILE_SIZE) {
            result = ContextoHerramienta.INSTANCE.getString("fileSizeExceededMsg", 
                        Contexto.SIGNED_MAX_FILE_SIZE_KB, signedFileBytes.length);
        }
        return result;
    }
    
    @Override public Respuesta call() throws Exception {
        long begin = System.currentTimeMillis();
        representativeReportFileName = ContextoHerramienta.INSTANCE.
                getString("representativeReportFileName");
        accessRequestFileName = ContextoHerramienta.INSTANCE.
                getString("accessRequestFileName");
        representativeVoteFileName = ContextoHerramienta.INSTANCE.
                getString("representativeVoteFileName");

        String backupPath = backupDir.getAbsolutePath();
        File trustedCertsFile = new File(backupPath + File.separator + 
                "systemTrustedCerts.pem");
        Collection<X509Certificate> trustedCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        systemTrustedCerts = new HashSet(trustedCerts);
        File eventTrustedCertsFile = new File(backupPath + File.separator + 
                "eventTrustedCerts.pem");
        Collection<X509Certificate> eventTrustedCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(eventTrustedCertsFile));        
        eventTrustedCertsSet = new HashSet(eventTrustedCerts);
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
            eventURL = metaInf.getEventURL();
        }
        for(OpcionEvento option : metaInf.getOptionList()) {
            optionsMap.put(option.getId(), new AtomicLong(0));
        }
        
        Respuesta respuesta = validateRepresentativeData();
        notifyValidationListener(respuesta.getCodigoEstado(), 
                respuesta.getMensaje(), ValidationEvent.REPRESENTATIVE_FINISH);
        String representativeValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        respuesta = validateAccessRequests();
        notifyValidationListener(respuesta.getCodigoEstado(), 
                respuesta.getMensaje(), ValidationEvent.ACCESS_REQUEST_FINISH);
        String accessrequestValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        respuesta = validateVotes();
        String votesValidationDurationStr = DateUtils.
                getElapsedTimeHoursMinutesFromMilliseconds(System.currentTimeMillis() - begin);
        notifyValidationListener(respuesta.getCodigoEstado(), 
                votesValidationDurationStr, ValidationEvent.VOTE_FINISH);
        
        
        logger.debug("representativeValidation duration: " + 
                representativeValidationDurationStr);
        logger.debug("accessrequestValidation duration: " + 
                accessrequestValidationDurationStr);
        logger.debug("votesValidationDurationStr duration: " + 
                votesValidationDurationStr);

        Integer statusCode = errorList.size() > 0? Respuesta.SC_ERROR:Respuesta.SC_OK;
        respuesta.setErrorList(errorList);
        respuesta.setCodigoEstado(statusCode);
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
        respuesta.setMensaje(durationStr);
        respuesta.setData(metaInf);
        return respuesta;
    }
    
    private void notifyValidationListener(Integer statusCode, 
            String message, ValidationEvent validationEvent) {
        if(validatorListener != null) {
            Respuesta response = new Respuesta(statusCode, message);
            response.setData(validationEvent);
            response.setErrorList(errorList);
            validatorListener.
                processValidationEvent(response);
        }
    }
    
    private Respuesta validateRepresentativeData() throws Exception {
        logger.debug("validateRepresentativeData");
        long numRepresentatives = 0;
        long numRepresented = 0;
        long numRepresentativesWithVote = 0;
        long numRepresentedWithAccessRequest = 0;
        long numVotesRepresentedByRepresentatives = 0;
        
        String repAccreditationsBackupPath = backupDir.getAbsolutePath() + 
            File.separator + ContextoHerramienta.INSTANCE.getString("repAccreditationsBackupPartPath");
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
                RepresentativeData representativeDataMetaInf = metaInf.
                        getRepresentativeData(representativeNif);
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
                                String errorMsg = ContextoHerramienta.INSTANCE.getString(
                                        "representedAccessRequestNotFound", 
                                        signedFile.getNifFromRepresented(),
                                        representativeNif);
                                errorList.add(errorMsg);
                            }
                        }
                        Respuesta respuesta = signedFile.validateAsRepresentationDocument(
                                systemTrustedCerts, metaInf.getDateFinish(),
                                representativeNif, timeStampServerCert);
                        logger.debug("respuesta.getCodigoEstado(): " + 
                                respuesta.getCodigoEstado());
                        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                            errorList.add(respuesta.getMensaje());
                        }
                        notifyValidationListener(respuesta.getCodigoEstado(), 
                                respuesta.getMensaje(), ValidationEvent.REPRESENTATIVE);
                    }
                }
                if(representativeDataMetaInf.getOptionSelectedId() != null) {
                    List<File> result = FileUtils.findRecursively(
                            votesDir, "_" + representativeNif + ".p7m");
                    if(!result.isEmpty()) {
                        File voteFile = result.iterator().next();
                        byte[] fileBytes = FileUtils.getBytesFromFile(voteFile);
                        SignedFile vote = new SignedFile(fileBytes, voteFile.getName());
                        Respuesta representativeVoteResponse = vote.validateAsVote(
                                systemTrustedCerts, eventTrustedCertsSet, 
                                representativeDataMetaInf.getOptionSelectedId(), eventURL,
                                metaInf.getDateInit(), metaInf.getDateFinish(),
                                timeStampServerCert);
                        if (Respuesta.SC_OK != representativeVoteResponse.getCodigoEstado()) {
                            errorList.add(representativeVoteResponse.getMensaje());
                        } else {
                            numRepresentativesWithVote++;    
                            numVotesRepresented = numRepresentations - 
                                    numRepresentedWithVote;
                            numVotesRepresentedByRepresentatives += numVotesRepresented;
                        } 
                    } else {
                        String errorMsg = ContextoHerramienta.INSTANCE.getString(
                                "representativeVoteNotFound", representativeNif);
                        errorList.add(errorMsg);
                    }
                }        
                String msg = "NIF: " + representativeNif +
                    " - numRepresentations: " + numRepresentations + 
                    " - numRepresentedWithVote: " + numRepresentedWithVote + 
                    " - numVotesRepresented: " + numVotesRepresented;
                logger.debug(msg);
                if(representativeDataMetaInf.getNumRepresentations() != numRepresentations ||
                    representativeDataMetaInf.getNumVotesRepresented() != numVotesRepresented ||
                    representativeDataMetaInf.getNumRepresentedWithVote() != numRepresentedWithVote) {           
                    String errorMsg = ContextoHerramienta.INSTANCE.getString(
                        "representativeDataErrorMsg", representativeNif, 
                        representativeDataMetaInf.getString(), msg);
                    errorList.add(errorMsg);
                } 
            }
        }
        int statusCode = Respuesta.SC_OK;
        numRepresented += numRepresentatives;
        String message = "numRepresentatives: " + numRepresentatives + 
                " - numRepresented: " + numRepresented + 
                " - numRepresentativesWithVote: " + numRepresentativesWithVote + 
                " - numRepresentedWithAccessRequest: " +  numRepresentedWithAccessRequest +
                " - numVotesRepresentedByRepresentatives: " + numVotesRepresentedByRepresentatives;
        if(representativesData.getNumRepresentatives() != numRepresentatives ||
                representativesData.getNumRepresentativesWithVote() != numRepresentativesWithVote ||
                representativesData.getNumRepresentedWithAccessRequest() != numRepresentedWithAccessRequest ||
                representativesData.getNumRepresented() != numRepresented ||
                representativesData.getNumVotesRepresentedByRepresentatives() 
                != numVotesRepresentedByRepresentatives) {
            statusCode = Respuesta.SC_ERROR;
            message = ContextoHerramienta.INSTANCE.getString(
                    "representativesDataErrorMsg", representativesData.getString(),
                    message);
            errorList.add(message);
        }
        logger.debug(message);
        return new Respuesta(statusCode, message);
    }
    
    private Respuesta validateAccessRequests() throws Exception {
        logger.debug("validateAccessRequests");
        File[] batchDirs = accessRequestsDir.listFiles();
        int statusCode = Respuesta.SC_OK;
        int numAccessRequestOK = 0;
        int numAccessRequestERROR = 0;
        Map<String, String> signersNifMap = new HashMap<String, String>();
        for(File batchDir:batchDirs) {
            if(batchDir.isDirectory()) {
                File[] accessRequests = batchDir.listFiles();
                for(File accessRequest : accessRequests) {
                    String errorMessage = null;
                    byte[] accessRequestBytes = FileUtils.
                            getBytesFromFile(accessRequest);
                    SignedFile signedFile = new SignedFile(
                            accessRequestBytes, accessRequest.getName());
                    Respuesta validationResponse = signedFile.validateAsAccessRequest(
                            systemTrustedCerts, eventURL, metaInf.getDateInit(), 
                            metaInf.getDateFinish(), timeStampServerCert);
                    statusCode = validationResponse.getCodigoEstado();
                    if(Respuesta.SC_OK == validationResponse.getCodigoEstado()) {
                        boolean repeatedAccessrequest = signersNifMap.containsKey(signedFile.getSignerNif());
                        if(repeatedAccessrequest) {
                            numAccessRequestERROR++;
                            errorMessage = ContextoHerramienta.INSTANCE.getString(
                                    "accessRequetsRepeatedErrorMsg", 
                                    signedFile.getSignerNif()) + " - " + 
                                    accessRequest.getAbsolutePath() + " - " + 
                                    signersNifMap.get(signedFile.getSignerNif());
                        } else {
                            numAccessRequestOK++;
                            signersNifMap.put(signedFile.getSignerNif(), 
                                    accessRequest.getAbsolutePath());
                        } 
                    } else {
                        numAccessRequestERROR++;
                        errorMessage = "ERROR ACCES REQUEST - File: " + 
                                accessRequest.getAbsolutePath() + " - msg: " +
                                validationResponse.getMensaje();
                    }
                    if(errorMessage != null) {
                        statusCode = Respuesta.SC_ERROR;
                        logger.error(errorMessage);
                        errorList.add(errorMessage);
                    } 
                    notifyValidationListener(statusCode, 
                            validationResponse.getMensaje(), 
                            ValidationEvent.ACCESS_REQUEST);
                }
            }
        }
        statusCode = Respuesta.SC_OK;
        logger.debug("numAccessRequestOK: " + numAccessRequestOK + 
                " - numAccessRequestERROR: " + numAccessRequestERROR);
        String message = null;
        if(metaInf.getNumAccessRequest() != numAccessRequestOK) {
            statusCode = Respuesta.SC_ERROR;
            message = ContextoHerramienta.INSTANCE.getString("numAccessRequestErrorMsg", 
                    metaInf.getNumAccessRequest(), numAccessRequestOK);
        } else message =  ContextoHerramienta.INSTANCE.getString(
                "accessRequestValidationResultMsg", metaInf.getNumAccessRequest());
        return new Respuesta(statusCode, message);
    }
    
    private Respuesta validateVotes() throws Exception {
        logger.debug("validateVotes");
        int statusCode = Respuesta.SC_OK;
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
                    Respuesta<Long> validationResponse = signedFile.validateAsVote(
                            systemTrustedCerts, eventTrustedCertsSet, null, eventURL,
                                metaInf.getDateInit(), metaInf.getDateFinish(),
                                timeStampServerCert);
                    statusCode = validationResponse.getCodigoEstado();
                    if(Respuesta.SC_OK == validationResponse.getCodigoEstado()) {
                        boolean repeatedVote = signerCertMap.containsKey(
                                signedFile.getNumSerieSignerCert());
                        if(repeatedVote){
                            numVotesERROR++;
                            statusCode = Respuesta.SC_ERROR;
                            String msg = ContextoHerramienta.INSTANCE.getString(
                                    "voteRepeatedErrorMsg", signedFile.getNumSerieSignerCert()) + " - " + 
                                    vote.getAbsolutePath() + " - " + 
                                    signerCertMap.get(signedFile.getNumSerieSignerCert());
                            errorList.add(msg);
                        } else {
                            numVotesOK++;
                            signerCertMap.put(signedFile.getNumSerieSignerCert(), 
                                    vote.getAbsolutePath());
                            if(vote.getAbsolutePath().contains(
                                    "_" + representativeVoteFileName + "_")) {
                                numRepresentativeVotes++;
                            } else {
                                Long optionSelectedId = validationResponse.getData();
                                optionsMap.get(optionSelectedId).getAndIncrement();
                            }
                        }
                    } else {
                        numVotesERROR++;
                        String msg = "ERROR ACCES REQUEST - File: " + 
                                vote.getAbsolutePath() + " - msg: " +
                                validationResponse.getMensaje();
                        logger.error(msg);
                        errorList.add(msg);
                    }
                    notifyValidationListener(statusCode, 
                            validationResponse.getMensaje(), ValidationEvent.VOTE);
                }
            }
        }
        statusCode = Respuesta.SC_OK;
        logger.debug("numVotesOK: " + numVotesOK + 
                " - numVotesERROR: " + numVotesERROR);
        if(numVotesERROR > 0) statusCode = Respuesta.SC_ERROR;
        logger.debug("numRepresentativeVotes: " + numRepresentativeVotes);
        String message = null;
        if(metaInf.getNumVotes() != numVotesOK) {
            statusCode = Respuesta.SC_ERROR;
            message = ContextoHerramienta.INSTANCE.getString("numVotesErrorMsg", 
                    metaInf.getNumVotes(), numVotesOK);
            
        }
        if(metaInf.getRepresentativesData().getNumRepresentativesWithVote() != 
                numRepresentativeVotes) {
            statusCode = Respuesta.SC_ERROR;
            String msg = ContextoHerramienta.INSTANCE.getString("numRepresentativesVotesErrorMsg", 
                    metaInf.getRepresentativesData().getNumRepresentativesWithVote(), 
                    numRepresentativeVotes);
            if(message == null) message = msg;
            else message = msg.concat("\n" + msg);
        }
        logger.debug(message);
        return new Respuesta(statusCode, message);
    }
    
    public static void main(String[] args) throws Exception {
        
        VotingBackupValidator dirBackupValidator = new VotingBackupValidator(
                "./Descargas/VotosEvento_4", null);
        dirBackupValidator.call();

        System.exit(0);
    }

    

}