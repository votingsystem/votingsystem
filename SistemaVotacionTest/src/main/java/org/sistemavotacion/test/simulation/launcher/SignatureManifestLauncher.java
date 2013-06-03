package org.sistemavotacion.test.simulation.launcher;

import com.itextpdf.text.pdf.PdfReader;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import javax.mail.internet.MimeBodyPart;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.PDFSignerWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignatureManifestLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(SignatureManifestLauncher.class);

    private static final int PDF_SIGNER_WORKER    = 0;
    private static final int MANIFEST_SENDER_WORKER = 1;
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time

    private String nif;
    private String urlTimeStampServer = null;
    private String submitSignatureURL = null;
    private String reason = null;
    private String location = null;
    private PdfReader manifestToSign = null;
    private Respuesta respuesta;
        
    public SignatureManifestLauncher (String nif, String urlTimeStampServer,
            String submitSignatureURL, PdfReader manifestToSign, String reason,
            String location) throws Exception {
        this.nif = nif;
        this.urlTimeStampServer = urlTimeStampServer;
        this.reason = reason;
        this.location = location;
        this.manifestToSign = manifestToSign;
        this.submitSignatureURL = submitSignatureURL;
    }
    
    
    @Override
    public Respuesta call() throws Exception {
        KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(nif);
        
        PrivateKey privateKey = (PrivateKey)mockDnie.getKey(
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS,
                ContextoPruebas.DEFAULTS.PASSWORD.toCharArray());
        Certificate[] signerCertChain = mockDnie.getCertificateChain(
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS);

        new PDFSignerWorker(PDF_SIGNER_WORKER, urlTimeStampServer,
                this, reason, location, null, manifestToSign, 
                privateKey, signerCertChain).execute();
        
        countDownLatch.await();
        return getResult();
    }


    @Override
    public void process(List<String> messages) {
        for(String message : messages)  {
            logger.debug("process -> " + message);
        }
    }

    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
        " - nif: " + nif +
        " - worker: " + worker.getClass().getSimpleName());
        respuesta = new Respuesta(worker.getStatusCode(), " - from:" + nif + 
                " - " + worker.getMessage());
        try {
            switch(worker.getId()) {
                case PDF_SIGNER_WORKER:
                    if(Respuesta.SC_OK == worker.getStatusCode()) { 
                        File signedAndTimeStampedPDF = ((PDFSignerWorker)worker).getSignedAndTimeStampedPDF();
                        File documentToSend = File.createTempFile("pdfEncryptedFile", ".eml");
                        documentToSend.deleteOnExit();

                        MimeBodyPart mimeBodyPart = Encryptor.encryptFile(
                                signedAndTimeStampedPDF, ContextoPruebas.INSTANCE.
                                getControlAcceso().getCertificate());
                        mimeBodyPart.writeTo(new FileOutputStream(documentToSend));
                        String contentType = Contexto.PDF_SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
                        new DocumentSenderWorker(MANIFEST_SENDER_WORKER, documentToSend, 
                                contentType, submitSignatureURL, this).execute();
                    } else {
                        logger.error("ERROR - " + worker.getMessage());
                        countDownLatch.countDown();
                    }
                    break;
                case MANIFEST_SENDER_WORKER:
                    countDownLatch.countDown();
                    break;
                default:
                    logger.debug("*** UNKNOWN WORKER ID: '" + worker.getId() + "'");
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, " - from:" 
                    + nif + " - " + ex.getMessage());
            countDownLatch.countDown();
        }

    }


    private Respuesta getResult() {
        return respuesta;
    }
}