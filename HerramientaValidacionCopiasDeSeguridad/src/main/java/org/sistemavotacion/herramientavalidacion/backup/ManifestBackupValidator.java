package org.sistemavotacion.herramientavalidacion.backup;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.herramientavalidacion.ContextoHerramienta;
import org.sistemavotacion.herramientavalidacion.modelo.MetaInf;
import org.sistemavotacion.herramientavalidacion.modelo.SignedFile;
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
public class ManifestBackupValidator implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(ManifestBackupValidator.class);

    private ValidatorListener validatorListener = null;
    private File backupDir = null;
    private List<String> errorList = new ArrayList<String>();
    //private Set<X509Certificate> systemTrustedCerts;
    private KeyStore trustedCertsKeyStore;
    private X509Certificate timeStampServerCert;
    private MetaInf metaInf;
    private String eventURL = null;
    private String manifestFileName = null;
    
    public ManifestBackupValidator(String backupPath, 
            ValidatorListener validatorListener) throws Exception {
        ContextoHerramienta.INSTANCE.init(null);
        backupDir = new File(backupPath);
        this.validatorListener =  validatorListener;
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
        manifestFileName = ContextoHerramienta.INSTANCE.
                getString("manifestFileName");

        String backupPath = backupDir.getAbsolutePath();
        File trustedCertsFile = new File(backupPath + File.separator + 
                "systemTrustedCerts.pem");
        Collection<X509Certificate> trustedCerts = CertUtil.fromPEMToX509CertCollection(
                FileUtils.getBytesFromFile(trustedCertsFile));
        trustedCertsKeyStore = KeyStore.getInstance("JKS");
        trustedCertsKeyStore.load(null, null);
        for(X509Certificate certificate:trustedCerts) {
            trustedCertsKeyStore.setCertificateEntry(
                    certificate.getSubjectDN().toString(), certificate);
        }
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
        
        Respuesta respuesta = validateManifests();

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
        notifyValidationListener(respuesta.getCodigoEstado(), 
                durationStr, ValidationEvent.MANIFEST_FINISIH);        
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
    
    private Respuesta validateManifests() throws Exception {
        logger.debug("validateManifests");
        File[] batchDirs = backupDir.listFiles();
        int statusCode = Respuesta.SC_OK;
        int numManifestOK = 0;
        int numManifestERROR = 0;
        Map<String, String> signersNifMap = new HashMap<String, String>();
        for(File batchDir:batchDirs) {
            if(batchDir.isDirectory()) {
                File[] manifests = batchDir.listFiles();
                for(File manifest : manifests) {
                    String errorMessage = null;
                    byte[] accessRequestBytes = FileUtils.
                            getBytesFromFile(manifest);
                    SignedFile signedFile = new SignedFile(
                            accessRequestBytes, manifest.getName());
                    Respuesta validationResponse = signedFile.validateAsManifestSignature(
                            trustedCertsKeyStore, metaInf.getDateInit(), 
                            metaInf.getDateFinish(), timeStampServerCert);
                    statusCode = validationResponse.getCodigoEstado();
                    if(Respuesta.SC_OK == validationResponse.getCodigoEstado()) {
                        numManifestOK++;
                        /*boolean repeatedAccessrequest = signersNifMap.containsKey(signedFile.getSignerNif());
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
                        } */
                    } else {
                        numManifestERROR++;
                        /*errorMessage = "ERROR ACCES REQUEST - File: " + 
                                accessRequest.getAbsolutePath() + " - msg: " +
                                validationResponse.getMensaje();*/
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
        logger.debug("numManifestOK: " + numManifestOK + 
                " - numManifestERROR: " + numManifestERROR);
        String message = null;
        if(metaInf.getNumSignatures() != numManifestOK) {
            statusCode = Respuesta.SC_ERROR;
            message = ContextoHerramienta.INSTANCE.getString("numAccessRequestErrorMsg", 
                    metaInf.getNumSignatures(), numManifestOK);
        } else message =  ContextoHerramienta.INSTANCE.getString(
                "accessRequestValidationResultMsg", metaInf.getNumSignatures());
        return new Respuesta(statusCode, message);
    }
    
    public static void main(String[] args) throws Exception {
        
        ManifestBackupValidator dirBackupValidator = new ManifestBackupValidator(
                "./Descargas/FirmasRecibidasEvento_4", null);
        dirBackupValidator.call();

        System.exit(0);
    }

    

}