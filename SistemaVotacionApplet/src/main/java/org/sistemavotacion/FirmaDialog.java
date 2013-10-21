package org.sistemavotacion;

import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import org.sistemavotacion.dialogo.PasswordDialog;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.DNIeSignedMailGenerator;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.callable.InfoGetter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.pdf.PdfReader;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;
import org.sistemavotacion.callable.InfoSender;
import static org.sistemavotacion.modelo.Operacion.Tipo.*;
import org.sistemavotacion.callable.PDFSignedSender;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.seguridad.VotingSystemException;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class FirmaDialog extends JDialog {

    private static Logger logger = LoggerFactory.getLogger(FirmaDialog.class);
    
    private byte[] bytesDocumento;
    private String eventId = null;
    private AtomicBoolean mostrandoPantallaEnvio = new AtomicBoolean(false);
    private Frame parentFrame;
    private Future executingTask;
    private AppletFirma appletFirma;
    private Operacion operacion;
    private SMIMEMessageWrapper smimeMessage;
    private final AtomicBoolean done = new AtomicBoolean(false);
    
    public FirmaDialog(Frame parent, boolean modal, final AppletFirma appletFirma) {
        super(parent, modal);
        this.parentFrame = parent;
        this.appletFirma = appletFirma;
        //parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);       
        initComponents();
        operacion = appletFirma.getOperacionEnCurso();
        if(operacion != null && operacion.getContenidoFirma() != null) {
            bytesDocumento = operacion.getContenidoFirma().toString().getBytes();
        }
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug(" - window closed event received");
                done.set(true);
            }

            public void windowClosing(WindowEvent e) {
                logger.debug(" - window closing event received");
                done.set(true);
                sendResponse(Operacion.SC_CANCELADO,
                        Contexto.INSTANCE.getString("operacionCancelada"));
            }
        });
        progressBarPanel.setVisible(false);
        messageLabel.setVisible(false);
        pack();
    }
    
    public void mostrar() {
        Operacion.Tipo tipoOperacion = operacion.getTipo();
        logger.debug("mostrar - tipoOperacion: " + tipoOperacion);
        setTitle(tipoOperacion.getCaption());
        switch(tipoOperacion) {
            case FIRMA_MANIFIESTO_PDF:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                        getResource("/resources/images/pdf_16x16.png"))); 
                        progressLabel.setText("<html>" + 
                Contexto.INSTANCE.getString("obteniendoDocumento") +"</html>");
                mostrarPantallaEnvio(true);
                PdfGetterWorker pdfGetterWorker = new PdfGetterWorker();
                pdfGetterWorker.execute();
                break;  
            case PUBLICACION_MANIFIESTO_PDF:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                        getResource("/resources/images/pdf_16x16.png"))); 
                        progressLabel.setText("<html>" + 
                Contexto.INSTANCE.getString("obteniendoDocumento") +"</html>");
                mostrarPantallaEnvio(true);
                PdfGetterWorker publisherWorker = new PdfGetterWorker();
                publisherWorker.execute();
                break;                          
            case SOLICITUD_COPIA_SEGURIDAD:
                verDocumentoButton.setIcon(new ImageIcon(getClass().
                        getResource("/resources/images/pdf_16x16.png"))); 
                break;
            case REPRESENTATIVE_SELECTION:
                if(operacion.getContenidoFirma().get("representativeName") != null) {
                    messageLabel.setText(Contexto.INSTANCE.getString("selectedRepresentativeMsg", 
                            operacion.getContenidoFirma().get("representativeName").toString()));
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
    
    
    class PdfGetterWorker extends SwingWorker<Respuesta, Object> {
       
        @Override public Respuesta doInBackground() {
            logger.debug("PdfGetterWorker.doInBackground");
            switch(operacion.getTipo()) {
                case FIRMA_MANIFIESTO_PDF:
                    InfoGetter infoGetter = new InfoGetter(null, 
                            operacion.getUrlDocumento(), Contexto.PDF_CONTENT_TYPE);
                    return infoGetter.call();
                case PUBLICACION_MANIFIESTO_PDF:
                    InfoSender infoSender = new InfoSender(null, 
                            operacion.getContenidoFirma().toString().getBytes(),
                            null, operacion.getUrlEnvioDocumento(), "eventId");
                    return infoSender.call();   
            }   
            return null;
        }

       @Override protected void done() {
           logger.debug("PdfGetterWorker.done");
            try {
                Respuesta respuesta = get();
                switch(operacion.getTipo()) {
                    case FIRMA_MANIFIESTO_PDF:
                        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) { 
                            try {
                                bytesDocumento = respuesta.getMessageBytes();
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        } else {
                            sendResponse(respuesta.getCodigoEstado(), 
                                    Contexto.INSTANCE.getString(
                                    "errorDescargandoDocumento") + " - " + respuesta.getMensaje());
                        }
                        break;
                    case PUBLICACION_MANIFIESTO_PDF:
                        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) { 
                          try {
                                bytesDocumento = respuesta.getMessageBytes();
                                eventId = ((List<String>)respuesta.getData()).iterator().next();
                                String urlEnvioDocumento = operacion.getUrlEnvioDocumento() + 
                                        "/" + eventId;
                                operacion.setUrlEnvioDocumento(urlEnvioDocumento);
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        } else {
                            sendResponse(respuesta.getCodigoEstado(), 
                                    Contexto.INSTANCE.getString(
                                    "errorDescargandoDocumento") + " - " + respuesta.getMensaje());
                        }
                        break;
                }   
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                sendResponse(Respuesta.SC_ERROR, ex.getMessage());
            }
            mostrarPantallaEnvio(false);
       }
   }

    private void sendResponse(int status, String message) {
        done.set(true);
        operacion.setCodigoEstado(status);
        operacion.setMensaje(message);
        appletFirma.enviarMensajeAplicacion(operacion);
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
                Contexto.INSTANCE.getString("cancelar"));
        else cerrarButton.setText(Contexto.INSTANCE.getString("cerrar"));
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
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/Bundle"); // NOI18N
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
            sendResponse(Operacion.SC_CANCELADO,
                    Contexto.INSTANCE.getString("operacionCancelada"));
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
                Contexto.INSTANCE.getString("progressLabel")+ "</html>");
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
            File documento = new File(Contexto.DEFAULTS.APPTEMPDIR + appletFirma.
                    getOperacionEnCurso().getTipo().getNombreArchivoEnDisco());
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

    
    class SignedSenderWorker extends SwingWorker<Respuesta, Object> {
        
        private String password = null;
        
        public SignedSenderWorker(String password) {
            this.password = password;
        }
        
        @Override public Respuesta doInBackground() 
                throws Exception {
            logger.debug("SignedSenderWorker.doInBackground");
            X509Certificate destinationCert = null;
            switch(operacion.getTipo()) {
                case REPRESENTATIVE_REVOKE:
                case REPRESENTATIVE_ACCREDITATIONS_REQUEST:
                case REPRESENTATIVE_VOTING_HISTORY_REQUEST:
                case REPRESENTATIVE_SELECTION:
                case ANULAR_VOTO:
                case ANULAR_SOLICITUD_ACCESO:
                case CAMBIO_ESTADO_CENTRO_CONTROL_SMIME:
                case ASOCIAR_CENTRO_CONTROL:
                case FIRMA_RECLAMACION_SMIME:
                case CANCELAR_EVENTO:
                    smimeMessage = DNIeSignedMailGenerator.
                            genMimeMessage(null, operacion.getNombreDestinatarioFirmaNormalizado(),
                            operacion.getContenidoFirma().toString(),
                            password.toCharArray(), operacion.getAsuntoMensajeFirmado(), null); 
                    destinationCert = Contexto.INSTANCE.
                        getAccessControl().getCertificate();
                    SMIMESignedSender senderWorker = new SMIMESignedSender(
                            null, smimeMessage, operacion.getUrlEnvioDocumento(), 
                            null, destinationCert);
                    return senderWorker.call();
                case PUBLICACION_VOTACION_SMIME:
                case PUBLICACION_RECLAMACION_SMIME:    
                    smimeMessage = DNIeSignedMailGenerator.
                        genMimeMessage(null, operacion.getNombreDestinatarioFirmaNormalizado(),
                        operacion.getContenidoFirma().toString(),
                        password.toCharArray(), operacion.getAsuntoMensajeFirmado(), null); 
                    destinationCert = Contexto.INSTANCE.
                        getAccessControl().getCertificate();
                    SMIMESignedSender worker = new SMIMESignedSender(
                            null, smimeMessage, operacion.getUrlEnvioDocumento(), 
                            null, destinationCert, "eventURL");
                    return worker.call();
                case SOLICITUD_COPIA_SEGURIDAD:
                case FIRMA_MANIFIESTO_PDF:
                case PUBLICACION_MANIFIESTO_PDF:
                    PdfReader readerManifiesto = new PdfReader(bytesDocumento);
                    String reason = null;
                    String location = null;
                    destinationCert = Contexto.INSTANCE.
                        getAccessControl().getCertificate();
                    PDFSignedSender pdfSignedSender = new PDFSignedSender(
                            null,  operacion.getUrlEnvioDocumento(), reason, location, 
                            password.toCharArray(), readerManifiesto, 
                            null, null, destinationCert);
                    return pdfSignedSender.call();
                default:
                    logger.debug("Operation not found" + operacion.getTipo().toString());
                    break;
            }
            return null;
       }

       @Override protected void done() {
            mostrarPantallaEnvio(false);
            try {
                Respuesta respuesta = get();
                if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    logger.debug("SignedSenderWorker.done - statusCode:" + 
                            respuesta.getCodigoEstado());
                    if(operacion.isRespuestaConRecibo()) {
                        try {
                            logger.debug("SignedSenderWorker.done - isRespuestaConRecibo");
                            ByteArrayInputStream bais = new ByteArrayInputStream(
                                    respuesta.getMensaje().getBytes());
                            SMIMEMessageWrapper smimeMessageResp = 
                                    new SMIMEMessageWrapper(null, bais, null);
                            String operationStr = smimeMessageResp.getSignedContent();
                            Operacion result = Operacion.parse(operationStr);
                            String msg = result.getMensaje();
                            if(Operacion.Tipo.PUBLICACION_VOTACION_SMIME == 
                                    operacion.getTipo() || Operacion.Tipo.
                                    PUBLICACION_RECLAMACION_SMIME == operacion.getTipo()) {
                                String eventURL = ((List<String>)respuesta.
                                        getData()).iterator().next();
                                result.setUrlDocumento(eventURL);
                                msg = eventURL;
                            }
                            sendResponse(respuesta.getCodigoEstado(), msg);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                            sendResponse(Respuesta.SC_ERROR, ex.getMessage());
                        }
                    } else sendResponse(respuesta.getCodigoEstado(), 
                            respuesta.getMensaje());   
                } else {
                    sendResponse(respuesta.getCodigoEstado(), 
                            respuesta.getMensaje());
                }
           } catch(Exception ex) {
               logger.error(ex.getMessage(), ex);
               String mensajeError = Contexto.INSTANCE.getString("signDocumentErrorMsg");
               if(ex.getCause() instanceof VotingSystemException) {
                   mensajeError = ex.getCause().getMessage();
               }
               sendResponse(Respuesta.SC_ERROR, mensajeError);
            }
            dispose();
       }
    }
   
}