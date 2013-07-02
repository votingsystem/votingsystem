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
import org.sistemavotacion.herramientavalidacion.modelo.BackupData;
import org.sistemavotacion.herramientavalidacion.modelo.ClaimBackupData;
import org.sistemavotacion.herramientavalidacion.modelo.ManifestBackupData;
import org.sistemavotacion.modelo.MetaInf;
import org.sistemavotacion.herramientavalidacion.modelo.SignedFile;
import org.sistemavotacion.herramientavalidacion.modelo.VotingBackupData;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.modelo.Tipo;
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
        
    
    public Respuesta call1() throws Exception {
        return null;
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
        Enumeration entries = backupZip.entries();

        VotingBackupData votingBackupData = null;        
        ManifestBackupData manifestBackupData = null; 
        BackupData resultBackupData = null;
        ClaimBackupData claimBackupData = null;
        MetaInf metaInf = null;
        if(backupZip.getEntry("meta.inf") != null) {
            InputStream inputStream = backupZip.getInputStream(
                backupZip.getEntry("meta.inf"));
            byte[] metaInfBytes = FileUtils.getBytesFromInputStream(inputStream);
            metaInf = MetaInf.parse(new String(metaInfBytes));
            logger.debug("metaInf: " + metaInf.getFormattedInfo());
        } else logger.error(" --- Backup without MetaInf ---");
        
        if(Tipo.EVENTO_VOTACION == metaInf.getType()) {
            votingBackupData = new VotingBackupData();  
            resultBackupData = votingBackupData;
        } else if(Tipo.EVENTO_FIRMA == metaInf.getType()) {
            manifestBackupData = new ManifestBackupData();
            resultBackupData = manifestBackupData;
        } else if(Tipo.EVENTO_RECLAMACION == metaInf.getType()) {
            claimBackupData = new ClaimBackupData();
            resultBackupData = claimBackupData;
        }
        resultBackupData.setMetaInf(metaInf);
        
        
        List<String> errorList = new ArrayList<String>();
        int numFilesOK = 0;
        

        while(entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry)entries.nextElement();
            
            if(entry.isDirectory())  {
                continue;
            }
            //Manifests
            if(entry.getName().contains("FIRMA_")) {
                byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                SignedFile signedFile = new SignedFile(
                    signedFileBytes, entry.getName());
                manifestBackupData.addSignature(signedFile);
            }
            //Claims
            if(entry.getName().contains("RECLAMACION_")) {
                byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                SignedFile signedFile = new SignedFile(
                    signedFileBytes, entry.getName());
                claimBackupData.addSignature(signedFile);
            }
            
            if(entry.getName().contains("systemTrustedCerts.pem")) {
                byte[] systemTrustedCerts = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                resultBackupData.setSystemTrustedCerts(systemTrustedCerts);
            }
            
            if(entry.getName().contains("timeStampCert.pem")) {
                byte[] timeStampCertBytes = FileUtils.getBytesFromInputStream(
                    backupZip.getInputStream(entry));
                resultBackupData.setTimeStampCert(timeStampCertBytes);
            }
 
            //Voting events
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

        }
        logger.debug(" ---- Event: " + metaInf.getId() + resultBackupData.getFormattedInfo());
        
        respuesta.setData(metaInf);
        if(metaInf != null && !errorList.isEmpty()) {
            metaInf.setErrorsList(errorList);
            if(metaInf.getNumSignatures() != numFilesOK) {
                errorList.add(" ### meta.inf expected '" + metaInf.getNumSignatures() + 
                        "' signatures - but the file only contains " + 
                        numFilesOK + " valid signatures");
            }
        }
        logger.debug("Backup with " + errorList.size() + " errors");
        backupZip.close();
        if(errorList.isEmpty()) {
            respuesta.setCodigoEstado(Respuesta.SC_OK);
        } 
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
            filePath = "/home/jgzornoza/Descargas/VotosEvento_5.zip";
            //filePath = "/home/jgzornoza/Descargas/CopiaSeguridadDeReclamacion.zip";
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
        System.exit(0);
    }
    

}