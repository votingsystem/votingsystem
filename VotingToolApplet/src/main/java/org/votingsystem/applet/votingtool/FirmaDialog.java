package org.votingsystem.applet.votingtool;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JDialog;

import org.votingsystem.applet.votingtool.dialog.PasswordDialog;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.DNIeSignedMailGenerator;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.FileUtils;
import org.votingsystem.applet.callable.InfoGetter;

import com.itextpdf.text.pdf.PdfReader;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.votingsystem.applet.callable.InfoSender;
import org.votingsystem.applet.callable.PDFSignedSender;
import org.votingsystem.applet.callable.SMIMESignedSender;
import org.votingsystem.applet.model.AppletOperation;
import org.votingsystem.applet.pdf.PdfFormHelper;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.VotingSystemException;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class FirmaDialog extends JDialog {

    private static Logger logger = Logger.getLogger(FirmaDialog.class);
    
    private byte[] bytesDocumento;
    private String eventId = null;
    private AtomicBoolean mostrandoPantallaEnvio = new AtomicBoolean(false);
    private Frame parentFrame;
    private Future executingTask;
    private AppletOperation operation;
    private SMIMEMessageWrapper smimeMessage;
    private final AtomicBoolean done = new AtomicBoolean(false);
    
    public FirmaDialog(Frame parent, boolean modal) {
        super(parent, modal);
        this.parentFrame = parent;
        //parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);       
        initComponents();
        if(operation != null && operation.getContenidoFirma() != null) {
            bytesDocumento = operation.getContenidoFirma().toString().getBytes();
        }
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
                done.set(true);
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                done.set(true);
                sendResponse(ResponseVS.SC_CANCELLED,
                        ContextVS.INSTANCE.getString("operacionCancelada"));
            }
        });
        progressBarPanel.setVisible(false);
        messageLabel.setVisible(false);
        pack();
    }
    
    public void show(AppletOperation operation) {
        this.operation = operation;
        AppletOperation.Type tipoOperacion = operation.getType();
        logger.debug("mostrar - tipoOperacion: " + tipoOperacion);
        setTitle(tipoOperacion.getCaption());
        switch(tipoOperacion) {
            case MANIFEST_SIGN:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                        getResource("/resources/images/pdf_16x16.png"))); 
                        progressLabel.setText("<html>" + 
                ContextVS.INSTANCE.getString("obteniendoDocumento") +"</html>");
                mostrarPantallaEnvio(true);
                PdfGetterWorker pdfGetterWorker = new PdfGetterWorker();
                pdfGetterWorker.execute();
                break;  
            case MANIFEST_PUBLISHING:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                        getResource("/resources/images/pdf_16x16.png"))); 
                        progressLabel.setText("<html>" + 
                ContextVS.INSTANCE.getString("obteniendoDocumento") +"</html>");
                mostrarPantallaEnvio(true);
                PdfGetterWorker publisherWorker = new PdfGetterWorker();
                publisherWorker.execute();
                break;                          
            case BACKUP_REQUEST:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                    getResource("/resources/images/pdf_16x16.png")));
                try {
                    bytesDocumento = PdfFormHelper.getBackupRequest(
                        operation.getEvento().getEventoId().toString(),
                        operation.getEvento().getAsunto(), 
                        operation.getEmailSolicitante());
                } catch(Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                break;
            case REPRESENTATIVE_SELECTION:
                if(operation.getContenidoFirma().get("representativeName") != null) {
                    messageLabel.setText(ContextVS.INSTANCE.getString("selectedRepresentativeMsg", 
                            operation.getContenidoFirma().get("representativeName").toString()));
                    messageLabel.setVisible(true);
                }
                progressBarPanel.setVisible(false);
                break;
            default:
                logger.debug("Operation without interfaze details -> " + 
                        tipoOperacion);
                progressBarPanel.setVisible(false);
                break;
        }
        pack();
        setVisible(true);
    }

    
    class PdfGetterWorker extends SwingWorker<ResponseVS, Object> {
       
        @Override public ResponseVS doInBackground() {
            logger.debug("PdfGetterWorker.doInBackground");
            switch(operation.getType()) {
                case MANIFEST_SIGN:
                    InfoGetter infoGetter = new InfoGetter(null, 
                            operation.getUrlDocumento(), ContentTypeVS.PDF);
                    return infoGetter.call();
                case MANIFEST_PUBLISHING:
                    InfoSender infoSender = new InfoSender(null, 
                            operation.getContenidoFirma().toString().getBytes(),
                            null, operation.getUrlEnvioDocumento(), "eventId");
                    return infoSender.call();   
            }   
            return null;
        }

       @Override protected void done() {
           logger.debug("PdfGetterWorker.done");
            try {
                ResponseVS responseVS = get();
                switch(operation.getType()) {
                    case MANIFEST_SIGN:
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) { 
                            try {
                                bytesDocumento = responseVS.getMessageBytes();
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        } else {
                            sendResponse(responseVS.getStatusCode(), 
                                    ContextVS.INSTANCE.getString(
                                    "errorDescargandoDocumento") + " - " + responseVS.getMessage());
                        }
                        break;
                    case MANIFEST_PUBLISHING:
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) { 
                          try {
                                bytesDocumento = responseVS.getMessageBytes();
                                eventId = ((List<String>)responseVS.getData()).iterator().next();
                                String urlEnvioDocumento = operation.getUrlEnvioDocumento() + 
                                        "/" + eventId;
                                operation.setUrlEnvioDocumento(urlEnvioDocumento);
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        } else {
                            sendResponse(responseVS.getStatusCode(), 
                                    ContextVS.INSTANCE.getString(
                                    "errorDescargandoDocumento") + " - " + responseVS.getMessage());
                        }
                        break;
                }   
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
            }
            mostrarPantallaEnvio(false);
       }
   }

    private void sendResponse(int status, String message) {
        done.set(true);
        operation.setStatusCode(status);
        operation.setMessage(message);
        ContextVS.INSTANCE.sendMessageToHost(operation);
        dispose();
    }
    
    public void inicializarSinDescargarPDF (byte[] bytesPDF) {
        logger.debug("inicializarSinDescargarPDF");
        bytesDocumento = bytesPDF;
        setVisible(true);
    }
    
    public void mostrarPantallaEnvio (final boolean visibility) {
        logger.debug("mostrarPantallaEnvio: " + visibility + 
                " - mostrandoPantallaEnvio: " + mostrandoPantallaEnvio.get());
        progressBarPanel.setVisible(visibility);
        enviarButton.setVisible(!visibility);
        confirmacionPanel.setVisible(!visibility);
        mostrandoPantallaEnvio.set(visibility);
        if (mostrandoPantallaEnvio.get()) cerrarButton.setText(
                ContextVS.INSTANCE.getString("cancelar"));
        else cerrarButton.setText(ContextVS.INSTANCE.getString("cerrar"));
        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cerrarButton = new javax.swing.JButton();
        confirmacionPanel = new javax.swing.JPanel();
        mensajeLabel = new javax.swing.JLabel();
        verDocumentoButton = new javax.swing.JButton();
        messageLabel = new javax.swing.JLabel();
        progressBarPanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        enviarButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        cerrarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/cancel_16x16.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/votingsystem/applet/votingtool/Bundle"); // NOI18N
        cerrarButton.setText(bundle.getString("FirmaDialog.cerrarButton.text")); // NOI18N
        cerrarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cerrarButtonActionPerformed(evt);
            }
        });

        confirmacionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        mensajeLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        mensajeLabel.setText(bundle.getString("FirmaDialog.mensajeLabel.text")); // NOI18N
        mensajeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        verDocumentoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/fileopen16x16.png"))); // NOI18N
        verDocumentoButton.setText(bundle.getString("FirmaDialog.verDocumentoButton.text")); // NOI18N
        verDocumentoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verDocumentoButtonActionPerformed(evt);
            }
        });

        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageLabel.setText(bundle.getString("FirmaDialog.messageLabel.text")); // NOI18N

        javax.swing.GroupLayout confirmacionPanelLayout = new javax.swing.GroupLayout(confirmacionPanel);
        confirmacionPanel.setLayout(confirmacionPanelLayout);
        confirmacionPanelLayout.setHorizontalGroup(
            confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confirmacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mensajeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(confirmacionPanelLayout.createSequentialGroup()
                        .addComponent(verDocumentoButton)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        confirmacionPanelLayout.setVerticalGroup(
            confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confirmacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(messageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(mensajeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(verDocumentoButton)
                .addContainerGap())
        );

        progressLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        progressLabel.setText(bundle.getString("FirmaDialog.progressLabel.text")); // NOI18N

        progressBar.setIndeterminate(true);

        javax.swing.GroupLayout progressBarPanelLayout = new javax.swing.GroupLayout(progressBarPanel);
        progressBarPanel.setLayout(progressBarPanelLayout);
        progressBarPanelLayout.setHorizontalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        progressBarPanelLayout.setVerticalGroup(
            progressBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(progressBarPanelLayout.createSequentialGroup()
                .addComponent(progressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        enviarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/signature-ok_16x16.png"))); // NOI18N
        enviarButton.setText(bundle.getString("FirmaDialog.enviarButton.text")); // NOI18N
        enviarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enviarButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBarPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(enviarButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cerrarButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(confirmacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 10, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(confirmacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enviarButton)
                    .addComponent(cerrarButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cerrarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cerrarButtonActionPerformed
        logger.debug("cerrarButtonActionPerformed - mostrandoPantallaEnvio: " + 
                mostrandoPantallaEnvio);
        if (mostrandoPantallaEnvio.get()) {
            if (executingTask != null) executingTask.cancel(true);
            mostrarPantallaEnvio(false);
            return;
        } else {
            sendResponse(ResponseVS.SC_CANCELLED,
                    ContextVS.INSTANCE.getString("operacionCancelada"));
        }
        dispose();
    }//GEN-LAST:event_cerrarButtonActionPerformed

    private void enviarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enviarButtonActionPerformed
        String password = null;
        PasswordDialog dialogoPassword = new PasswordDialog (parentFrame, true);
        dialogoPassword.setVisible(true);
        password = dialogoPassword.getPassword();
        if (password == null) return;
        mostrarPantallaEnvio(true);
        progressLabel.setText("<html>" + 
                ContextVS.INSTANCE.getString("progressLabel")+ "</html>");
        SignedSenderWorker signedSenderWorker = new SignedSenderWorker(password);
        executingTask = signedSenderWorker;
        signedSenderWorker.execute();
        pack();       
    }//GEN-LAST:event_enviarButtonActionPerformed

    private void verDocumentoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verDocumentoButtonActionPerformed
        if (!Desktop.isDesktopSupported()) {
            logger.debug("No hay soporte de escritorio");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            logger.debug("No se puede editar archivos");
        }
        try {
            File documento = new File(VotingToolContext.DEFAULTS.APPTEMPDIR + 
                    operation.getType().getNombreArchivoEnDisco());
            documento.deleteOnExit();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(bytesDocumento), documento);
            logger.info("documento.getAbsolutePath(): " + documento.getAbsolutePath());
            desktop.open(documento);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }//GEN-LAST:event_verDocumentoButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cerrarButton;
    private javax.swing.JPanel confirmacionPanel;
    private javax.swing.JButton enviarButton;
    private javax.swing.JLabel mensajeLabel;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel progressBarPanel;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JButton verDocumentoButton;
    // End of variables declaration//GEN-END:variables

    
    class SignedSenderWorker extends SwingWorker<ResponseVS, Object> {
        
        private String password = null;
        
        public SignedSenderWorker(String password) {
            this.password = password;
        }
        
        @Override public ResponseVS doInBackground() 
                throws Exception {
            logger.debug("SignedSenderWorker.doInBackground");
            X509Certificate destinationCert = null;
            switch(operation.getType()) {
                case REPRESENTATIVE_REVOKE:
                case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
                case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
                case REPRESENTATIVE_SELECTION:
                case VOTE_CANCELLATION:
                case ACCESS_REQUEST_CANCELLATION:
                case CONTROL_CENTER_STATE_CHANGE_SMIME:
                case CONTROL_CENTER_ASSOCIATION:
                case SMIME_CLAIM_SIGNATURE:
                case EVENT_CANCELLATION:
                    smimeMessage = DNIeSignedMailGenerator.
                            genMimeMessage(null, operation.getNombreDestinatarioFirmaNormalizado(),
                            operation.getContenidoFirma().toString(),
                            password.toCharArray(), operation.getAsuntoMensajeFirmado(), null); 
                    destinationCert = ContextVS.INSTANCE.
                        getAccessControl().getCertificate();
                    SMIMESignedSender senderWorker = new SMIMESignedSender(
                            null, smimeMessage, operation.getUrlEnvioDocumento(), 
                            null, destinationCert);
                    return senderWorker.call();
                case VOTING_PUBLISHING:
                case CLAIM_PUBLISHING:    
                    smimeMessage = DNIeSignedMailGenerator.
                        genMimeMessage(null, operation.getNombreDestinatarioFirmaNormalizado(),
                        operation.getContenidoFirma().toString(),
                        password.toCharArray(), operation.getAsuntoMensajeFirmado(), null); 
                    destinationCert = ContextVS.INSTANCE.
                        getAccessControl().getCertificate();
                    SMIMESignedSender worker = new SMIMESignedSender(
                            null, smimeMessage, operation.getUrlEnvioDocumento(), 
                            null, destinationCert, "eventURL");
                    return worker.call();
                case BACKUP_REQUEST:
                case MANIFEST_SIGN:
                case MANIFEST_PUBLISHING:
                    PdfReader readerManifiesto = new PdfReader(bytesDocumento);
                    String reason = null;
                    String location = null;
                    destinationCert = ContextVS.INSTANCE.getAccessControl().getCertificate();
                    PDFSignedSender pdfSignedSender = new PDFSignedSender(
                            null,  operation.getUrlEnvioDocumento(), reason, location, 
                            password.toCharArray(), readerManifiesto, 
                            null, null, destinationCert);
                    return pdfSignedSender.call();
                default:
                    logger.debug("Operation not found" + operation.getType().toString());
                    break;
            }
            return null;
       }

       @Override protected void done() {
            mostrarPantallaEnvio(false);
            try {
                ResponseVS responseVS = get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    logger.debug("SignedSenderWorker.done - statusCode:" + 
                            responseVS.getStatusCode());
                    if(operation.isRespuestaConRecibo()) {
                        try {
                            logger.debug("SignedSenderWorker.done - isRespuestaConRecibo");
                            SMIMEMessageWrapper smimeMessageResp = responseVS.getSmimeMessage();
                            if(smimeMessageResp == null) {
                                ByteArrayInputStream bais = new ByteArrayInputStream(
                                    responseVS.getMessageBytes());
                                smimeMessageResp = new SMIMEMessageWrapper(bais);
                            }
                            String operationStr = smimeMessageResp.getSignedContent();
                            AppletOperation result = AppletOperation.parse(operationStr);
                            String msg = result.getMessage();
                            if(AppletOperation.Type.VOTING_PUBLISHING == 
                                    operation.getType() || AppletOperation.Type.
                                    CLAIM_PUBLISHING == operation.getType()) {
                                String eventURL = ((List<String>)responseVS.
                                        getData()).iterator().next();
                                result.setUrlDocumento(eventURL);
                                msg = eventURL;
                            }
                            sendResponse(responseVS.getStatusCode(), msg);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                            sendResponse(ResponseVS.SC_ERROR, ex.getMessage());
                        }
                    } else sendResponse(responseVS.getStatusCode(), 
                            responseVS.getMessage());   
                } else {
                    sendResponse(responseVS.getStatusCode(), responseVS.getMessage());
                }
           } catch(Exception ex) {
               logger.error(ex.getMessage(), ex);
               String mensajeError = ContextVS.INSTANCE.getString("signDocumentErrorMsg");
               if(ex.getCause() instanceof VotingSystemException) {
                   mensajeError = ex.getCause().getMessage();
               }
               sendResponse(ResponseVS.SC_ERROR, mensajeError);
            }
            dispose();
       }
    }
   
}