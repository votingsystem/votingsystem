package org.sistemavotacion;


import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.SwingWorker;
import javax.swing.text.html.parser.ParserDelegator;
import org.sistemavotacion.dialogo.PasswordDialog;
import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.sistemavotacion.smime.DNIeSignedMailGenerator;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.callable.AccessRequestor;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VotacionDialog extends JDialog {

    private static Logger logger = LoggerFactory.getLogger(VotacionDialog.class);
    
    private volatile boolean mostrandoPantallaEnvio = false;
    private Frame parentFrame;
    private Future<Respuesta> tareaEnEjecucion;
    private Evento votoEvento;
    private Operacion operacion;
    private SMIMEMessageWrapper smimeMessage;
    private final AtomicBoolean done = new AtomicBoolean(false);
    
    public VotacionDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.parentFrame = parent;
        //parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);       
        initComponents();
        operacion = Contexto.INSTANCE.getPendingOperation();
        votoEvento = operacion.getEvento();
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                logger.debug("VotacionDialog window closed event received");
            }

            public void windowClosing(WindowEvent e) {
                logger.debug("VotacionDialog window closing event received");
                sendResponse(Operacion.SC_CANCELADO,
                        Contexto.INSTANCE.getString("operacionCancelada"));
            }
        });
        //Bug similar to -> http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993691
        ParserDelegator workaround = new ParserDelegator();
        
        messageLabel.setText(Contexto.INSTANCE.getString(
                "mensajeVotacion", votoEvento.getAsunto(), 
                votoEvento.getOpcionSeleccionada().getContent()));
        setTitle(operacion.getTipo().getCaption());
        progressBarPanel.setVisible(false);
        pack();
    }

    private void sendResponse(int status, String message) {
        done.set(true);
        operacion.setCodigoEstado(status);
        operacion.setMensaje(message);
        Contexto.INSTANCE.sendMessageToHost(operacion);
        dispose();
    }
        
    public void mostrarPantallaEnvio (boolean visibility) {
        logger.debug("mostrarPantallaEnvio - " + visibility);
        mostrandoPantallaEnvio = visibility;
        progressBarPanel.setVisible(visibility);
        enviarButton.setVisible(!visibility);
        confirmacionPanel.setVisible(!visibility);
        if (mostrandoPantallaEnvio) cerrarButton.setText(
                Contexto.INSTANCE.getString("cancelar"));
        else cerrarButton.setText(
                Contexto.INSTANCE.getString("cerrar"));
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
        verDocumentoButton = new javax.swing.JButton();
        messageLabel = new javax.swing.JLabel();
        progressBarPanel = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        enviarButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        cerrarButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/cancel_16x16.png"))); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sistemavotacion/Bundle"); // NOI18N
        cerrarButton.setText(bundle.getString("VotacionDialog.cerrarButton.text")); // NOI18N
        cerrarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cerrarButtonActionPerformed(evt);
            }
        });

        confirmacionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        verDocumentoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/fileopen16x16.png"))); // NOI18N
        verDocumentoButton.setText(bundle.getString("VotacionDialog.verDocumentoButton.text")); // NOI18N
        verDocumentoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verDocumentoButtonActionPerformed(evt);
            }
        });

        messageLabel.setText(bundle.getString("VotacionDialog.messageLabel.text")); // NOI18N

        javax.swing.GroupLayout confirmacionPanelLayout = new javax.swing.GroupLayout(confirmacionPanel);
        confirmacionPanel.setLayout(confirmacionPanelLayout);
        confirmacionPanelLayout.setHorizontalGroup(
            confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confirmacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(verDocumentoButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        confirmacionPanelLayout.setVerticalGroup(
            confirmacionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confirmacionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 71, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(verDocumentoButton)
                .addContainerGap())
        );

        progressLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        progressLabel.setText(bundle.getString("VotacionDialog.progressLabel.text")); // NOI18N

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
        enviarButton.setText(bundle.getString("VotacionDialog.enviarButton.text")); // NOI18N
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
                    .addComponent(confirmacionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(confirmacionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enviarButton)
                    .addComponent(cerrarButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cerrarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cerrarButtonActionPerformed
        logger.debug("cerrarButtonActionPerformed - mostrandoPantallaEnvio: " + mostrandoPantallaEnvio);
        if (mostrandoPantallaEnvio) {
            if (tareaEnEjecucion != null) tareaEnEjecucion.cancel(true);
            mostrarPantallaEnvio(false);
            return;
        }
        sendResponse(Operacion.SC_CANCELADO,
                Contexto.INSTANCE.getString("operacionCancelada"));
    }//GEN-LAST:event_cerrarButtonActionPerformed

    private void enviarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enviarButtonActionPerformed
        String password = null;
        PasswordDialog dialogoPassword = new PasswordDialog (parentFrame, true);
        dialogoPassword.setVisible(true);
        password = dialogoPassword.getPassword();
        if (password == null) return;
        mostrarPantallaEnvio(true);
        progressLabel.setText("<html>" + Contexto.INSTANCE.
                getString("progressLabel") + "</html>");
        AccesRequestWorker accesRequestWorker = new AccesRequestWorker(password);
        tareaEnEjecucion = accesRequestWorker;
        accesRequestWorker.execute();
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
            File documento = new File(Contexto.DEFAULTS.APPTEMPDIR +
                    UUID.randomUUID().toString() + "_" + operacion.getTipo().
                    getNombreArchivoEnDisco());
            documento.deleteOnExit();
            String accessRequest = votoEvento.getAccessRequestJSON().toString();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(
                    accessRequest.getBytes()), documento);
            logger.info("documento.getAbsolutePath(): " + documento.getAbsolutePath());
            desktop.open(documento);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }//GEN-LAST:event_verDocumentoButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JButton cerrarButton;
    javax.swing.JPanel confirmacionPanel;
    javax.swing.JButton enviarButton;
    javax.swing.JLabel messageLabel;
    javax.swing.JProgressBar progressBar;
    javax.swing.JPanel progressBarPanel;
    javax.swing.JLabel progressLabel;
    javax.swing.JButton verDocumentoButton;
    // End of variables declaration//GEN-END:variables
 
    class AccesRequestWorker extends SwingWorker<Respuesta, Object> {

        private String password = null;
        private PKCS10WrapperClient pkcs10WrapperClient;
        
        private AccesRequestWorker(String password) {
            this.password = password;
        }
       
        @Override public Respuesta doInBackground() {
            logger.debug("AccesRequestWorker.doInBackground");
            try {
                String fromUser = Contexto.INSTANCE.getString("electorLbl");
                String toUser =  votoEvento.getControlAcceso().getNombreNormalizado();
                String msgSubject = Contexto.INSTANCE.getString(
                        "accessRequestMsgSubject")  + votoEvento.getEventoId();
                smimeMessage = DNIeSignedMailGenerator.genMimeMessage(fromUser, 
                        toUser, votoEvento.getAccessRequestJSON().toString(),
                        password.toCharArray(), msgSubject, null);

                //No se hace la comprobación antes porque no hay usuario en contexto
                //hasta que no se firma al menos una vez
                votoEvento.setUsuario(Contexto.INSTANCE.getUsuario());

                X509Certificate accesRequestServerCert = Contexto.INSTANCE.
                        getAccessControl().getCertificate();
                AccessRequestor accessRequestor = new AccessRequestor(smimeMessage, 
                        votoEvento, accesRequestServerCert);
                Respuesta respuesta =  accessRequestor.call();
                pkcs10WrapperClient = accessRequestor.getPKCS10WrapperClient();
                return respuesta;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            }
        }

       @Override protected void done() {
           mostrarPantallaEnvio(false);
            try {
                Respuesta respuesta = get();
                logger.debug("AccesRequestWorker.done - response status: " + 
                        respuesta.getCodigoEstado());
                if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                    progressLabel.setText("<html><b>" + Contexto.INSTANCE.getString(
                        "notificandoCentroControlLabel") +"</b></html>");
                    mostrarPantallaEnvio(true);
                    VoteSenderWorker voteSenderWorker = new VoteSenderWorker(
                        pkcs10WrapperClient, votoEvento);
                    tareaEnEjecucion = voteSenderWorker;
                    voteSenderWorker.execute();
                } else {
                    /*MensajeDialog mensajeDialog = new MensajeDialog(parentFrame,true);
                    String errorLanzandoVotoMsg = 
                            Contexto.INSTANCE.getString("errorLanzandoVotoMsg");
                    mensajeDialog.showMessage(errorLanzandoVotoMsg + " - " 
                            + ex.getMessage(), errorLanzandoVotoMsg);*/
                    sendResponse(respuesta.getCodigoEstado(), respuesta.getMensaje());
                }
            } catch(Exception ex) {
                sendResponse(Respuesta.SC_ERROR, ex.getMessage());
            }
       }
   }
    
    
    class VoteSenderWorker extends SwingWorker<Respuesta, Object> {

        PKCS10WrapperClient pkcs10WrapperClient;
        Evento vote;
        
        private VoteSenderWorker(PKCS10WrapperClient pkcs10WrapperClient, 
                Evento votoEvento) {
            this.pkcs10WrapperClient = pkcs10WrapperClient;
            this.vote = votoEvento;
        }
       
        @Override public Respuesta doInBackground() {
            logger.debug("VoteSenderWorker.doInBackground");
            String textToSign = vote.getVoteJSON().toString();
            try {
                String fromUser = votoEvento.getHashCertificadoVotoBase64();
                String toUser = StringUtils.getCadenaNormalizada(
                        votoEvento.getCentroControl().getNombre());
                String msgSubject = Contexto.INSTANCE.getString("asuntoVoto");
                smimeMessage = pkcs10WrapperClient.genMimeMessage(fromUser, toUser, 
                        textToSign, msgSubject, null);

                String urlVoteService = votoEvento.getUrlRecolectorVotosCentroControl();
                SMIMESignedSender signedSender = new SMIMESignedSender(null,
                        smimeMessage, urlVoteService, pkcs10WrapperClient.
                        getKeyPair(), Contexto.INSTANCE. getControlCenter().getCertificate(),
                        "voteURL");
                return signedSender.call();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
            }
        }

       @Override protected void done() {
           mostrarPantallaEnvio(false);
            try {
                Respuesta respuesta = get();
                logger.debug("VoteSenderWorker.done - response status: " + 
                        respuesta.getCodigoEstado());
                String msg = respuesta.getMensaje();
                if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {  
                    SMIMEMessageWrapper validatedVote = respuesta.getSmimeMessage();
                    ReciboVoto reciboVoto = new ReciboVoto(
                            Respuesta.SC_OK, validatedVote, votoEvento);
                    respuesta.setReciboVoto(reciboVoto);
                    reciboVoto.setVoto(votoEvento);
                    Contexto.INSTANCE.addReceipt(
                        votoEvento.getHashCertificadoVotoBase64(), reciboVoto);
                    //voteURL header
                    msg = ((List<String>)respuesta.getData()).iterator().next();
                }
                sendResponse(respuesta.getCodigoEstado(), msg);
            } catch(Exception ex) {
                sendResponse(Respuesta.SC_ERROR, ex.getMessage());
            }
       }
   }
}