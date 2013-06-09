package org.sistemavotacion.test.simulation.callable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.MetaInf;
import org.sistemavotacion.herramientavalidacion.modelo.SignedFile;
import org.sistemavotacion.herramientavalidacion.modelo.VotingBackupData;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class BackupValidator implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(BackupValidator.class);
    
    private Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
    private byte[] backupFileBytes = null;
    
    public BackupValidator(byte[] backupFileBytes) 
            throws Exception {
        this.backupFileBytes = backupFileBytes;
    }
   
    private String checkByteArraySize (byte[] signedFileBytes) {
        //logger.debug("checkByteArraySize");
        String result = null;
        if (signedFileBytes.length > Contexto.SIGNED_MAX_FILE_SIZE) {
            result = ContextoPruebas.INSTANCE.getString("fileSizeExceededMsg", 
                        Contexto.SIGNED_MAX_FILE_SIZE_KB, signedFileBytes.length);
        }
        return result;
    }

    private List<String> validateMetaInf(MetaInf metaInf, 
            List<SignedFile> signedFileList) {
        switch(metaInf.getType()) {
            case EVENTO_FIRMA:
                return validateMetaInfManifest(metaInf, signedFileList);
            case EVENTO_RECLAMACION:
                return validateMetaInfClaim(metaInf, signedFileList);
            case EVENTO_VOTACION:
                return validateMetaInfVoting(metaInf, signedFileList);
        }
        return null;
    }
    
    private List<String> validateMetaInfManifest(MetaInf metaInf, 
            List<SignedFile> signedFileList) {
        int numSignatures = 0;
        List<String> errorList =  new ArrayList<String>();
        for(SignedFile signedFile:signedFileList) {
            if(!signedFile.isPDF()) {
                errorList.add("SignedFile error - " + signedFile.getName());
            } else numSignatures++;
        }
        if(metaInf.getNumSignatures().intValue() != numSignatures) {
            errorList.add("MetaInf error - expected: " + metaInf.
                    getNumSignatures() + " - found: " + numSignatures);
        }
        return errorList;
    }
    
    private List<String> validateMetaInfClaim(MetaInf metaInf, 
            List<SignedFile> signedFileList) {
        int numSignatures = 0;
        List<String> errorList =  new ArrayList<String>();
        for(SignedFile signedFile:signedFileList) {
            if(!signedFile.isPDF()) {
                errorList.add("SignedFile error - " + signedFile.getName());
            } else numSignatures++;
        }
        if(metaInf.getNumSignatures().intValue() != numSignatures) {
            errorList.add("MetaInf error - expected: " + metaInf.
                    getNumSignatures() + " - found: " + numSignatures);
        }
        return errorList;
    }
    
    private List<String> validateMetaInfVoting(MetaInf metaInf, 
            List<SignedFile> signedFileList) {
        int numSignatures = 0;
         List<String> errorList =  new ArrayList<String>();
        for(SignedFile signedFile:signedFileList) {
            //if(signedFile.isPDF())
        }
        return errorList;
    }
    
        
    @Override public Respuesta call() throws Exception {
        String representativeReportFileName = ContextoPruebas.INSTANCE.
                getString("representativeReportFileName");
        String accessRequestFileName = ContextoPruebas.INSTANCE.
                getString("accessRequestFileName");
        
        
        File backupFile = File.createTempFile("backupFile", ".zip");
        backupFile.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(backupFileBytes), backupFile);
        ZipFile backupZip = new ZipFile(backupFile);
        List<SignedFile> signedFileList = new ArrayList<SignedFile>();
        Enumeration entries = backupZip.entries();

                
        MetaInf metaInf = null;
        if(backupZip.getEntry("meta.inf") != null) {
            InputStream inputStream = backupZip.getInputStream(
                backupZip.getEntry("meta.inf"));
            byte[] metaInfBytes = FileUtils.getBytesFromInputStream(inputStream);
            metaInf = MetaInf.parse(new String(metaInfBytes));
            logger.debug("metaInf: " + metaInf.getFormattedInfo());
        } else logger.error(" --- Backup without MetaInf ---");

        
        List<String> errorList = new ArrayList<String>();
        int numFilesOK = 0;
        
        VotingBackupData votingBackupData = new VotingBackupData();
        while(entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry)entries.nextElement();
            
            if(entry.isDirectory())  {
                logger.debug("---------- dir -> " + entry.getName());
                continue;
            }
            
            if(entry.getName().contains("VotoRepresentante_")) {
                byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                SignedFile signedFile = new SignedFile(
                    signedFileBytes, entry.getName());
                votingBackupData.addRepresentativeVote(signedFile);
            };
                        
            if(entry.getName().contains("RepDoc_")) {
                byte[] repDocBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                SignedFile signedFile = new SignedFile(
                    repDocBytes, entry.getName());
                votingBackupData.addRepresentationDoc(signedFile);
            }
            
            if(entry.getName().contains(representativeReportFileName)) {
                byte[] repReportBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                votingBackupData.setRepresentativeReport(repReportBytes);
            }
            
            if(entry.getName().contains(accessRequestFileName)) {
                byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                SignedFile signedFile = new SignedFile(
                    signedFileBytes, entry.getName());
                votingBackupData.addAccessRequests(signedFile);
            }

            if(entry.getName().contains("accessRequestTrustedCerts.pem")) {
                byte[] certsBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                votingBackupData.setAccessRequestTrustedCertsBytes(certsBytes);
            }
            
            if(entry.getName().contains("eventTrustedCerts.pem")) {
                byte[] certsBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                votingBackupData.setEventTrustedCertsBytes(certsBytes);
            }
            
            if(entry.getName().contains("Voto_")) {
                byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                SignedFile signedFile = new SignedFile(
                    signedFileBytes, entry.getName());
                votingBackupData.addVote(signedFile);
            }
            
            

            

            
            
            /*byte[] entryBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
            if(!entry.isDirectory()) {
                byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                String msg = checkByteArraySize(signedFileBytes);
                if(msg == null) {
                    if ("meta.inf".equals(entry.getName())) continue;
                    try {
                        SignedFile signedFile = new SignedFile(
                                signedFileBytes, entry.getName());
                        signedFileList.add(signedFile);
                        if(!signedFile.isValidSignature()) {
                             msg = "ERROR ZipEntry -> " + entry.getName();

                        } else {
                            numFilesOK++;
                            logger.debug("OK ZipEntry -> " + entry.getName());
                        } 
                    } catch(Exception ex) {
                        msg = " ### ZipEntry -> " + entry.getName() + " - msg: " +
                                ex.getMessage();
                        logger.error(msg, ex);
                    }
                } 
                if(msg != null) errorList.add(msg);
            }*/
        }
        
        logger.debug(" ---- FormattedInfo: " + votingBackupData.getFormattedInfo());
        
        respuesta.setData(metaInf);
        if(metaInf != null && !errorList.isEmpty()) {
            metaInf.setErrorsList(errorList);
            if(metaInf.getNumSignatures() != numFilesOK) {
                errorList.add(" ### meta.inf expected '" + metaInf.getNumSignatures() + 
                        "' signatures - but the file only contains " + 
                        numFilesOK + " valid signatures");
            }
        }
        
        logger.debug("Backup with " + signedFileList.size() + " files and " + 
                errorList.size() + " errors");
        backupZip.close();
        if(errorList.isEmpty()) respuesta.setCodigoEstado(Respuesta.SC_OK);
        else {
            respuesta.setErrorList(errorList);
            respuesta.setCodigoEstado(Respuesta.SC_ERROR);
        }
        return respuesta;
    }
    
    public static void main(String[] args) throws Exception {
        ContextoPruebas.INSTANCE.init();
        String filePath = null;
        if(args != null && args.length > 0) {
            logger.debug("args[0]");
            filePath = args[0];
        } else {
            //filePath = "/home/jgzornoza/Descargas/CopiaSeguridadDeManifiesto.zip";
            filePath = "/home/jgzornoza/Descargas/CopiaSeguridadDeVotacion.zip";
        }
        File backupFile = new File(filePath);
        byte[] backupFilebytes = FileUtils.getBytesFromFile(backupFile);
        BackupValidator backupValidator = new BackupValidator(backupFilebytes);
        Respuesta respuesta = backupValidator.call();
        logger.debug("Respuesta - CodigoEstado: " + respuesta.getCodigoEstado());
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
            logger.error("--------- Error List -----------------");
            logger.error("Num. errors: " + respuesta.getErrorList().size());
            logger.error(StringUtils.getFormattedErrorList(respuesta.getErrorList()));
        }
    }
    

}