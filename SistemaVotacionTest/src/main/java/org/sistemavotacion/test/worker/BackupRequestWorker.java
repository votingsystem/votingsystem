package org.sistemavotacion.test.worker;

import com.itextpdf.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import org.sistemavotacion.worker.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.SwingWorker;
import org.bouncycastle.operator.OperatorCreationException;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.MetaInf;
import org.sistemavotacion.herramientavalidacion.modelo.SignedFile;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class BackupRequestWorker extends SwingWorker<Respuesta<MetaInf>, String> 
        implements VotingSystemWorker, VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(BackupRequestWorker.class);
    
    private static final int REQUEST_BACKUP_WORKER      = 0;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    
    private Integer id;
    private String urlRequest;
    private Respuesta respuesta = new Respuesta(Respuesta.SC_ERROR);
    private VotingSystemWorkerListener workerListener;

    private PrivateKey signerPrivateKey;
    private Certificate[] signerCertChain;
    private X509Certificate destinationCert;
    private PdfReader requestBackupPDF = null;

    
    public BackupRequestWorker(Integer id, String urlRequest, 
            byte[] pdfRequestBytes, VotingSystemWorkerListener workerListener) 
            throws OperatorCreationException {
        this.id = id;
        this.urlRequest = ContextoPruebas.INSTANCE.getUrlBackupEvents();
        this.workerListener = workerListener;
        try {
            signerPrivateKey = ContextoPruebas.INSTANCE.getUserTestPrivateKey();
            signerCertChain = ContextoPruebas.INSTANCE.getUserTestCertificateChain();
            destinationCert = Contexto.INSTANCE.getAccessControl().getCertificate(); 
            requestBackupPDF = new PdfReader(pdfRequestBytes);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
    
    @Override protected Respuesta doInBackground() throws Exception {
        new PDFSignedSenderWorker(REQUEST_BACKUP_WORKER, urlRequest, null, null, 
                null, requestBackupPDF, signerPrivateKey, signerCertChain, 
                null, this).execute();
        countDownLatch.await();
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            List<SignedFile> signedFileList = new ArrayList<SignedFile>();
            byte[] backupFileBytes = respuesta.getBytesArchivo();
            File backupFile = File.createTempFile("backupFile", ".zip");
            backupFile.deleteOnExit();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(backupFileBytes), backupFile);
            ZipFile backupZip = new ZipFile(backupFile);
            Enumeration entries = backupZip.entries();
            
            MetaInf metaInf = null;
            List<String> errorList = new ArrayList<String>();
            int counter = 0;
            while(entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                byte[] entryBytes = FileUtils.getBytesFromInputStream(
                        backupZip.getInputStream(entry));
                if(!entry.isDirectory()) {
                    byte[] signedFileBytes = FileUtils.getBytesFromInputStream(
                        backupZip.getInputStream(entry));
                    String msg = checkByteArraySize(signedFileBytes);
                    if(msg == null) {
                        if ("meta.inf".equals(entry.getName())) {
                            metaInf = MetaInf.parse(new String(entryBytes));
                            logger.debug("MetaInf OK");
                            counter = 0;
                        } else {
                            SignedFile signedFile = new SignedFile(
                                    signedFileBytes, entry.getName());
                            signedFileList.add(signedFile);
                            if(!signedFile.isValidSignature()) {
                                 msg = "ERROR ZipEntry '" + entry.getName() + "' invalid signature";
                                
                            } else logger.debug("OK ZipEntry " + entry.getName());
                        }
                    } 
                    if(msg != null){
                        logger.error(msg);
                        errorList.add(msg);
                    }
                }
                counter++;
            }
            if(metaInf != null && !errorList.isEmpty()) metaInf.setErrorsList(errorList);
            respuesta.setData(metaInf);
            logger.debug("Backup with " + signedFileList.size() + " files and " + 
                    errorList.size() + " errors");
            backupZip.close();
        }
        return respuesta;
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

    @Override protected void done() {//on the EDT
        try {
            respuesta = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        } 
        workerListener.showResult(this);
    }
    
    @Override//on the EDT
    protected void process(List<String> messages) {
        workerListener.processVotingSystemWorkerMsg(messages);
    }


   @Override public String getMessage() {
        if(respuesta == null) return null;
        else return respuesta.getMensaje();
    }

    @Override public int getId() {
        return this.id;
    }

    @Override  public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public Respuesta getRespuesta() {
        return respuesta;
    }

    @Override
    public void processVotingSystemWorkerMsg(List<String> messages) { }

    @Override public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
                " - worker: " + worker.getClass().getSimpleName() + 
                " - workerId:" + worker.getId());
        String msg = null;
        respuesta = worker.getRespuesta();
        switch(worker.getId()) {
            case REQUEST_BACKUP_WORKER:
                if(Respuesta.SC_OK == worker.getStatusCode()) { }
                break;
            
        }
        countDownLatch.countDown();
    }

}